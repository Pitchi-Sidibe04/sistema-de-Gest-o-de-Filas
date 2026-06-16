package app;

import app.dao.AtendimentoDAO;
import app.model.AtendimentoBalcao;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller do Ecrã de Chamada (Sala de Espera) — documento de visão §7.3.
 *
 * Actualiza periodicamente (a cada 3 segundos):
 *  - "SENHAS NO BALCÃO": a senha actualmente em atendimento em cada um
 *    dos 4 balcões (consulta AtendimentoDAO.listarEmAtendimentoPorBalcao()).
 *  - "PRÓXIMAS SENHAS": até 4 senhas em EM_ESPERA (prioridade → FIFO).
 *  - Relógio: hora e data actuais.
 *
 * Quando um balcão não tem atendimento em curso, mostra "Livre" e a
 * caixa da letra fica em cinzento (estilo ec-balcao-box-vazio).
 */
public class EcraChamadaController {

    @FXML private Label lblHora;
    @FXML private Label lblData;

    // Balcão 1
    @FXML private VBox  boxLetra1;
    @FXML private Label lblLetra1;
    @FXML private Label lblNumero1;
    @FXML private Label lblServico1;

    // Balcão 2
    @FXML private VBox  boxLetra2;
    @FXML private Label lblLetra2;
    @FXML private Label lblNumero2;
    @FXML private Label lblServico2;

    // Balcão 3
    @FXML private VBox  boxLetra3;
    @FXML private Label lblLetra3;
    @FXML private Label lblNumero3;
    @FXML private Label lblServico3;

    // Balcão 4
    @FXML private VBox  boxLetra4;
    @FXML private Label lblLetra4;
    @FXML private Label lblNumero4;
    @FXML private Label lblServico4;

    @FXML private VBox boxProximas;

    private final AtendimentoDAO atendimentoDAO = new AtendimentoDAO();

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_PROXIMAS = 4;
    private static final int NUM_BALCOES  = 4;

    private Timeline relogio;
    private Timeline atualizador;

    @FXML
    public void initialize() {
        atualizarRelogio();
        relogio = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarRelogio()));
        relogio.setCycleCount(Timeline.INDEFINITE);
        relogio.play();

        atualizarDados();
        atualizador = new Timeline(new KeyFrame(Duration.seconds(3), e -> atualizarDados()));
        atualizador.setCycleCount(Timeline.INDEFINITE);
        atualizador.play();
    }

    private void atualizarRelogio() {
        LocalDateTime agora = LocalDateTime.now();
        lblHora.setText(agora.format(FMT_HORA));
        lblData.setText(agora.format(FMT_DATA));
    }

    /** Consulta a BD e actualiza os 4 balcões + lista de próximas senhas. */
    private void atualizarDados() {
        atualizarBalcoes();
        atualizarProximas();
    }

    private void atualizarBalcoes() {
        Map<Integer, AtendimentoBalcao> porBalcao;
        try {
            porBalcao = atendimentoDAO.listarEmAtendimentoPorBalcao();
        } catch (SQLException e) {
            System.err.println("⚠ Erro ao listar atendimentos por balcão: " + e.getMessage());
            porBalcao = Map.of();
        }

        for (int i = 1; i <= NUM_BALCOES; i++) {
            AtendimentoBalcao at = porBalcao.get(i);
            preencherBalcao(i, at);
        }
    }

    /** Preenche os campos de um balcão (1..4) com o atendimento actual, ou "Livre". */
    private void preencherBalcao(int numero, AtendimentoBalcao at) {
        VBox  box;
        Label lblLetra, lblNumero, lblServico;

        switch (numero) {
            case 1 -> { box = boxLetra1; lblLetra = lblLetra1; lblNumero = lblNumero1; lblServico = lblServico1; }
            case 2 -> { box = boxLetra2; lblLetra = lblLetra2; lblNumero = lblNumero2; lblServico = lblServico2; }
            case 3 -> { box = boxLetra3; lblLetra = lblLetra3; lblNumero = lblNumero3; lblServico = lblServico3; }
            case 4 -> { box = boxLetra4; lblLetra = lblLetra4; lblNumero = lblNumero4; lblServico = lblServico4; }
            default -> { return; }
        }

        box.getStyleClass().removeAll("ec-balcao-box", "ec-balcao-box-vazio");

        if (at == null) {
            box.getStyleClass().add("ec-balcao-box-vazio");
            lblLetra.setText("—");
            lblNumero.setText("—");
            lblServico.setText("Livre");
        } else {
            box.getStyleClass().add("ec-balcao-box");
            lblLetra.setText(at.getLetra() != null ? at.getLetra() : "—");
            lblNumero.setText(at.getNumero() != null ? at.getNumero() : "—");
            lblServico.setText(at.getNomeServico() != null ? at.getNomeServico() : "—");
        }
    }

    /** Carrega até MAX_PROXIMAS senhas em espera (prioridade → FIFO). */
    private void atualizarProximas() {
        boxProximas.getChildren().clear();

        List<AtendimentoBalcao> proximas;
        try {
            proximas = atendimentoDAO.listarProximas(MAX_PROXIMAS);
        } catch (SQLException e) {
            System.err.println("⚠ Erro ao listar próximas senhas: " + e.getMessage());
            proximas = List.of();
        }

        if (proximas.isEmpty()) {
            Label vazio = new Label("Nenhuma senha em espera");
            vazio.getStyleClass().add("ec-empty-label");
            vazio.setMaxWidth(Double.MAX_VALUE);
            boxProximas.getChildren().add(vazio);
            return;
        }

        for (AtendimentoBalcao p : proximas) {
            boxProximas.getChildren().add(criarLinhaProxima(p));
        }
    }

    /** Cria uma linha visual (badge letra + número + nome do serviço) para "Próximas Senhas". */
    private HBox criarLinhaProxima(AtendimentoBalcao p) {
        VBox badge = new VBox();
        badge.getStyleClass().add("ec-proxima-badge");
        badge.setAlignment(Pos.CENTER);
        Label lblLetra = new Label(p.getLetra() != null ? p.getLetra() : "—");
        lblLetra.getStyleClass().add("ec-proxima-badge-letra");
        badge.getChildren().add(lblLetra);

        Label lblNumero = new Label(p.getNumero() != null ? p.getNumero() : "—");
        lblNumero.getStyleClass().add("ec-proxima-numero");

        Region divisor = new Region();
        divisor.getStyleClass().add("ec-proxima-divisor");

        Label lblServico = new Label(p.getNomeServico() != null ? p.getNomeServico() : "—");
        lblServico.getStyleClass().add("ec-proxima-servico");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, badge, lblNumero, divisor, lblServico, spacer);
        row.getStyleClass().add("ec-proxima-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }
}
