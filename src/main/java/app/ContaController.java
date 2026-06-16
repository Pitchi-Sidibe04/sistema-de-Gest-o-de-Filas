package app;

import app.model.ServicoInfo;
import app.util.EscolherPdf;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Window;

public class ContaController {

    private static final String  LETRA = "B";
    private static final boolean PRIO  = false;

    @FXML private Button abertura;

    private void ir(String nomeSubservico) {
        Window w = abertura != null ? abertura.getScene().getWindow() : null;
        String caminho = EscolherPdf.abrir(LETRA + "_senha", w);
        if (caminho == null) return;

        SenhaGeradaController.configurar(
            new ServicoInfo(nomeSubservico, LETRA, PRIO, -1, caminho));
        App.mudarCena("SenhaGerada.fxml");
    }

    @FXML private void abertura()    { ir("Abertura de Conta"); }
    @FXML private void atualizacao() { ir("Atualização de Dados"); }
    @FXML private void voltarMenu()  { App.mudarCena("Menu.fxml"); }
}
