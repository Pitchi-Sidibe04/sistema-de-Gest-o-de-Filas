package app.service;

import app.DatabaseConnection;
import app.dao.AtendimentoDAO;
import app.dao.LogDAO;
import app.dao.SenhaDAO;
import app.model.LogAtividade.Acao;
import app.model.LogAtividade;
import app.model.Senha;
import app.model.Senha.Estado;
import app.model.ServicoInfo;
import app.Sessao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Gere a fila FIFO com prioridade.
 *
 * Regra:  senhas prioritárias (D) → chamadas antes das normais.
 *         dentro do mesmo grupo   → FIFO por data_emissao.
 *
 * Mantém as filas em memória (sessão actual) sincronizadas com a BD.
 * Se as filas em memória estiverem vazias consulta a BD como fallback.
 */
public class FilaService {

    private static FilaService instancia;

    private final Queue<Senha> filaPrioritaria = new LinkedList<>();
    private final Queue<Senha> filaNormal      = new LinkedList<>();
    private Senha              senhaEmAtendimento;

    private final SenhaDAO senhaDAO = new SenhaDAO();
    private final LogDAO   logDAO   = new LogDAO();
    private final AtendimentoDAO atendimentoDAO = new AtendimentoDAO();

    private FilaService() {}

    public static FilaService get() {
        if (instancia == null) instancia = new FilaService();
        return instancia;
    }

    // ── Emissão ──────────────────────────────────────────────────

    /**
     * Emite uma senha para o serviço descrito em ServicoInfo.
     * Resolve o idServico automaticamente se não estiver preenchido.
     */
    public Senha emitirSenha(ServicoInfo info) throws SQLException {
        int idSvc = info.getIdServico() > 0
            ? info.getIdServico()
            : resolverIdServico(info.getLetra());

        Senha nova = senhaDAO.inserir(
            idSvc, info.getLetra(), info.isPrioritario(), info.getNome());

        if (info.isPrioritario()) filaPrioritaria.offer(nova);
        else                      filaNormal.offer(nova);

        log(Acao.GERAR_SENHA,
            "Senha: " + nova.getCodigo() + " | Serviço: " + info.getNome(), null);
        return nova;
    }

    /**
     * Versão por parâmetros individuais — mantida para compatibilidade
     * com PainelGerenteController e SenhaGeradaController antigos.
     */
    public Senha emitirSenha(int idServico, String letra,
                             boolean prioritario, String nomeServico) throws SQLException {
        return emitirSenha(new ServicoInfo(nomeServico, letra, prioritario, idServico));
    }

    // ── Chamadas ─────────────────────────────────────────────────

    /**
     * Chama a próxima senha respeitando prioridade → FIFO.
     * Faz fallback à BD se as filas em memória estiverem vazias.
     */
    public Senha chamarProxima(Integer idBalcao) throws SQLException {
        Senha proxima = filaPrioritaria.poll();
        if (proxima == null) proxima = filaNormal.poll();
        if (proxima == null) proxima = senhaDAO.proximaDaFila(); // fallback BD

        if (proxima == null) return null;

        senhaDAO.actualizarEstado(proxima.getIdSenha(), Estado.CHAMADA);
        senhaDAO.incrementarChamadas(proxima.getIdSenha());
        proxima.setEstado(Estado.CHAMADA);
        senhaEmAtendimento = proxima;

        // Regista o atendimento na BD para o Ecrã de Chamada conseguir
        // mostrar qual senha cada balcão está a atender.
        if (idBalcao != null) {
            try {
                int idUtilizador = Sessao.get().getIdUtilizador();
                atendimentoDAO.registarChamada(proxima.getIdSenha(), idBalcao, idUtilizador);
            } catch (SQLException e) {
                System.err.println("⚠ registarChamada: " + e.getMessage());
            }
        }

        log(Acao.CHAMAR_SENHA, "Chamada: " + proxima.getCodigo(), idBalcao);
        return proxima;
    }

    /**
     * Marca o cliente como ausente e chama automaticamente a próxima senha.
     */
    public Senha clienteAusente(Senha senha, Integer idBalcao) throws SQLException {
        if (senha == null) return null;
        senhaDAO.actualizarEstado(senha.getIdSenha(), Estado.AUSENTE);
        senha.setEstado(Estado.AUSENTE);
        finalizarAtendimentoAberto(senha.getIdSenha());
        log(Acao.CLIENTE_AUSENTE, "Ausente: " + senha.getCodigo(), idBalcao);
        return chamarProxima(idBalcao);
    }

    /** Conclui o atendimento actual. */
    public void concluir(Senha senha) throws SQLException {
        if (senha == null) return;
        senhaDAO.actualizarEstado(senha.getIdSenha(), Estado.CONCLUIDA);
        senha.setEstado(Estado.CONCLUIDA);
        finalizarAtendimentoAberto(senha.getIdSenha());
        if (senhaEmAtendimento != null
                && senhaEmAtendimento.getIdSenha() == senha.getIdSenha()) {
            senhaEmAtendimento = null;
        }
    }

    // ── Consultas ────────────────────────────────────────────────

    public Senha getSenhaEmAtendimento()  { return senhaEmAtendimento; }
    public int   tamanhoFilaPrioritaria() { return filaPrioritaria.size(); }
    public int   tamanhoFilaNormal()      { return filaNormal.size(); }
    public int   tamanhoTotalFila()       { return filaPrioritaria.size() + filaNormal.size(); }

    /** Reinicia as filas em memória (novo dia / nova sessão). */
    public void limpar() {
        filaPrioritaria.clear();
        filaNormal.clear();
        senhaEmAtendimento = null;
    }

    // ── Helpers privados ─────────────────────────────────────────

    /** Busca o id_servico pela letra sem SQL inline no controller. */
    private int resolverIdServico(String letra) {
        if (letra == null) return 1;
        try {
            Connection conn = DatabaseConnection.getConexao();
            if (conn == null) return 1;
            try (PreparedStatement ps =
                     conn.prepareStatement("SELECT id_servico FROM servico WHERE letra = ?")) {
                ps.setString(1, letra);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("⚠ resolverIdServico(" + letra + "): " + e.getMessage());
        }
        return 1;
    }

    /**
     * Marca hora_fim no registo de atendimento em aberto (hora_fim IS NULL)
     * para a senha indicada — usado por concluir() e clienteAusente() para
     * que a senha deixe de aparecer no Ecrã de Chamada como "em atendimento".
     */
    private void finalizarAtendimentoAberto(int idSenha) {
        try {
            int idAtendimento = atendimentoDAO.idAtendimentoAberto(idSenha);
            if (idAtendimento != -1) {
                atendimentoDAO.finalizar(idAtendimento);
            }
        } catch (SQLException e) {
            System.err.println("⚠ finalizarAtendimentoAberto: " + e.getMessage());
        }
    }

    private void log(Acao acao, String descricao, Integer balcao) {
        try {
            logDAO.registar(new LogAtividade(
                Sessao.get().getIdUtilizador(),
                Sessao.get().getNome() != null ? Sessao.get().getNome() : "Quiosque",
                acao, descricao, balcao));
        } catch (Exception e) {
            System.err.println("⚠ Log: " + e.getMessage());
        }
    }

    /**
     * Avança a fila em memória sem aceder à BD.
     * Usado como fallback quando a BD está indisponível no clienteAusente.
     */
    public Senha chamarProximaEmMemoria() {
        Senha proxima = filaPrioritaria.poll();
        if (proxima == null) proxima = filaNormal.poll();
        return proxima;
    }

}