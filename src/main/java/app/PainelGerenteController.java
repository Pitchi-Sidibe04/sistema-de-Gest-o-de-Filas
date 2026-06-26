package app;

import app.dao.SenhaDAO;
import app.model.Senha;
import app.service.ExcelService;
import app.service.LogService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PainelGerenteController {

    // ── Cards ─────────────────────────────────────────────────────
    @FXML private Label lblBemVindo;
    @FXML private Label lblTotalHoje;
    @FXML private Label lblTotalSub;
    @FXML private Label lblConcluidas;
    @FXML private Label lblConclSub;
    @FXML private Label lblEmEspera;
    @FXML private Label lblTempoMedio;

    // ── Gráficos ──────────────────────────────────────────────────
    @FXML private LineChart<String, Number>  chartEvolucao;
    @FXML private BarChart<String, Number>   chartServicos;
    @FXML private PieChart                   chartDistribuicao;

    // ── Tabelas ───────────────────────────────────────────────────
    @FXML private TableView<Senha>                  tabelaSenhas;
    @FXML private TableColumn<Senha, String>        colCodigo;
    @FXML private TableColumn<Senha, String>        colServico;
    @FXML private TableColumn<Senha, String>        colEstado;
    @FXML private TableColumn<Senha, String>        colHora;

    @FXML private TableView<BalconistaRow>           tabelaBalconistas;
    @FXML private TableColumn<BalconistaRow, String> colBalcNome;
    @FXML private TableColumn<BalconistaRow, Number> colBalcAten;

    // ── Services ──────────────────────────────────────────────────
    private final SenhaDAO     senhaDAO = new SenhaDAO();
    private final ExcelService excelSvc = new ExcelService();

    private static final DateTimeFormatter FMT_HORA =
        DateTimeFormatter.ofPattern("HH:mm");

    // ── INIT ──────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        String nome = Sessao.get().getNome();
        if (lblBemVindo != null)
            lblBemVindo.setText(nome != null ? nome : "Gerente");

        configurarTabelaSenhas();
        configurarTabelaBalconistas();
        atualizar();
    }

    private void configurarTabelaSenhas() {
        if (colCodigo  != null) colCodigo .setCellValueFactory(new PropertyValueFactory<>("codigo"));
        if (colServico != null) colServico.setCellValueFactory(new PropertyValueFactory<>("nomeServico"));
        if (colEstado  != null) colEstado .setCellValueFactory(new PropertyValueFactory<>("estadoFormatado"));
        if (colHora    != null) colHora   .setCellValueFactory(d -> {
            Senha s = d.getValue();
            String h = s.getDataEmissao() != null ? s.getDataEmissao().format(FMT_HORA) : "—";
            return new SimpleStringProperty(h);
        });
    }

    private void configurarTabelaBalconistas() {
        if (colBalcNome != null) colBalcNome.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().nome()));
        if (colBalcAten != null) colBalcAten.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().atendimentos()));
    }

    // ── ACÇÕES ────────────────────────────────────────────────────

    @FXML
    public void atualizar() {
        atualizarCards();
        atualizarGraficoLinha();
        atualizarGraficoBarras();
        atualizarGraficoPizza();
        atualizarTabelaBalconistas();
        atualizarTabelaSenhas();
    }

    @FXML
    private void gerarExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar relatório Excel");
        fc.setInitialFileName("Relatorio_" + java.time.LocalDate.now() + ".xlsx");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));

        File ficheiro = fc.showSaveDialog(
            tabelaSenhas != null ? tabelaSenhas.getScene().getWindow() : null);
        if (ficheiro == null) return;

        boolean ok = excelSvc.gerarRelatorio(ficheiro.getAbsolutePath());
        Alert a = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        a.setTitle(ok ? "Excel gerado" : "Erro");
        a.setHeaderText(null);
        a.setContentText(ok ? "Guardado em:\n" + ficheiro.getAbsolutePath()
                            : "Não foi possível gerar o relatório.");
        a.showAndWait();
    }

    @FXML
    private void irMenuPrincipal() { App.mudarCena("Menu.fxml"); }

    @FXML
    private void terminarSessao() {
        LogService.logout();
        Sessao.get().terminar();
        App.mudarCenaLogin("Login.fxml");
    }

    // ── ACTUALIZAÇÃO DOS CARDS ────────────────────────────────────

    private void atualizarCards() {
        try {
            int total    = senhaDAO.contarHoje();
            int emEspera = senhaDAO.contarEmEspera();
            int concl    = total - emEspera;

            if (lblTotalHoje  != null) lblTotalHoje .setText(String.valueOf(total));
            if (lblEmEspera   != null) lblEmEspera  .setText(String.valueOf(emEspera));
            if (lblConcluidas != null) lblConcluidas.setText(String.valueOf(concl));

        } catch (SQLException e) {
            System.err.println("⚠ Erro ao carregar cards: " + e.getMessage());
        }
    }

    // ── GRÁFICO DE LINHA: Evolução por hora ───────────────────────

    private void atualizarGraficoLinha() {
        if (chartEvolucao == null) return;
        chartEvolucao.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Senhas");

        try {
            Map<String, Integer> dados = senhaDAO.evolucaoPorHora();
            if (dados.isEmpty()) {
                // dados de demonstração para a apresentação
                serie.getData().addAll(
                    ponto("08h",3), ponto("09h",8), ponto("10h",12),
                    ponto("11h",7), ponto("12h",4), ponto("14h",9),
                    ponto("15h",11),ponto("16h",6)
                );
            } else {
                dados.forEach((h, v) -> serie.getData().add(ponto(h, v)));
            }
        } catch (SQLException e) {
            serie.getData().addAll(
                ponto("08h",3), ponto("09h",8), ponto("10h",12),
                ponto("11h",7), ponto("12h",4)
            );
        }

        chartEvolucao.getData().add(serie);

        // Estilizar linha em verde
        serie.getNode().setStyle("-fx-stroke: #006d57; -fx-stroke-width: 2;");
    }

    // ── GRÁFICO DE BARRAS: Serviços mais procurados ───────────────

    private void atualizarGraficoBarras() {
        if (chartServicos == null) return;
        chartServicos.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Senhas");

        try {
            Map<String, Integer> dados = senhaDAO.contarPorServico();
            if (dados.isEmpty()) {
                serie.getData().addAll(
                    ponto("Caixa",32), ponto("Contas",18),
                    ponto("Cartões",14), ponto("Prioritário",8), ponto("Outros",10)
                );
            } else {
                dados.forEach((s, v) -> serie.getData().add(ponto(s, v)));
            }
        } catch (SQLException e) {
            serie.getData().addAll(
                ponto("Caixa",32), ponto("Contas",18),
                ponto("Cartões",14), ponto("Outros",10)
            );
        }

        chartServicos.getData().add(serie);
    }

    // ── GRÁFICO DE PIZZA: Distribuição por estado ─────────────────

    private void atualizarGraficoPizza() {
        if (chartDistribuicao == null) return;
        chartDistribuicao.getData().clear();

        try {
            Map<String, Integer> dados = senhaDAO.contarPorEstado();
            if (dados.isEmpty()) {
                chartDistribuicao.getData().addAll(
                    fatia("Em Espera", 8),
                    fatia("Atendidas", 24),
                    fatia("Ausentes",  4)
                );
            } else {
                dados.forEach((estado, v) ->
                    chartDistribuicao.getData().add(fatia(formatarEstado(estado), v)));
            }
        } catch (SQLException e) {
            chartDistribuicao.getData().addAll(
                fatia("Em Espera", 8),
                fatia("Atendidas", 24),
                fatia("Ausentes",  4)
            );
        }
    }

    // ── TABELA DE BALCONISTAS ─────────────────────────────────────

    private void atualizarTabelaBalconistas() {
        if (tabelaBalconistas == null) return;

        ObservableList<BalconistaRow> lista = FXCollections.observableArrayList();
        try {
            Map<String, Integer> dados = senhaDAO.desempenhoBalconistas();
            if (dados.isEmpty()) {
                // dados de demonstração
                lista.addAll(
                    new BalconistaRow("Maria Santos",  12),
                    new BalconistaRow("João Pereira",   9),
                    new BalconistaRow("Ana Oliveira",   7)
                );
            } else {
                dados.forEach((nome, v) -> lista.add(new BalconistaRow(nome, v)));
            }
        } catch (SQLException e) {
            lista.addAll(new BalconistaRow("—", 0));
        }
        tabelaBalconistas.setItems(lista);
    }

    // ── TABELA DE SENHAS ──────────────────────────────────────────

    private void atualizarTabelaSenhas() {
        if (tabelaSenhas == null) return;
        try {
            List<Senha> lista = senhaDAO.listarHoje();
            tabelaSenhas.setItems(FXCollections.observableArrayList(lista));
        } catch (SQLException e) {
            System.err.println("⚠ Erro ao listar senhas: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private XYChart.Data<String, Number> ponto(String x, Number y) {
        return new XYChart.Data<>(x, y);
    }

    private PieChart.Data fatia(String nome, double valor) {
        return new PieChart.Data(nome, valor);
    }

    private String formatarEstado(String estado) {
        return switch (estado) {
            case "EM_ESPERA"       -> "Em Espera";
            case "CHAMADA"         -> "Chamada";
            case "EM_ATENDIMENTO"  -> "Em Atendimento";
            case "CONCLUIDA"       -> "Atendida";
            case "AUSENTE"         -> "Ausente";
            case "CANCELADA"       -> "Cancelada";
            default                -> estado;
        };
    }

    // ── Record auxiliar para a tabela de balconistas ──────────────
    public record BalconistaRow(String nome, int atendimentos) {}
}
