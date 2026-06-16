package app.service;

import app.Sessao;
import app.dao.LogDAO;
import app.model.LogAtividade;
import app.model.LogAtividade.Acao;

/**
 * Facade para registo de logs — centraliza todos os registos
 * e garante que nunca lançam excepção para o chamador.
 */
public class LogService {

    private static final LogDAO dao = new LogDAO();

    public static void login() {
        registar(Acao.LOGIN, "Login efectuado", null);
    }

    public static void logout() {
        registar(Acao.LOGOUT, "Logout efectuado", null);
    }

    public static void gerarSenha(String codigo, String servico) {
        registar(Acao.GERAR_SENHA, "Senha: " + codigo + " | Serviço: " + servico, null);
    }

    public static void chamarSenha(String codigo, Integer balcao) {
        registar(Acao.CHAMAR_SENHA, "Chamada: " + codigo, balcao);
    }

    public static void clienteAusente(String codigo, Integer balcao) {
        registar(Acao.CLIENTE_AUSENTE, "Ausente: " + codigo, balcao);
    }

    public static void gerarPdf(String codigo) {
        registar(Acao.GERAR_PDF, "PDF gerado para: " + codigo, null);
    }

    public static void gerarExcel() {
        registar(Acao.GERAR_EXCEL, "Relatório Excel gerado", null);
    }

    private static void registar(Acao acao, String descricao, Integer balcao) {
        try {
            Sessao s = Sessao.get();
            dao.registar(new LogAtividade(
                s.getIdUtilizador(),
                s.getNome() != null ? s.getNome() : "Sistema",
                acao, descricao, balcao
            ));
        } catch (Exception e) {
            System.err.println("⚠ Log silencioso: " + e.getMessage());
        }
    }
}
