package app;

import app.model.ServicoInfo;
import app.util.EscolherPdf;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Window;

public class PrioritarioController {

    @FXML private Button btnGerar;

    @FXML
    private void gerarSenha() {
        Window w = btnGerar != null ? btnGerar.getScene().getWindow() : null;
        String caminho = EscolherPdf.abrir("D_senha", w);
        if (caminho == null) return;

        SenhaGeradaController.configurar(
            new ServicoInfo("Atendimento Prioritário", "D", true, -1, caminho));
        App.mudarCena("SenhaGerada.fxml");
    }

    @FXML private void voltarMenu() { App.mudarCena("Menu.fxml"); }
}
