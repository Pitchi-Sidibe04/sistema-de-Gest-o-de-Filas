package app;

import app.model.Senha;
import app.model.ServicoInfo;
import app.service.FilaService;
import app.service.PdfService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Tela de confirmação de senha.
 *
 * Fluxo:
 *  1. Controller de serviço abre FileChooser → utilizador escolhe onde guardar.
 *  2. configurar(ServicoInfo) é chamado com o caminho já preenchido.
 *  3. initialize() emite a senha e gera o PDF no caminho escolhido.
 */
public class SenhaGeradaController {

    @FXML private Label lblCodigoSenha;
    @FXML private Label lblServico;
    @FXML private Label lblHora;
    @FXML private Label lblEmEspera;
    @FXML private Label lblPdfGuardado;

    private static ServicoInfo servicoInfo;
    private final  PdfService  pdfService = new PdfService();

    public static void configurar(ServicoInfo info) {
        servicoInfo = info;
    }

    @FXML
    public void initialize() {
        if (servicoInfo == null) {
            lblCodigoSenha.setText("ERRO");
            return;
        }
        emitirEGuardar();
    }

    private void emitirEGuardar() {
        try {
            Senha senha = FilaService.get().emitirSenha(servicoInfo);

            lblCodigoSenha.setText(senha.getCodigo());
            if (lblServico  != null) lblServico.setText(servicoInfo.getNome());
            if (lblHora     != null) lblHora.setText(
                senha.getDataEmissao().format(DateTimeFormatter.ofPattern("HH:mm")));

            int emEspera = Math.max(0, FilaService.get().tamanhoTotalFila() - 1);
            if (lblEmEspera != null)
                lblEmEspera.setText("Clientes à sua frente: " + emEspera);

            // Gera PDF no caminho escolhido pelo utilizador
            String caminho = servicoInfo.getCaminhoPdf();
            if (caminho != null) {
                boolean ok = pdfService.gerarPdf(senha, caminho);
                if (lblPdfGuardado != null)
                    lblPdfGuardado.setText(ok
                        ? "✓ PDF guardado em: " + caminho
                        : "⚠ Erro ao guardar PDF");
            } else {
                if (lblPdfGuardado != null) lblPdfGuardado.setText("");
            }

        } catch (SQLException e) {
            System.err.println("✗ Erro ao emitir senha: " + e.getMessage());
            lblCodigoSenha.setText(servicoInfo.getLetra() + "??");
            if (lblPdfGuardado != null)
                lblPdfGuardado.setText("⚠ Sem ligação à base de dados");
        }
    }

    @FXML
    private void voltarMenu() {
        App.mudarCena("Menu.fxml");
    }
}
