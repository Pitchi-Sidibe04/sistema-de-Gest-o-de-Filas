package app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class PrioritarioController {

    @FXML
    private Button btnVoltar;

    @FXML
    public void initialize() {
        System.out.println("Tela de Atendimento Prioritário carregada");
    }

    @FXML
    private void voltarMenu() {
        App.mudarCena("Menu.fxml");
    }
}
