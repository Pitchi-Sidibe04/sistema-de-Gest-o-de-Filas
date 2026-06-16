package app;

import javafx.fxml.FXML;

public class MenuController {

    @FXML
    public void initialize() {
        System.out.println("Menu carregado");
    }

    @FXML
    private void caixa() {
        App.mudarCena("Caixa.fxml");
    }

    @FXML
    private void conta() {
        App.mudarCena("Conta.fxml");
    }

    @FXML
    private void cartoes() {
        App.mudarCena("Cartoes.fxml");
    }

    @FXML
    private void prioritario() {
        App.mudarCena("Prioritario.fxml");
    }

    @FXML
    private void outros() {
        App.mudarCena("Outrosservicos.fxml");
    }
}
