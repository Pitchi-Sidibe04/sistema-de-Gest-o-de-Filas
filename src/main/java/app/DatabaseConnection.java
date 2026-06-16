package app;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static Connection conexao = null;

    private static String url;
    private static String usuario;
    private static String senha;

    static {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class
                .getResourceAsStream("/db.properties")) {
            if (is != null) {
                props.load(is);
                url     = props.getProperty("db.url");
                usuario = props.getProperty("db.usuario");
                senha   = props.getProperty("db.senha");
            } else {
                System.err.println("⚠ db.properties não encontrado — a usar valores padrão");
                url     = "jdbc:mysql://localhost:3306/sistema_senhas";
                usuario = "root";
                senha   = "root";
            }
        } catch (IOException e) {
            System.err.println("✗ Erro ao ler db.properties: " + e.getMessage());
        }
    }

    public static Connection getConexao() {
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = DriverManager.getConnection(url, usuario, senha);
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
