package app;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtSenha;

    @FXML
    private Button btnEntrar;

    @FXML
    private Hyperlink linkEsqueceu;

    @FXML
    private void handleLogin() {
        String usuario = txtUsuario.getText().trim();
        String senha   = txtSenha.getText().trim();

        if (usuario.isEmpty() || senha.isEmpty()) {
            mostrarMensagem("Erro", "Preencha todos os campos.");
            return;
        }

        if (autenticarUtilizador(usuario, senha)) {
            App.mudarCena("Menu.fxml");
        } else {
            mostrarMensagem("Acesso negado", "Utilizador ou senha incorretos.");
        }
    }

    private boolean autenticarUtilizador(String username, String senha) {
        String sql = "SELECT id_utilizador FROM utilizador " +
                     "WHERE username = ? AND senha = ? AND estado = 'ATIVO'";

        Connection conn = DatabaseConnection.getConexao();

        if (conn == null) {
            // Fallback para credenciais de emergência se não houver BD
            System.err.println("⚠ BD indisponível — a usar credenciais de emergência");
            return username.equals("admin") && senha.equals("1234");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, senha);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("✗ Erro na autenticação: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void esqueceuSenha() {
        mostrarMensagem("Esqueceu a senha", "Contacte o administrador do sistema.");
    }

    private void mostrarMensagem(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
