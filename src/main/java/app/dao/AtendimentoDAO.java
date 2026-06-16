package app.dao;

import app.DatabaseConnection;
import app.model.AtendimentoBalcao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para a tabela atendimento — liga uma senha chamada a um balcão e a
 * um utilizador (balconista), com hora_chamada / hora_inicio / hora_fim.
 *
 * Usado para alimentar:
 *  - EcraChamadaController (Ecrã de Chamada / Sala de Espera): mostra a
 *    senha actualmente em atendimento em cada balcão.
 *  - PainelBalconistaController: regista chamada/fim de atendimento.
 */
public class AtendimentoDAO {

    /**
     * Regista uma chamada na tabela atendimento.
     * hora_chamada e hora_inicio são definidos para NOW().
     *
     * @return o id_atendimento gerado, ou -1 em caso de falha.
     */
    public int registarChamada(int idSenha, int idBalcao, int idUtilizador) throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação à base de dados");

        String sql = "INSERT INTO atendimento " +
                     "(id_senha, id_balcao, id_utilizador, hora_chamada, hora_inicio) " +
                     "VALUES (?, ?, ?, NOW(), NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idSenha);
            ps.setInt(2, idBalcao);
            ps.setInt(3, idUtilizador);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    /** Marca a hora_fim do atendimento (conclusão ou ausência). */
    public void finalizar(int idAtendimento) throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação à base de dados");
        String sql = "UPDATE atendimento SET hora_fim = NOW() WHERE id_atendimento = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAtendimento);
            ps.executeUpdate();
        }
    }

    /**
     * Lista a senha actualmente em atendimento em cada balcão (hora_fim NULL),
     * indexado por número do balcão (1..N).
     *
     * Usado pelo Ecrã de Chamada para mostrar "SENHAS NO BALCÃO".
     */
    public Map<Integer, AtendimentoBalcao> listarEmAtendimentoPorBalcao() throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        Map<Integer, AtendimentoBalcao> mapa = new HashMap<>();
        if (conn == null) return mapa;

        String sql =
            "SELECT b.numero_balcao, s.codigo, sv.nome, sv.letra, a.hora_chamada " +
            "FROM atendimento a " +
            "JOIN balcao b  ON a.id_balcao = b.id_balcao " +
            "JOIN senha s   ON a.id_senha  = s.id_senha " +
            "JOIN servico sv ON s.id_servico = sv.id_servico " +
            "WHERE a.hora_fim IS NULL " +
            "ORDER BY a.hora_chamada DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int numeroBalcao = rs.getInt("numero_balcao");
                // Mantém apenas o atendimento mais recente por balcão
                // (ORDER BY hora_chamada DESC + putIfAbsent garante isso)
                mapa.putIfAbsent(numeroBalcao, new AtendimentoBalcao(
                        numeroBalcao,
                        rs.getString("letra"),
                        rs.getString("codigo"),
                        rs.getString("nome"),
                        rs.getTimestamp("hora_chamada")
                ));
            }
        }
        return mapa;
    }

    /**
     * Lista até `limite` senhas em EM_ESPERA, ordenadas por prioridade → FIFO.
     * Usado pelo Ecrã de Chamada para "PRÓXIMAS SENHAS".
     */
    public List<AtendimentoBalcao> listarProximas(int limite) throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        List<AtendimentoBalcao> lista = new ArrayList<>();
        if (conn == null) return lista;

        String sql =
            "SELECT s.codigo, sv.nome, sv.letra " +
            "FROM senha s " +
            "JOIN servico sv ON s.id_servico = sv.id_servico " +
            "WHERE s.estado = 'EM_ESPERA' " +
            "ORDER BY sv.prioritario DESC, s.data_emissao ASC " +
            "LIMIT ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new AtendimentoBalcao(
                            0,
                            rs.getString("letra"),
                            rs.getString("codigo"),
                            rs.getString("nome"),
                            null
                    ));
                }
            }
        }
        return lista;
    }

    /**
     * Devolve o id_atendimento em aberto (hora_fim IS NULL) para a senha
     * indicada, ou -1 se não existir nenhum.
     */
    public int idAtendimentoAberto(int idSenha) throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) return -1;
        String sql = "SELECT id_atendimento FROM atendimento " +
                     "WHERE id_senha = ? AND hora_fim IS NULL " +
                     "ORDER BY hora_chamada DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSenha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }
}
