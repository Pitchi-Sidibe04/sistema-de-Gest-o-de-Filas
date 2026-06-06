package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/sistema_senhas";
    private static final String USUARIO  = "root";
    private static final String SENHA    = "root";

    private static Connection conexao = null;

    public static Connection getConexao() {
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = DriverManager.getConnection(URL, USUARIO, SENHA);
                System.out.println("✓ Conexão com a base de dados estabelecida");
            }
        } catch (SQLException e) {
            System.err.println("✗ Erro ao conectar à base de dados: " + e.getMessage());
            conexao = null;
        }
        return conexao;
    }

    public static void fecharConexao() {
        try {
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
                System.out.println("✓ Conexão com a base de dados encerrada");
            }
        } catch (SQLException e) {
            System.err.println("✗ Erro ao fechar conexão: " + e.getMessage());
        }
    }
}
