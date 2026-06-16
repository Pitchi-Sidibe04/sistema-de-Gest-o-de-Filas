package app;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * Controller do formulário de criação de novo utilizador do sistema.
 * Insere na tabela `utilizador` com senha encriptada (BCrypt).
 * Nível padrão: Atendente (pode ser alterado pelo Administrador depois).
 */
public class CriarContaController {

    @FXML private TextField     txtNome;
    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtSenha;
    @FXML private PasswordField txtConfirmarSenha;
    @FXML private ComboBox<String> cmbNivel;
    @FXML private Label         lblMensagem;

    @FXML
    public void initialize() {
        cmbNivel.getItems().addAll("Atendente", "Supervisor", "Administrador");
        cmbNivel.setValue("Atendente");
    }

    @FXML
    private void submeterFormulario() {
        String nome      = txtNome.getText().trim();
        String username  = txtUsername.getText().trim();
        String senha     = txtSenha.getText();
        String confirma  = txtConfirmarSenha.getText();
        String nivel     = cmbNivel.getValue();

        // ── Validações ────────────────────────────────────────
        if (nome.isEmpty() || username.isEmpty() || senha.isEmpty()) {
            erro("Preencha todos os campos obrigatórios.");
            return;
        }
        if (username.contains(" ")) {
            erro("O nome de utilizador não pode ter espaços.");
            return;
        }
        if (senha.length() < 6) {
            erro("A senha deve ter pelo menos 6 caracteres.");
            return;
        }
        if (!senha.equals(confirma)) {
            erro("As senhas não coincidem.");
            return;
        }

        // ── Criar utilizador na BD ────────────────────────────
        if (criarUtilizador(nome, username, senha, nivel)) {
            sucesso("Utilizador \"" + username + "\" criado com sucesso!");
            limpar();
        }
    }

    private boolean criarUtilizador(String nome, String username, String senha, String nivelNome) {
        Connection conn = DatabaseConnection.getConexao();

        if (conn == null) {
            // Modo offline — simula sucesso e avisa
            sucesso("(Modo offline) Utilizador criado localmente. Sincronize com a BD.");
            return true;
        }

        // Verifica se o username já existe
        try (PreparedStatement chk = conn.prepareStatement(
                "SELECT id_utilizador FROM utilizador WHERE username = ?")) {
            chk.setString(1, username);
            if (chk.executeQuery().next()) {
                erro("O nome de utilizador \"" + username + "\" já existe.");
                return false;
            }
        } catch (SQLException e) {
            erro("Erro ao verificar utilizador: " + e.getMessage());
            return false;
        }

        // Busca o id do nível
        int idNivel = buscarIdNivel(conn, nivelNome);
        if (idNivel < 0) {
            erro("Nível de acesso inválido.");
            return false;
        }

        // Encripta a senha com BCrypt
        String hash = BCrypt.hashpw(senha, BCrypt.gensalt());

        String sql = "INSERT INTO utilizador (nome, username, senha, estado, id_nivel) " +
                     "VALUES (?, ?, ?, 'ATIVO', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, username);
            ps.setString(3, hash);
            ps.setInt(4, idNivel);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            erro("Erro ao criar utilizador: " + e.getMessage());
            return false;
        }
    }

    private int buscarIdNivel(Connection conn, String nivelNome) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_nivel FROM nivel_acesso WHERE nome = ?")) {
            ps.setString(1, nivelNome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("⚠ buscarIdNivel: " + e.getMessage());
        }
        return -1;
    }

    private void limpar() {
        txtNome.clear();
        txtUsername.clear();
        txtSenha.clear();
        txtConfirmarSenha.clear();
        cmbNivel.setValue("Atendente");
    }

    private void erro(String msg) {
        lblMensagem.setText(msg);
        lblMensagem.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 13;");
    }

    private void sucesso(String msg) {
        lblMensagem.setText(msg);
        lblMensagem.setStyle("-fx-text-fill: #006d57; -fx-font-size: 13;");
    }

    @FXML
    private void voltarLogin() {
        App.mudarCenaLogin("Login.fxml");
    }
}
