package app;

import app.dao.SenhaDAO;
import app.model.Senha;
import app.model.Senha.Estado;
import app.service.FilaService;
import app.service.LogService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
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

/**
 * Controller do Painel do Balconista — nível de acesso 3 (Atendente).
 *
 * Funcionalidades:
 *  - Mostra a senha actualmente em atendimento (letra, número, serviço)
 *  - Cronómetro de tempo de atendimento (actualiza a cada segundo)
 *  - "Chamar Próxima" → FilaService.chamarProxima() (prioridade → FIFO)
 *  - "Cliente Ausente" → FilaService.clienteAusente() e chama a próxima automaticamente
 *  - "Finalizar Atendimento" → FilaService.concluir() e regista nas "últimas senhas"
 *  - Lista "Próximas Senhas" (até 3) consultada da BD (estado EM_ESPERA)
 *  - Lista "Últimas Senhas" (até 3) — concluídas nesta sessão, com tempo de atendimento
 *  - Relógio no canto superior direito, actualizado a cada segundo
 */
public class PainelBalconistaController {

    // ── UI: topo ─────────────────────────────────────────────
    @FXML private Label lblBemVindo;
    @FXML private Label lblHora;
    @FXML private Label lblData;

    // ── UI: card "Atendendo" ─────────────────────────────────
    @FXML private Label  lblLetraAtual;
    @FXML private Label  lblNumeroAtual;
    @FXML private Label  lblServicoAtual;
    @FXML private Label  lblTempoAtendimento;

    @FXML private Button btnChamarProxima;
    @FXML private Button btnFinalizar;
    @FXML private Button btnAusente;

    // ── UI: listas ───────────────────────────────────────────
    @FXML private VBox boxProximas;
    @FXML private VBox boxUltimas;

    // ── Services ─────────────────────────────────────────────
    private final SenhaDAO    senhaDAO   = new SenhaDAO();
    private final FilaService filaSvc    = FilaService.get();

    // ── Estado interno ───────────────────────────────────────
    private Senha            senhaActual;
    private LocalDateTime    inicioAtendimento;
    private Timeline         relogio;
    private Timeline         cronometro;

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Histórico de "últimas senhas" concluídas nesta sessão (mais recente primeiro)
    private final List<HistoricoItem> ultimasSenhas = new ArrayList<>();
    private static final int MAX_LISTA = 3;

    // ── INIT ─────────────────────────────────────────────────
    @FXML
    public void initialize() {

        // Saudação
        String nome = Sessao.get().getNome();
        lblBemVindo.setText("Bom dia" + (nome != null ? ", " + nome : ""));

        // Relógio (actualiza a cada segundo)
        atualizarRelogio();
        relogio = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarRelogio()));
        relogio.setCycleCount(Timeline.INDEFINITE);
        relogio.play();

        // Cronómetro de atendimento (actualiza a cada segundo)
        cronometro = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarCronometro()));
        cronometro.setCycleCount(Timeline.INDEFINITE);
        cronometro.play();

        atualizarBotoes();
        carregarProximas();
        renderizarUltimas();
    }

    // ── RELÓGIO / CRONÓMETRO ──────────────────────────────────

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
        long segundos = java.time.Duration.between(inicioAtendimento, LocalDateTime.now()).getSeconds();
        long h = segundos / 3600;
        long m = (segundos % 3600) / 60;
        long s = segundos % 60;
        lblTempoAtendimento.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    // ── ACÇÕES ────────────────────────────────────────────────

    /** Chama a próxima senha (prioridade → FIFO) e inicia o atendimento. */
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
            System.err.println("⚠ Erro ao chamar próxima senha: " + e.getMessage());
        }
    }

    /** Marca a senha actual como AUSENTE e chama automaticamente a próxima. */
    @FXML
    private void clienteAusente() {
        if (senhaActual == null) return;

        try {
            adicionarAoHistorico(senhaActual, "Ausente");
            Senha proxima = filaSvc.clienteAusente(senhaActual, 1);

            if (proxima == null) {
                senhaActual = null;
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

        } catch (SQLException e) {
            System.err.println("⚠ Erro ao marcar ausência: " + e.getMessage());
        }
    }

    /** Conclui o atendimento actual e regista nas "últimas senhas". */
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
            System.err.println("⚠ Erro ao finalizar atendimento: " + e.getMessage());
        }
    }

    @FXML
    private void abrirAtendimentos() {
        // Placeholder — pode ser ligado a uma futura tela de histórico de atendimentos
        System.out.println("→ Atendimentos (em desenvolvimento)");
    }

    @FXML
    private void abrirHistorico() {
        // Placeholder — pode ser ligado a uma futura tela de histórico completo
        System.out.println("→ Histórico (em desenvolvimento)");
    }

    @FXML
    private void terminarSessao() {
        LogService.logout();
        Sessao.get().terminar();
        App.mudarCenaLogin("Login.fxml");
    }

    // ── ACTUALIZAÇÃO DE UI ────────────────────────────────────

    private void mostrarSenhaAtual() {
        if (senhaActual == null) {
            mostrarSemSenhas();
            return;
        }
        String codigo = senhaActual.getCodigo();
        String letra  = codigo.substring(0, 1);
        String numero = codigo.length() > 1 ? codigo.substring(1) : "";

        lblLetraAtual.setText(letra);
        lblNumeroAtual.setText(numero);
        lblServicoAtual.setText(senhaActual.getNomeServico() != null
                ? senhaActual.getNomeServico() : "—");
    }

    private void mostrarSemSenhas() {
        lblLetraAtual.setText("—");
        lblNumeroAtual.setText("000");
        lblServicoAtual.setText("Sem atendimento");
        lblTempoAtendimento.setText("00:00:00");
    }

    /** Activa/desactiva botões conforme existe ou não senha em atendimento. */
    private void atualizarBotoes() {
        boolean emAtendimento = senhaActual != null;
        btnFinalizar.setDisable(!emAtendimento);
        btnAusente.setDisable(!emAtendimento);
        // "Chamar Próxima" fica sempre activo — chamar de novo durante um
        // atendimento substitui automaticamente a senha actual (comportamento
        // alinhado com FilaService, que não impede chamadas concorrentes).
    }

    /** Carrega da BD até 3 senhas em estado EM_ESPERA e popula boxProximas. */
    private void carregarProximas() {
        boxProximas.getChildren().clear();

        List<Senha> emEspera;
        try {
            emEspera = senhaDAO.listarHoje().stream()
                    .filter(s -> s.getEstado() == Estado.EM_ESPERA)
                    .limit(MAX_LISTA)
                    .toList();
        } catch (SQLException e) {
            System.err.println("⚠ Erro ao listar próximas senhas: " + e.getMessage());
            emEspera = List.of();
        }

        if (emEspera.isEmpty()) {
            Label vazio = new Label("Nenhuma senha em espera");
            vazio.getStyleClass().add("balc-empty-label");
            vazio.setMaxWidth(Double.MAX_VALUE);
            boxProximas.getChildren().add(vazio);
            return;
        }

        for (Senha s : emEspera) {
            boxProximas.getChildren().add(criarLinhaSenha(
                    s.getCodigo(), s.getNomeServico(), null));
        }
    }

    /** Renderiza até 3 últimas senhas concluídas/ausentes nesta sessão. */
    private void renderizarUltimas() {
        boxUltimas.getChildren().clear();

        if (ultimasSenhas.isEmpty()) {
            Label vazio = new Label("Ainda sem atendimentos hoje");
            vazio.getStyleClass().add("balc-empty-label");
            vazio.setMaxWidth(Double.MAX_VALUE);
            boxUltimas.getChildren().add(vazio);
            return;
        }

        for (HistoricoItem item : ultimasSenhas) {
            boxUltimas.getChildren().add(criarLinhaSenha(
                    item.codigo, item.servico, item.tempo));
        }
    }

    /** Cria uma linha visual (badge + número + serviço [+ tempo]) para as listas. */
    private HBox criarLinhaSenha(String codigo, String servico, String tempo) {
        String letra  = codigo.substring(0, 1);
        String numero = codigo.length() > 1 ? codigo.substring(1) : "";

        VBox badge = new VBox();
        badge.getStyleClass().add("balc-badge");
        badge.setAlignment(javafx.geometry.Pos.CENTER);
        Label lblLetra = new Label(letra);
        lblLetra.getStyleClass().add("balc-badge-letra");
        badge.getChildren().add(lblLetra);

        Label lblNumero = new Label(numero);
        lblNumero.getStyleClass().add("balc-row-numero");

        Label lblServico = new Label(servico != null ? servico : "—");
        lblServico.getStyleClass().add("balc-row-servico");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, badge, lblNumero, lblServico, spacer);
        row.getStyleClass().add("balc-list-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        if (tempo != null) {
            Label lblTempo = new Label(tempo);
            lblTempo.getStyleClass().add("balc-row-tempo");
            row.getChildren().add(lblTempo);
        }

        return row;
    }

    /** Adiciona um item ao topo do histórico de últimas senhas (máx. 3). */
    private void adicionarAoHistorico(Senha senha, String tempo) {
        ultimasSenhas.add(0, new HistoricoItem(senha.getCodigo(), senha.getNomeServico(), tempo));
        while (ultimasSenhas.size() > MAX_LISTA) {
            ultimasSenhas.remove(ultimasSenhas.size() - 1);
        }
    }

    /** Formata a duração do atendimento actual como HH:mm:ss. */
    private String formatarDuracao() {
        if (inicioAtendimento == null) return "00:00:00";
        long segundos = java.time.Duration.between(inicioAtendimento, LocalDateTime.now()).getSeconds();
        long h = segundos / 3600;
        long m = (segundos % 3600) / 60;
        long s = segundos % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Pequena estrutura interna para representar uma linha do histórico. */
    private record HistoricoItem(String codigo, String servico, String tempo) {}
}
