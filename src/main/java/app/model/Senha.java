package app.model;

import java.time.LocalDateTime;

public class Senha {

    public enum Estado { EM_ESPERA, CHAMADA, EM_ATENDIMENTO, CONCLUIDA, AUSENTE, CANCELADA }

    private int           idSenha;
    private String        codigo;
    private int           idServico;
    private String        nomeServico;
    private boolean       prioritario;
    private Estado        estado;
    private int           numeroChamadas;
    private LocalDateTime dataEmissao;

    public Senha() {}

    public Senha(String codigo, int idServico, String nomeServico, boolean prioritario) {
        this.codigo        = codigo;
        this.idServico     = idServico;
        this.nomeServico   = nomeServico;
        this.prioritario   = prioritario;
        this.estado        = Estado.EM_ESPERA;
        this.numeroChamadas = 0;
        this.dataEmissao   = LocalDateTime.now();
    }

    public int           getIdSenha()        { return idSenha; }
    public void          setIdSenha(int v)   { this.idSenha = v; }
    public String        getCodigo()         { return codigo; }
    public void          setCodigo(String v) { this.codigo = v; }
    public int           getIdServico()      { return idServico; }
    public void          setIdServico(int v) { this.idServico = v; }
    public String        getNomeServico()    { return nomeServico; }
    public void          setNomeServico(String v) { this.nomeServico = v; }
    public boolean       isPrioritario()     { return prioritario; }
    public void          setPrioritario(boolean v){ this.prioritario = v; }
    public Estado        getEstado()         { return estado; }
    public void          setEstado(Estado v) { this.estado = v; }
    public int           getNumeroChamadas() { return numeroChamadas; }
    public void          setNumeroChamadas(int v){ this.numeroChamadas = v; }
    public LocalDateTime getDataEmissao()    { return dataEmissao; }
    public void          setDataEmissao(LocalDateTime v){ this.dataEmissao = v; }

    @Override
    public String toString() { return codigo + " [" + estado + "]" + (prioritario ? " ★" : ""); }

    public String getEstadoFormatado() {
        if (estado == null) return "—";
        return switch (estado) {
            case EM_ESPERA      -> "⏳ Em Espera";
            case CHAMADA        -> "📢 Chamada";
            case EM_ATENDIMENTO -> "✅ Em Atendimento";
            case CONCLUIDA      -> "✔ Concluída";
            case AUSENTE        -> "✗ Ausente";
            case CANCELADA      -> "✗ Cancelada";
        };
    }

}