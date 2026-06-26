package app;

import app.dao.SenhaDAO;
import app.model.Senha;
import app.model.Senha.Estado;
import app.service.FilaService;
import app.service.LogService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PainelBalconistaController {

    @FXML private Label  lblBemVindo;
    @FXML private Label  lblHora;
    @FXML private Label  lblData;
    @FXML private Label  lblLetraAtual;
    @FXML private Label  lblNumeroAtual;
    @FXML private Label  lblServicoAtual;
    @FXML private Label  lblTempoAtendimento;
    @FXML private Button btnChamarProxima;
    @FXML private Button btnFinalizar;
    @FXML private Button btnAusente;
    @FXML private VBox   boxProximas;
    @FXML private VBox   boxUltimas;

    private final SenhaDAO    senhaDAO = new SenhaDAO();
    private final FilaService filaSvc  = FilaService.get();

    private Senha         senhaActual;
    private LocalDateTime inicioAtendimento;
    private Timeline      relogio;
    private Timeline      cronometro;

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<HistoricoItem> ultimasSenhas = new ArrayList<>();
    private static final int MAX_LISTA = 3;

    @FXML
    public void initialize() {
        String nome = Sessao.get().getNome();
        lblBemVindo.setText("Bom dia" + (nome != null ? ", " + nome : ""));

        atualizarRelogio();
        relogio = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarRelogio()));
        relogio.setCycleCount(Timeline.INDEFINITE);
        relogio.play();

        cronometro = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarCronometro()));
        cronometro.setCycleCount(Timeline.INDEFINITE);
        cronometro.play();

        atualizarBotoes();
        carregarProximas();
        renderizarUltimas();
    }

    private void atualizarRelogio() {
        LocalDateTime agora = LocalDateTime.now();
        lblHora.setText(agora.format(FMT_HORA));
        lblData.setText(agora.format(FMT_DATA));
    }

    private void atualizarCronometro() {
        if (senhaActual == null || inicioAtendimento == null) {
            lblTempoAtendimento.setText("00:00:00");
            return;
        }
        long s = java.time.Duration.between(inicioAtendimento, LocalDateTime.now()).getSeconds();
        lblTempoAtendimento.setText(String.format("%02d:%02d:%02d", s/3600, (s%3600)/60, s%60));
    }

    @FXML
    private void chamarProxima() {
        try {
            Senha proxima = filaSvc.chamarProxima(1);
            if (proxima == null) {
                mostrarSemSenhas();
                return;
            }
            senhaActual       = proxima;
            inicioAtendimento = LocalDateTime.now();
            mostrarSenhaAtual();
            atualizarBotoes();
            carregarProximas();
        } catch (SQLException e) {
            // ── Correcção: mostrar erro ao utilizador em vez de silêncio ──
            mostrarErro("Erro ao chamar senha",
                "Não foi possível chamar a próxima senha.\n" +
                "Verifique a ligação à base de dados.\n\n" +
                "Detalhe: " + e.getMessage());
        }
    }

    @FXML
    private void clienteAusente() {
        if (senhaActual == null) return;

        try {
            adicionarAoHistorico(senhaActual, "Ausente");

            // ── Correcção principal: tentar marcar na BD, mas continuar
            //    com a fila em memória mesmo se a BD falhar ─────────────
            Senha proxima;
            try {
                proxima = filaSvc.clienteAusente(senhaActual, 1);
            } catch (SQLException dbErr) {
                // BD falhou — avança a fila em memória manualmente
                System.err.println("⚠ BD indisponível para ausente, avançando fila em memória: "
                    + dbErr.getMessage());
                proxima = filaSvc.chamarProximaEmMemoria();
            }

            if (proxima == null) {
                senhaActual       = null;
                inicioAtendimento = null;
                mostrarSemSenhas();
            } else {
                senhaActual       = proxima;
                inicioAtendimento = LocalDateTime.now();
                mostrarSenhaAtual();
            }

            atualizarBotoes();
            carregarProximas();
            renderizarUltimas();

        } catch (Exception e) {
            mostrarErro("Erro ao marcar ausência",
                "Ocorreu um erro ao processar o cliente ausente.\n\n" +
                "Detalhe: " + e.getMessage());
        }
    }

    @FXML
    private void finalizarAtendimento() {
        if (senhaActual == null) return;
        try {
            adicionarAoHistorico(senhaActual, formatarDuracao());
            filaSvc.concluir(senhaActual);
            senhaActual       = null;
            inicioAtendimento = null;
            mostrarSemSenhas();
            atualizarBotoes();
            renderizarUltimas();
        } catch (SQLException e) {
            mostrarErro("Erro ao finalizar",
                "Não foi possível finalizar o atendimento.\n\n" +
                "Detalhe: " + e.getMessage());
        }
    }

    @FXML private void abrirAtendimentos() { System.out.println("→ Atendimentos"); }
    @FXML private void abrirHistorico()     { System.out.println("→ Histórico"); }

    @FXML
    private void terminarSessao() {
        LogService.logout();
        Sessao.get().terminar();
        App.mudarCenaLogin("Login.fxml");
    }

    private void mostrarSenhaAtual() {
        if (senhaActual == null) { mostrarSemSenhas(); return; }
        String codigo = senhaActual.getCodigo();
        lblLetraAtual.setText(codigo.substring(0, 1));
        lblNumeroAtual.setText(codigo.length() > 1 ? codigo.substring(1) : "");
        lblServicoAtual.setText(senhaActual.getNomeServico() != null
            ? senhaActual.getNomeServico() : "—");
    }

    private void mostrarSemSenhas() {
        lblLetraAtual.setText("—");
        lblNumeroAtual.setText("000");
        lblServicoAtual.setText("Sem atendimento");
        lblTempoAtendimento.setText("00:00:00");
    }

    private void atualizarBotoes() {
        boolean em = senhaActual != null;
        btnFinalizar.setDisable(!em);
        btnAusente.setDisable(!em);
    }

    private void carregarProximas() {
        boxProximas.getChildren().clear();
        List<Senha> emEspera;
        try {
            emEspera = senhaDAO.listarHoje().stream()
                .filter(s -> s.getEstado() == Estado.EM_ESPERA)
                .limit(MAX_LISTA).toList();
        } catch (SQLException e) {
            emEspera = List.of();
        }
        if (emEspera.isEmpty()) {
            Label v = new Label("Nenhuma senha em espera");
            v.getStyleClass().add("balc-empty-label");
            v.setMaxWidth(Double.MAX_VALUE);
            boxProximas.getChildren().add(v);
            return;
        }
        for (Senha s : emEspera)
            boxProximas.getChildren().add(criarLinhaSenha(s.getCodigo(), s.getNomeServico(), null));
    }

    private void renderizarUltimas() {
        boxUltimas.getChildren().clear();
        if (ultimasSenhas.isEmpty()) {
            Label v = new Label("Ainda sem atendimentos hoje");
            v.getStyleClass().add("balc-empty-label");
            v.setMaxWidth(Double.MAX_VALUE);
            boxUltimas.getChildren().add(v);
            return;
        }
        for (HistoricoItem item : ultimasSenhas)
            boxUltimas.getChildren().add(criarLinhaSenha(item.codigo, item.servico, item.tempo));
    }

    private HBox criarLinhaSenha(String codigo, String servico, String tempo) {
        String letra  = codigo.substring(0, 1);
        String numero = codigo.length() > 1 ? codigo.substring(1) : "";

        VBox badge = new VBox();
        badge.getStyleClass().add("balc-badge");
        badge.setAlignment(javafx.geometry.Pos.CENTER);
        Label lLetra = new Label(letra);
        lLetra.getStyleClass().add("balc-badge-letra");
        badge.getChildren().add(lLetra);

        Label lNum  = new Label(numero);  lNum.getStyleClass().add("balc-row-numero");
        Label lServ = new Label(servico != null ? servico : "—");
        lServ.getStyleClass().add("balc-row-servico");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, badge, lNum, lServ, spacer);
        row.getStyleClass().add("balc-list-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        if (tempo != null) {
            Label lTempo = new Label(tempo);
            lTempo.getStyleClass().add("balc-row-tempo");
            row.getChildren().add(lTempo);
        }
        return row;
    }

    private void adicionarAoHistorico(Senha senha, String tempo) {
        ultimasSenhas.add(0, new HistoricoItem(senha.getCodigo(), senha.getNomeServico(), tempo));
        while (ultimasSenhas.size() > MAX_LISTA)
            ultimasSenhas.remove(ultimasSenhas.size() - 1);
    }

    private String formatarDuracao() {
        if (inicioAtendimento == null) return "00:00:00";
        long s = java.time.Duration.between(inicioAtendimento, LocalDateTime.now()).getSeconds();
        return String.format("%02d:%02d:%02d", s/3600, (s%3600)/60, s%60);
    }

    private void mostrarErro(String titulo, String mensagem) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensagem);
        a.showAndWait();
    }

    private record HistoricoItem(String codigo, String servico, String tempo) {}
}
