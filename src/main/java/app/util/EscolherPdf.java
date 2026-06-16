package app.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Abre o diálogo "Guardar como PDF" antes de navegar para a tela de senha.
 */
public class EscolherPdf {

    private static File ultimaPasta = null;

    /**
     * @param sugestaoNome  nome sugerido sem extensão, ex: "A_20260606"
     * @param window        janela pai (pode ser null)
     * @return caminho absoluto escolhido, ou null se cancelou
     */
    public static String abrir(String sugestaoNome, Window window) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar senha em PDF");
        fc.setInitialFileName(sugestaoNome + ".pdf");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Ficheiro PDF (*.pdf)", "*.pdf"));

        if (ultimaPasta != null && ultimaPasta.exists()) {
            fc.setInitialDirectory(ultimaPasta);
        } else {
            File docs = Paths.get(System.getProperty("user.home"), "Documents").toFile();
            if (docs.exists()) fc.setInitialDirectory(docs);
        }

        File escolhido = fc.showSaveDialog(window);
        if (escolhido != null) {
            ultimaPasta = escolhido.getParentFile();
            return escolhido.getAbsolutePath();
        }
        return null;
    }
}
