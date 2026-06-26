package app.dao;

import app.DatabaseConnection;
import app.model.Senha;
import app.model.Senha.Estado;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * DAO responsável por todas as operações de base de dados da tabela senha.
 */
public class SenhaDAO {

    /**
     * Gera o próximo código sequencial para o serviço/dia.
     * Formato: {LETRA}{YYYYMMDD}{NNN}  ex: A20260606001
     * Usa SELECT FOR UPDATE dentro de transação para evitar duplicados concorrentes.
     */
    public Senha inserir(int idServico, String letra, boolean prioritario, String nomeServico)
            throws SQLException {

        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação à base de dados");

        conn.setAutoCommit(false);
        try {
            // Conta senhas de HOJE para este serviço — sequência reinicia a cada dia
            String sqlSeq = "SELECT COUNT(*) FROM senha " +
                            "WHERE id_servico = ? AND DATE(data_emissao) = CURDATE()";
            int seq;
            try (PreparedStatement ps = conn.prepareStatement(sqlSeq)) {
                ps.setInt(1, idServico);
                ResultSet rs = ps.executeQuery();
                seq = rs.next() ? rs.getInt(1) + 1 : 1;
            }

            // Formato: A01 … A99, depois A00 (reinicia a cada 100 senhas no dia)
            String codigo = String.format("%s%03d", letra, seq);

            String sqlIns = "INSERT INTO senha (codigo, id_servico, estado, numero_chamadas, data_emissao) " +
                            "VALUES (?, ?, 'EM_ESPERA', 0, NOW())";
            int gerado;
            try (PreparedStatement ps = conn.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, codigo);
                ps.setInt(2, idServico);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                gerado = keys.next() ? keys.getInt(1) : -1;
            }

            conn.commit();

            Senha s = new Senha(codigo, idServico, nomeServico, prioritario);
            s.setIdSenha(gerado);
            return s;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Actualiza o estado de uma senha. */
    public void actualizarEstado(int idSenha, Estado estado) throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação");
        String sql = "UPDATE senha SET estado = ? WHERE id_senha = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idSenha);
            ps.executeUpdate();
        }
    }

    /** Incrementa o contador de chamadas. */
    public void incrementarChamadas(int idSenha) throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação");
        String sql = "UPDATE senha SET numero_chamadas = numero_chamadas + 1 WHERE id_senha = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSenha);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna a próxima senha da fila respeitando FIFO com prioridade.
     * Senhas prioritárias (servico.prioritario = TRUE) são chamadas primeiro.
     */
    public Senha proximaDaFila() throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação");

        String sql =
            "SELECT s.id_senha, s.codigo, s.id_servico, sv.nome, sv.prioritario, " +
            "       s.estado, s.numero_chamadas, s.data_emissao " +
            "FROM senha s " +
            "JOIN servico sv ON s.id_servico = sv.id_servico " +
            "WHERE s.estado = 'EM_ESPERA' " +
            "ORDER BY sv.prioritario DESC, s.data_emissao ASC " +
            "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    /** Lista todas as senhas do dia com info do serviço. */
    public List<Senha> listarHoje() throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) throw new SQLException("Sem ligação");
        List<Senha> lista = new ArrayList<>();
        String sql =
            "SELECT s.id_senha, s.codigo, s.id_servico, sv.nome, sv.prioritario, " +
            "       s.estado, s.numero_chamadas, s.data_emissao " +
            "FROM senha s JOIN servico sv ON s.id_servico = sv.id_servico " +
            "WHERE DATE(s.data_emissao) = CURDATE() " +
            "ORDER BY s.data_emissao DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Conta senhas em espera actualmente. */
    public int contarEmEspera() throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) return 0;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM senha WHERE estado='EM_ESPERA' AND DATE(data_emissao)=CURDATE()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Conta total de senhas emitidas hoje. */
    public int contarHoje() throws SQLException {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) return 0;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM senha WHERE DATE(data_emissao)=CURDATE()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Senha mapear(ResultSet rs) throws SQLException {
        Senha s = new Senha();
        s.setIdSenha(rs.getInt("id_senha"));
        s.setCodigo(rs.getString("codigo"));
        s.setIdServico(rs.getInt("id_servico"));
        s.setNomeServico(rs.getString("nome"));
        s.setPrioritario(rs.getBoolean("prioritario"));
        try { s.setEstado(Estado.valueOf(rs.getString("estado"))); }
        catch (IllegalArgumentException e) { s.setEstado(Estado.EM_ESPERA); }
        s.setNumeroChamadas(rs.getInt("numero_chamadas"));
        Timestamp ts = rs.getTimestamp("data_emissao");
        if (ts != null) s.setDataEmissao(ts.toLocalDateTime());
        return s;
    }

    // ── Métodos para gráficos do Painel do Gerente ─────────────────

    /** Conta senhas de hoje por serviço (para BarChart e PieChart). */
    public java.util.Map<String, Integer> contarPorServico() throws SQLException {
        java.util.Map<String, Integer> mapa = new LinkedHashMap<>();
        Connection conn = app.DatabaseConnection.getConexao();
        if (conn == null) return mapa;
        String sql = "SELECT sv.nome, COUNT(*) AS total " +
                     "FROM senha s JOIN servico sv ON s.id_servico = sv.id_servico " +
                     "WHERE DATE(s.data_emissao) = CURDATE() " +
                     "GROUP BY sv.nome ORDER BY total DESC";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) mapa.put(rs.getString("nome"), rs.getInt("total"));
        }
        return mapa;
    }

    /** Conta senhas de hoje por estado (para PieChart de distribuição). */
    public java.util.Map<String, Integer> contarPorEstado() throws SQLException {
        java.util.Map<String, Integer> mapa = new LinkedHashMap<>();
        Connection conn = app.DatabaseConnection.getConexao();
        if (conn == null) return mapa;
        String sql = "SELECT estado, COUNT(*) AS total FROM senha " +
                     "WHERE DATE(data_emissao) = CURDATE() GROUP BY estado";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) mapa.put(rs.getString("estado"), rs.getInt("total"));
        }
        return mapa;
    }

    /** Conta senhas por hora do dia (para LineChart de evolução). */
    public java.util.Map<String, Integer> evolucaoPorHora() throws SQLException {
        java.util.Map<String, Integer> mapa = new LinkedHashMap<>();
        Connection conn = app.DatabaseConnection.getConexao();
        if (conn == null) return mapa;
        String sql = "SELECT LPAD(HOUR(data_emissao), 2, '0') AS hora, COUNT(*) AS total " +
                     "FROM senha WHERE DATE(data_emissao) = CURDATE() " +
                     "GROUP BY HOUR(data_emissao) ORDER BY hora";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) mapa.put(rs.getString("hora") + "h", rs.getInt("total"));
        }
        return mapa;
    }

    /** Conta atendimentos concluídos por utilizador hoje (para tabela de desempenho). */
    public java.util.Map<String, Integer> desempenhoBalconistas() throws SQLException {
        java.util.Map<String, Integer> mapa = new LinkedHashMap<>();
        Connection conn = app.DatabaseConnection.getConexao();
        if (conn == null) return mapa;
        String sql = "SELECT u.nome, COUNT(*) AS total " +
                     "FROM atendimento a JOIN utilizador u ON a.id_utilizador = u.id_utilizador " +
                     "WHERE DATE(a.hora_chamada) = CURDATE() AND a.hora_fim IS NOT NULL " +
                     "GROUP BY u.nome ORDER BY total DESC LIMIT 5";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) mapa.put(rs.getString("nome"), rs.getInt("total"));
        }
        return mapa;
    }

}