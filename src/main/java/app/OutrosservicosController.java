package app;

import app.model.ServicoInfo;
import app.util.EscolherPdf;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Window;

public class OutrosservicosController {

    private static final String  LETRA = "E";
    private static final boolean PRIO  = false;

    @FXML private Button creditos;

    private void ir(String nomeSubservico) {
        Window w = creditos != null ? creditos.getScene().getWindow() : null;
        String caminho = EscolherPdf.abrir(LETRA + "_senha", w);
        if (caminho == null) return;

        SenhaGeradaController.configurar(
            new ServicoInfo(nomeSubservico, LETRA, PRIO, -1, caminho));
        App.mudarCena("SenhaGerada.fxml");
    }

    @FXML private void creditos()       { ir("Créditos"); }
    @FXML private void transferencias() { ir("Transferências"); }
    @FXML private void seguros()        { ir("Seguros"); }
    @FXML private void saldosExtratos() { ir("Saldos / Extratos"); }
    @FXML private void aplicacoes()     { ir("Aplicações"); }
    @FXML private void voltarMenu()     { App.mudarCena("Menu.fxml"); }
}
