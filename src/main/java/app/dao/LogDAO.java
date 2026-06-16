package app.dao;

import app.DatabaseConnection;
import app.model.LogAtividade;
import app.model.LogAtividade.Acao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para registo e leitura de logs de actividade.
 */
public class LogDAO {

    /**
     * Regista uma acção no log. Silencioso se a BD não estiver disponível
     * (não deve impedir o fluxo principal da aplicação).
     */
    public void registar(LogAtividade log) {
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) {
            System.out.printf("[LOG-OFFLINE] %s | %s | %s%n",
                log.getNomeUtilizador(), log.getAcao(), log.getDescricao());
            return;
        }
        String sql = "INSERT INTO log_atividade (id_utilizador, acao, descricao) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getIdUtilizador());
            ps.setString(2, log.getAcao().name());
            // Inclui balcão na descrição se disponível
            String desc = log.getDescricao();
            if (log.getIdBalcao() != null) desc += " | Balcão: " + log.getIdBalcao();
            ps.setString(3, desc);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("✗ Erro ao registar log: " + e.getMessage());
        }
    }

    /** Lê os últimos N logs para exibição no painel do gerente. */
    public List<String[]> listarRecentes(int limite) {
        List<String[]> lista = new ArrayList<>();
        Connection conn = DatabaseConnection.getConexao();
        if (conn == null) return lista;
        String sql = "SELECT u.nome, l.acao, l.descricao, l.data_hora " +
                     "FROM log_atividade l " +
                     "JOIN utilizador u ON l.id_utilizador = u.id_utilizador " +
                     "ORDER BY l.data_hora DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{
                    rs.getString("nome"),
                    rs.getString("acao"),
                    rs.getString("descricao"),
                    rs.getString("data_hora")
                });
            }
        } catch (SQLException e) {
            System.err.println("✗ Erro ao listar logs: " + e.getMessage());
        }
        return lista;
    }
}
