package app;

import app.model.LogAtividade.Acao;
import app.dao.LogDAO;
import app.service.LogService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class LoginController {

    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private Button        btnEntrar;
    @FXML private Hyperlink     linkEsqueceu;
    @FXML private Hyperlink     linkCriarConta;

    @FXML
    private void handleLogin() {
        String username = txtUsuario.getText().trim();
        String senha    = txtSenha.getText().trim();

        if (username.isEmpty() || senha.isEmpty()) {
            mostrarAlerta("Erro", "Preencha todos os campos.");
            return;
        }

        if (autenticarUtilizador(username, senha)) {
            LogService.login();
            // CORRECÇÃO: a navegação após login pertence à janela de Acesso
            // (Login), não à janela do Quiosque do Cliente.
            //
            // Encaminhamento por nível de acesso:
            //  - Administrador / Supervisor → PainelGerente.fxml
            //  - Atendente (nível 3)         → PainelBalconista.fxml
            if (Sessao.get().isGerente()) {
                App.mudarCenaLogin("PainelGerente.fxml");
            } else {
                App.mudarCenaLogin("PainelBalconista.fxml");
            }
        } else {
            mostrarAlerta("Acesso negado", "Utilizador ou senha incorretos.");
        }
    }

    private boolean autenticarUtilizador(String username, String senhaInput) {
        String sql = "SELECT u.id_utilizador, u.nome, u.senha, n.nome AS nivel " +
                     "FROM utilizador u " +
                     "JOIN nivel_acesso n ON u.id_nivel = n.id_nivel " +
                     "WHERE u.username = ? AND u.estado = 'ATIVO'";

        Connection conn = DatabaseConnection.getConexao();

        if (conn == null) {
            // Fallback offline
            if (username.equals("gerente") && senhaInput.equals("1234")) {
                Sessao.get().iniciar(0, "Gerente Demo", username, "ADMINISTRADOR");
                return true;
            }
            if (username.equals("admin") && senhaInput.equals("1234")) {
                Sessao.get().iniciar(0, "Admin Demo", username, "ATENDENTE");
                return true;
            }
            return false;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("senha");
                // Suporta senhas ainda em texto simples (migração gradual)
                boolean ok;
                if (hash != null && hash.startsWith("$2")) {
                    ok = BCrypt.checkpw(senhaInput, hash);
                } else {
                    ok = senhaInput.equals(hash);
                }
                if (ok) {
                    Sessao.get().iniciar(
                        rs.getInt("id_utilizador"),
                        rs.getString("nome"),
                        username,
                        rs.getString("nivel")
                    );
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("✗ Erro na autenticação: " + e.getMessage());
            return false;
        }
    }

    @FXML private void esqueceuSenha() {
        mostrarAlerta("Esqueceu a senha", "Contacte o administrador do sistema.");
    }

    @FXML private void abrirCriarConta() {
        App.mudarCenaLogin("CriarConta.fxml");
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}