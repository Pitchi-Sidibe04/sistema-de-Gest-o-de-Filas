package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage stage;
    private static Scene scene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        try {
            Parent root = FXMLLoader.load(App.class.getResource("/app/Login.fxml"));
            scene = new Scene(root, 640, 661);
            
            stage.setScene(scene);
            stage.setTitle("Banco Ubuntu");
            stage.setWidth(640);
            stage.setHeight(661);
            stage.show();
            
            System.out.println("✓ Aplicação iniciada com sucesso");
        } catch (Exception e) {
            System.err.println("✗ Erro ao carregar Login.fxml:");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        DatabaseConnection.fecharConexao();
    }

    public static void mudarCena(String fxml) {
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/app/" + fxml));
            scene.setRoot(root);
            System.out.println("✓ Cena carregada: " + fxml);
        } catch (Exception e) {
            System.err.println("✗ Erro ao carregar " + fxml + ":");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}