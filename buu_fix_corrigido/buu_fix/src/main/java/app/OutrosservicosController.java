package app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class OutrosservicosController {

    @FXML
    private Button btnVoltar;

    @FXML
    public void initialize() {
        System.out.println("Tela de Outros Serviços carregada");
    }

    @FXML
    private void voltarMenu() {
        App.mudarCena("Menu.fxml");
    }
}
