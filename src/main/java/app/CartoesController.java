package app;

import app.model.ServicoInfo;
import app.util.EscolherPdf;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Window;

public class CartoesController {

    private static final String  LETRA = "C";
    private static final boolean PRIO  = false;

    @FXML private Button multicaixa;

    private void ir(String nomeSubservico) {
        Window w = multicaixa != null ? multicaixa.getScene().getWindow() : null;
        String caminho = EscolherPdf.abrir(LETRA + "_senha", w);
        if (caminho == null) return;

        SenhaGeradaController.configurar(
            new ServicoInfo(nomeSubservico, LETRA, PRIO, -1, caminho));
        App.mudarCena("SenhaGerada.fxml");
    }

    @FXML private void multicaixa()  { ir("Cartão Multicaixa"); }
    @FXML private void visa()        { ir("Cartão VISA"); }
    @FXML private void mastercard()  { ir("Cartão Mastercard"); }
    @FXML private void voltarMenu()  { App.mudarCena("Menu.fxml"); }
}
