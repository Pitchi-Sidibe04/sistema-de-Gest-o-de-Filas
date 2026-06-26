package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    // Janela 1 — Quiosque do Cliente (Menu)
    private static Stage stage;
    private static Scene scene;

    // Janela 2 — Acesso (Login) → Balconista / Gerente
    private static Stage loginStage;
    private static Scene loginScene;

    // Janela 3 — Ecrã de Chamada (Sala de Espera)
    private static Stage ecraStage;
    private static Scene ecraScene;

    @Override
    public void start(Stage primaryStage) {
        try {
            // ── Janela 1: Quiosque do Cliente ──────────────────────────────
            stage = primaryStage;
            Parent menuRoot = FXMLLoader.load(App.class.getResource("/app/Menu.fxml"));
            scene = new Scene(menuRoot, 520, 580);
            stage.setScene(scene);
            stage.setTitle("Banco Ubuntu — Quiosque do Cliente");
            stage.setResizable(true);
            stage.show();

            // ── Janela 2: Acesso (Login) — Balconista / Gerente ────────────
            Parent loginRoot = FXMLLoader.load(App.class.getResource("/app/Login.fxml"));
            loginScene = new Scene(loginRoot, 920, 620);
            loginStage = new Stage();
            loginStage.setScene(loginScene);
            loginStage.setTitle("Banco Ubuntu — Acesso (Balconista / Gerente)");
            loginStage.setResizable(true);
            loginStage.setX(stage.getX() + stage.getWidth() + 20);
            loginStage.setY(stage.getY());
            loginStage.show();

            // ── Janela 3: Ecrã de Chamada (Sala de Espera) ─────────────────
            // Carrega o FXML antecipadamente, mas adia o show() para depois
            // do JavaFX ter aplicado o layout e as coordenadas do stage estarem
            // disponíveis (stage.getX/Y/Height retornam 0 antes do primeiro pulse).
            Parent ecraRoot = FXMLLoader.load(App.class.getResource("/app/EcraChamada.fxml"));
            ecraScene = new Scene(ecraRoot, 960, 520);
            ecraStage = new Stage();
            ecraStage.setScene(ecraScene);
            ecraStage.setTitle("Banco Ubuntu — Sala de Espera");
            ecraStage.setResizable(true);

            // Platform.runLater garante que as dimensões reais do stage
            // já estão calculadas quando posicionamos e mostramos o ecraStage.
            Platform.runLater(() -> {
                ecraStage.setX(stage.getX());
                ecraStage.setY(stage.getY() + stage.getHeight() + 20);
                ecraStage.show();
                System.out.println("✓ Sala de Espera aberta em ("
                        + ecraStage.getX() + ", " + ecraStage.getY() + ")");
            });

            System.out.println("✓ Aplicação iniciada: Quiosque + Login + Sala de Espera");

        } catch (Exception e) {
            System.err.println("✗ Erro ao iniciar aplicação:");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            DatabaseConnection.fecharConexao();
        } catch (Exception e) {
            System.err.println("Erro ao fechar conexão:");
            e.printStackTrace();
        }
    }

    // ── Quiosque (Janela 1) ─────────────────────────────────────────────────
    public static void mudarCena(String fxml) {
        if (scene == null) { System.err.println("✗ Scene (quiosque) não inicializada"); return; }
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/app/" + fxml));
            scene.setRoot(root);
            System.out.println("✓ [Quiosque] Cena carregada: " + fxml);
        } catch (Exception e) {
            System.err.println("✗ [Quiosque] Erro ao carregar: " + fxml);
            e.printStackTrace();
        }
    }

    // ── Acesso (Janela 2) ───────────────────────────────────────────────────
    public static void mudarCenaLogin(String fxml) {
        if (loginScene == null) { System.err.println("✗ Scene (login) não inicializada"); return; }
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/app/" + fxml));
            loginScene.setRoot(root);
            System.out.println("✓ [Acesso] Cena carregada: " + fxml);
        } catch (Exception e) {
            System.err.println("✗ [Acesso] Erro ao carregar: " + fxml);
            e.printStackTrace();
        }
    }

    // ── Ecrã de Chamada (Janela 3) ──────────────────────────────────────────
    public static void mudarCenaEcra(String fxml) {
        if (ecraScene == null) { System.err.println("✗ Scene (ecrã) não inicializada"); return; }
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/app/" + fxml));
            ecraScene.setRoot(root);
            System.out.println("✓ [Ecrã] Cena carregada: " + fxml);
        } catch (Exception e) {
            System.err.println("✗ [Ecrã] Erro ao carregar: " + fxml);
            e.printStackTrace();
        }
    }

    public static Stage getEcraStage() { return ecraStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
