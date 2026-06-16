package app.service;

import app.DatabaseConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Gera o relatório diário em Excel utilizando Apache POI.
 * Inclui: totais, ausentes, serviços mais procurados,
 * tempo médio de espera e desempenho por balcão.
 */
public class ExcelService {

    public boolean gerarRelatorio(String caminho) {
        Connection conn = DatabaseConnection.getConexao();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── Estilos ──────────────────────────────────────────
            CellStyle estTitulo = wb.createCellStyle();
            Font fTitulo = wb.createFont();
            fTitulo.setBold(true); fTitulo.setFontHeightInPoints((short) 14);
            estTitulo.setFont(fTitulo);
            estTitulo.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            estTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estTitulo.setAlignment(HorizontalAlignment.CENTER);
            Font fTituloC = wb.createFont();
            fTituloC.setBold(true); fTituloC.setFontHeightInPoints((short) 14);
            fTituloC.setColor(IndexedColors.WHITE.getIndex());
            estTitulo.setFont(fTituloC);

            CellStyle estCabec = wb.createCellStyle();
            Font fCabec = wb.createFont(); fCabec.setBold(true);
            estCabec.setFont(fCabec);
            estCabec.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estCabec.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estCabec.setBorderBottom(BorderStyle.THIN);

            CellStyle estNum = wb.createCellStyle();
            estNum.setAlignment(HorizontalAlignment.CENTER);

            String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // ── Aba 1: Resumo ────────────────────────────────────
            Sheet resumo = wb.createSheet("Resumo Diário");
            int r = 0;

            Row rowTit = resumo.createRow(r++);
            Cell cTit = rowTit.createCell(0);
            cTit.setCellValue("BANCO UBUNTUU — Relatório de " + hoje);
            cTit.setCellStyle(estTitulo);
            resumo.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            r++; // linha vazia
            String[][] totais = {
                {"Total de senhas emitidas",     contarSQL(conn, "SELECT COUNT(*) FROM senha WHERE DATE(data_emissao)=CURDATE()")},
                {"Total atendidas (CONCLUIDA)",  contarSQL(conn, "SELECT COUNT(*) FROM senha WHERE estado='CONCLUIDA' AND DATE(data_emissao)=CURDATE()")},
                {"Total ausentes",               contarSQL(conn, "SELECT COUNT(*) FROM senha WHERE estado='AUSENTE' AND DATE(data_emissao)=CURDATE()")},
                {"Ainda em espera",              contarSQL(conn, "SELECT COUNT(*) FROM senha WHERE estado='EM_ESPERA' AND DATE(data_emissao)=CURDATE()")},
                {"Tempo médio de espera (min)",  tempoMedioEspera(conn)},
            };

            Row hRow = resumo.createRow(r++);
            celula(hRow, 0, "Indicador", estCabec);
            celula(hRow, 1, "Valor", estCabec);

            for (String[] linha : totais) {
                Row row = resumo.createRow(r++);
                celula(row, 0, linha[0], null);
                celula(row, 1, linha[1], estNum);
            }

            resumo.autoSizeColumn(0);
            resumo.autoSizeColumn(1);

            // ── Aba 2: Serviços mais procurados ─────────────────
            Sheet servSheet = wb.createSheet("Por Serviço");
            int sr = 0;
            Row sTit = servSheet.createRow(sr++);
            Cell sTitC = sTit.createCell(0);
            sTitC.setCellValue("Serviços mais procurados — " + hoje);
            sTitC.setCellStyle(estTitulo);
            servSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
            sr++;

            Row sH = servSheet.createRow(sr++);
            celula(sH, 0, "Serviço", estCabec);
            celula(sH, 1, "Total senhas", estCabec);
            celula(sH, 2, "Concluídas", estCabec);

            if (conn != null) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                         "SELECT sv.nome, COUNT(*) AS total, " +
                         "SUM(CASE WHEN s.estado='CONCLUIDA' THEN 1 ELSE 0 END) AS concluidas " +
                         "FROM senha s JOIN servico sv ON s.id_servico=sv.id_servico " +
                         "WHERE DATE(s.data_emissao)=CURDATE() " +
                         "GROUP BY sv.nome ORDER BY total DESC")) {
                    while (rs.next()) {
                        Row row = servSheet.createRow(sr++);
                        celula(row, 0, rs.getString("nome"), null);
                        celula(row, 1, rs.getString("total"), estNum);
                        celula(row, 2, rs.getString("concluidas"), estNum);
                    }
                } catch (SQLException e) {
                    System.err.println("⚠ Serviços: " + e.getMessage());
                }
            }
            servSheet.autoSizeColumn(0);
            servSheet.autoSizeColumn(1);
            servSheet.autoSizeColumn(2);

            // ── Aba 3: Desempenho por balcão ─────────────────────
            Sheet balcSheet = wb.createSheet("Por Balcão");
            int br = 0;
            Row bTit = balcSheet.createRow(br++);
            Cell bTitC = bTit.createCell(0);
            bTitC.setCellValue("Desempenho por balcão — " + hoje);
            bTitC.setCellStyle(estTitulo);
            balcSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            br++;

            Row bH = balcSheet.createRow(br++);
            celula(bH, 0, "Balcão", estCabec);
            celula(bH, 1, "Atendente", estCabec);
            celula(bH, 2, "Atendimentos", estCabec);
            celula(bH, 3, "Tempo médio (min)", estCabec);

            if (conn != null) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                         "SELECT b.numero_balcao, u.nome, COUNT(*) AS total, " +
                         "ROUND(AVG(TIMESTAMPDIFF(MINUTE, a.hora_chamada, a.hora_fim)),1) AS tempo_med " +
                         "FROM atendimento a " +
                         "JOIN balcao b ON a.id_balcao=b.id_balcao " +
                         "JOIN utilizador u ON a.id_utilizador=u.id_utilizador " +
                         "WHERE DATE(a.hora_inicio)=CURDATE() " +
                         "GROUP BY b.numero_balcao, u.nome ORDER BY b.numero_balcao")) {
                    while (rs.next()) {
                        Row row = balcSheet.createRow(br++);
                        celula(row, 0, "Balcão " + rs.getString("numero_balcao"), null);
                        celula(row, 1, rs.getString("nome"), null);
                        celula(row, 2, rs.getString("total"), estNum);
                        celula(row, 3, rs.getString("tempo_med"), estNum);
                    }
                } catch (SQLException e) {
                    System.err.println("⚠ Balcões: " + e.getMessage());
                }
            }
            balcSheet.autoSizeColumn(0); balcSheet.autoSizeColumn(1);
            balcSheet.autoSizeColumn(2); balcSheet.autoSizeColumn(3);

            // ── Gravar ───────────────────────────────────────────
            try (FileOutputStream fos = new FileOutputStream(caminho)) {
                wb.write(fos);
            }

            LogService.gerarExcel();
            return true;

        } catch (IOException e) {
            System.err.println("✗ Erro ao gerar Excel: " + e.getMessage());
            return false;
        }
    }

    // ── helpers ──────────────────────────────────────────────────

    private String contarSQL(Connection conn, String sql) {
        if (conn == null) return "N/D";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "0";
        } catch (SQLException e) { return "N/D"; }
    }

    private String tempoMedioEspera(Connection conn) {
        if (conn == null) return "N/D";
        String sql = "SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, s.data_emissao, a.hora_chamada)), 1) " +
                     "FROM atendimento a JOIN senha s ON a.id_senha=s.id_senha " +
                     "WHERE DATE(s.data_emissao)=CURDATE()";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            String v = rs.next() ? rs.getString(1) : null;
            return v != null ? v : "0";
        } catch (SQLException e) { return "N/D"; }
    }

    private void celula(Row row, int col, String valor, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(valor != null ? valor : "");
        if (style != null) c.setCellStyle(style);
    }
}
