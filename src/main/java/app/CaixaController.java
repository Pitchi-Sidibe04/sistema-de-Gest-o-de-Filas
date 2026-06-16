package app;

import app.model.ServicoInfo;
import app.util.EscolherPdf;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Window;

public class CaixaController {

    private static final String  LETRA = "A";
    private static final boolean PRIO  = false;

    @FXML private Button deposito;
    @FXML private Button levantamento;

    /** Abre FileChooser → guarda caminho → navega para SenhaGerada. */
    private void ir(String nomeSubservico) {
        Window w = deposito != null ? deposito.getScene().getWindow() : null;
        String caminho = EscolherPdf.abrir(LETRA + "_senha", w);
        if (caminho == null) return;   // utilizador cancelou — não navega

        SenhaGeradaController.configurar(
            new ServicoInfo(nomeSubservico, LETRA, PRIO, -1, caminho));
        App.mudarCena("SenhaGerada.fxml");
    }

    @FXML private void deposito()     { ir("Depósito"); }
    @FXML private void levantamento() { ir("Levantamento"); }
    @FXML private void voltarMenu()   { App.mudarCena("Menu.fxml"); }
}
