package app.service;

import app.dao.SenhaDAO;
import app.model.Senha;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Gera o comprovativo PDF da senha usando Apache PDFBox 3.x.
 *
 * Dois modos de utilização:
 *  1. gerarEGuardarAutomaticamente(senha) — guarda sem pedir ao utilizador.
 *     Pasta: {user.home}/Documents/BancoUbuntuu/Senhas/{YYYY-MM-DD}/
 *  2. gerarPdf(senha, caminho)            — guarda no caminho explícito.
 */
public class PdfService {

    // Pasta base onde os PDFs são guardados automaticamente
    private static final Path PASTA_BASE = Paths.get(
        System.getProperty("user.home"), "Documents", "BancoUbuntuu", "Senhas");

    private static final PDType1Font BOLD    =
        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font REGULAR =
        new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ── API pública ───────────────────────────────────────────────

    /**
     * Guarda o PDF automaticamente assim que a senha é emitida.
     * Não exige interacção do utilizador.
     *
     * Caminho resultante:
     *   ~/Documents/BancoUbuntuu/Senhas/2026-06-06/A20260606001.pdf
     *
     * @return O ficheiro criado, ou null se falhou.
     */
    public File gerarEGuardarAutomaticamente(Senha senha) {
        try {
            // Cria subpasta do dia se não existir
            Path pastaDia = PASTA_BASE.resolve(
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            Files.createDirectories(pastaDia);

            String nomeFicheiro = senha.getCodigo() + ".pdf";
            Path destino = pastaDia.resolve(nomeFicheiro);

            boolean ok = gerarPdf(senha, destino.toString());
            if (ok) {
                System.out.println("✓ PDF guardado: " + destino);
                LogService.gerarPdf(senha.getCodigo());
                return destino.toFile();
            }
        } catch (IOException e) {
            System.err.println("✗ Erro ao criar pasta de PDFs: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gera o PDF para o caminho indicado (usado para "Guardar como…").
     * @return true se bem-sucedido.
     */
    public boolean gerarPdf(Senha senha, String caminho) {
        try (PDDocument doc = new PDDocument()) {
            // Ticket: 80 mm × 150 mm em pontos (1 mm = 2.8346 pt)
            PDRectangle pageSize = new PDRectangle(226.77f, 425.20f);
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            int emEspera = 0;
            try { emEspera = new SenhaDAO().contarEmEspera(); }
            catch (Exception ignored) {}

            String data = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float w = pageSize.getWidth();
                float y = pageSize.getHeight() - 20;

                // Banco (centrado)
                escreverCentrado(cs, BOLD,    13, w, y, "BANCO UBUNTUU");       y -= 16;
                escreverCentrado(cs, REGULAR,  8, w, y, "COMUNIDADE E SABEDORIA"); y -= 20;
                linha(cs, 10, w - 10, y);                                y -= 12;

                // Serviço (centrado)
                escreverCentrado(cs, REGULAR,  9, w, y, "SERVIÇO");             y -= 16;
                escreverCentrado(cs, BOLD,    10, w, y,
                    senha.getNomeServico() != null ? senha.getNomeServico() : ""); y -= 24;

                // Código (centrado, destaque) — ligeiramente mais abaixo
                y -= 10;
                escreverCentrado(cs, BOLD,    36, w, y, senha.getCodigo());      y -= 44;

                // Data / Hora (centrado)
                escreverCentrado(cs, REGULAR,  9, w, y,
                    "Data: " + data + "   Hora: " + hora);               y -= 14;

                // Fila (centrado)
                escreverCentrado(cs, REGULAR,  9, w, y,
                    "Clientes à sua frente: " + emEspera);               y -= 18;
                linha(cs, 10, w - 10, y);                                y -= 15;

                // QR Code
                BufferedImage qr = gerarQrCode(senha.getCodigo(), 100);
                if (qr != null) {
                    PDImageXObject xObj = PDImageXObject.createFromByteArray(
                        doc, toBytes(qr), "qr");
                    float sz = 80f;
                    cs.drawImage(xObj, (w - sz) / 2, y - sz, sz, sz);
                    y -= sz + 10;
                }

                // Rodapé (centrado)
                escreverCentrado(cs, REGULAR, 7, w, y, "Guarde este comprovativo"); y -= 10;
                escreverCentrado(cs, REGULAR, 7, w, y, "Obrigado pela preferência");
            }

            doc.save(caminho);
            return true;

        } catch (IOException e) {
            System.err.println("✗ Erro ao gerar PDF: " + e.getMessage());
            return false;
        }
    }

    /** Retorna a pasta base onde os PDFs são guardados. */
    public static Path getPastaBase() { return PASTA_BASE; }

    // ── Helpers privados ─────────────────────────────────────────

    /**
     * Escreve texto centrado horizontalmente na largura w da página.
     * Calcula a largura real do texto na fonte/tamanho indicados para
     * determinar o ponto de partida — em vez de uma margem fixa de 10pt,
     * como na versão anterior (que deixava todo o ticket alinhado à
     * esquerda em vez de centrado).
     */
    private void escreverCentrado(PDPageContentStream cs, PDType1Font font,
                          int size, float w, float y, String texto) throws IOException {
        String t = texto != null ? texto : "";
        float largura = font.getStringWidth(t) / 1000f * size;
        float x = (w - largura) / 2f;
        if (x < 4f) x = 4f; // margem mínima de segurança

        cs.setFont(font, size);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private void linha(PDPageContentStream cs, float x1, float x2, float y) throws IOException {
        cs.moveTo(x1, y); cs.lineTo(x2, y); cs.stroke();
    }

    private BufferedImage gerarQrCode(String conteudo, int tam) {
        try {
            BitMatrix m = new QRCodeWriter()
                .encode(conteudo, BarcodeFormat.QR_CODE, tam, tam);
            return MatrixToImageWriter.toBufferedImage(m);
        } catch (WriterException e) {
            System.err.println("⚠ QR Code: " + e.getMessage());
            return null;
        }
    }

    private byte[] toBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", bos);
        return bos.toByteArray();
    }
}