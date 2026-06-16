package app;

import app.model.Senha;
import app.model.Senha.Estado;
import app.dao.SenhaDAO;
import app.service.FilaService;
import app.service.ExcelService;
import app.service.LogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller do Painel do Gerente — nível de acesso Administrador / Supervisor.
 *
 * Funcionalidades:
 *  - Cards de estatísticas em tempo real (total, em espera, concluídas, tempo médio)
 *  - Tabela com todas as senhas emitidas hoje
 *  - Chamar próxima senha, marcar ausente, concluir atendimento
 *  - Gerar relatório Excel do dia
 *  - Terminar sessão e voltar ao Login
 */
public class PainelGerenteController {

    // ── Cards de estatísticas ────────────────────────────────────
    @FXML private Label lblBemVindo;
    @FXML private Label lblTotalHoje;
    @FXML private Label lblEmEspera;
    @FXML private Label lblConcluidas;
    @FXML private Label lblTempoMedio;

    // ── Barra de atendimento ─────────────────────────────────────
    @FXML private Label  lblSenhaActual;
    @FXML private Button btnChamarProxima;
    @FXML private Button btnClienteAusente;
    @FXML private Button btnConcluir;

    // ── Tabela de senhas ─────────────────────────────────────────
    @FXML private TableView<Senha>        tabelaSenhas;
    @FXML private TableColumn<Senha, String> colCodigo;
    @FXML private TableColumn<Senha, String> colServico;
    @FXML private TableColumn<Senha, String> colEstado;
    @FXML private TableColumn<Senha, String> colHora;

    // ── Services ─────────────────────────────────────────────────
    private final SenhaDAO    senhaDAO = new SenhaDAO();
    private final FilaService filaSvc  = FilaService.get();
    private final ExcelService excelSvc = new ExcelService();

    // ── Estado interno ───────────────────────────────────────────
    private Senha senhaActual;

    private static final DateTimeFormatter FMT_HORA =
        DateTimeFormatter.ofPattern("HH:mm");

    // ── INIT ─────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Saudação
        String nome = Sessao.get().getNome();
        if (lblBemVindo != null)
            lblBemVindo.setText(nome != null ? nome : "Gerente");

        // Configurar colunas da tabela
        if (colCodigo  != null) colCodigo .setCellValueFactory(new PropertyValueFactory<>("codigo"));
        if (colServico != null) colServico.setCellValueFactory(new PropertyValueFactory<>("nomeServico"));
        if (colEstado  != null) colEstado .setCellValueFactory(new PropertyValueFactory<>("estadoFormatado"));
        if (colHora    != null) colHora   .setCellValueFactory(d -> {
            Senha s = d.getValue();
            String hora = s.getDataEmissao() != null
                ? s.getDataEmissao().format(FMT_HORA) : "—";
            return new javafx.beans.property.SimpleStringProperty(hora);
        });

        atualizar();
    }

    // ── ACÇÕES ────────────────────────────────────────────────────

    /** Actualiza cards de estatísticas e tabela. */
    @FXML
    private void atualizar() {
        try {
            // Cards
            int total    = senhaDAO.contarHoje();
            int emEspera = senhaDAO.contarEmEspera();

            if (lblTotalHoje  != null) lblTotalHoje .setText(String.valueOf(total));
            if (lblEmEspera   != null) lblEmEspera  .setText(String.valueOf(emEspera));
            if (lblConcluidas != null) lblConcluidas.setText(
                String.valueOf(total - emEspera));

            // Tabela
            List<Senha> lista = senhaDAO.listarHoje();
            ObservableList<Senha> obs = FXCollections.observableArrayList(lista);
            if (tabelaSenhas != null) tabelaSenhas.setItems(obs);

        } catch (SQLException e) {
            System.err.println("⚠ Erro ao atualizar painel: " + e.getMessage());
        }
    }

    /** Chama a próxima senha da fila (prioridade → FIFO). */
    @FXML
    private void chamarProxima() {
        try {
            Senha proxima = filaSvc.chamarProxima(null);

            if (proxima == null) {
                mostrarInfo("Fila vazia", "Não há senhas em espera.");
                return;
            }

            senhaActual = proxima;

            if (lblSenhaActual    != null) lblSenhaActual.setText(senhaActual.getCodigo());
            if (btnClienteAusente != null) btnClienteAusente.setDisable(false);
            if (btnConcluir       != null) btnConcluir.setDisable(false);

            atualizar();

        } catch (SQLException e) {
            System.err.println("⚠ Erro ao chamar próxima: " + e.getMessage());
        }
    }

    /** Marca o cliente actual como ausente e chama automaticamente o próximo. */
    @FXML
    private void clienteAusente() {
        if (senhaActual == null) return;
        try {
            Senha proxima = filaSvc.clienteAusente(senhaActual, null);

            if (proxima != null) {
                senhaActual = proxima;
                if (lblSenhaActual != null) lblSenhaActual.setText(senhaActual.getCodigo());
            } else {
                senhaActual = null;
                if (lblSenhaActual    != null) lblSenhaActual.setText("—");
                if (btnClienteAusente != null) btnClienteAusente.setDisable(true);
                if (btnConcluir       != null) btnConcluir.setDisable(true);
            }

            atualizar();

        } catch (SQLException e) {
            System.err.println("⚠ Erro ao marcar ausente: " + e.getMessage());
        }
    }

    /** Conclui o atendimento actual. */
    @FXML
    private void concluir() {
        if (senhaActual == null) return;
        try {
            filaSvc.concluir(senhaActual);
            senhaActual = null;

            if (lblSenhaActual    != null) lblSenhaActual.setText("—");
            if (btnClienteAusente != null) btnClienteAusente.setDisable(true);
            if (btnConcluir       != null) btnConcluir.setDisable(true);

            atualizar();

        } catch (SQLException e) {
            System.err.println("⚠ Erro ao concluir: " + e.getMessage());
        }
    }

    /** Gera o relatório Excel do dia com FileChooser. */
    @FXML
    private void gerarExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar relatório Excel");
        fc.setInitialFileName("Relatorio_" +
            java.time.LocalDate.now().toString() + ".xlsx");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel", "*.xlsx"));

        File ficheiro = fc.showSaveDialog(
            tabelaSenhas != null ? tabelaSenhas.getScene().getWindow() : null);

        if (ficheiro == null) return;

        boolean ok = excelSvc.gerarRelatorio(ficheiro.getAbsolutePath());
        mostrarInfo(
            ok ? "Relatório gerado" : "Erro",
            ok ? "Relatório guardado em:\n" + ficheiro.getAbsolutePath()
               : "Erro ao gerar relatório Excel."
        );
    }

    /** Navega para o Menu principal do quiosque (janela 1). */
    @FXML
    private void irMenuPrincipal() {
        App.mudarCena("Menu.fxml");
    }

    /** Termina a sessão e volta ao Login. */
    @FXML
    private void terminarSessao() {
        LogService.logout();
        Sessao.get().terminar();
        App.mudarCenaLogin("Login.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void mostrarInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
