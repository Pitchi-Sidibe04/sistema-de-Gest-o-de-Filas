package app.model;

import java.sql.Timestamp;

/**
 * Representa uma senha associada a um balcão (para o Ecrã de Chamada) ou
 * uma senha em espera (lista "Próximas Senhas").
 *
 * Quando numeroBalcao == 0, representa uma senha em espera (sem balcão
 * associado ainda).
 */
public class AtendimentoBalcao {

    private final int       numeroBalcao;
    private final String    letra;
    private final String    codigo;     // ex: A008
    private final String    nomeServico;
    private final Timestamp horaChamada; // pode ser null (senhas em espera)

    public AtendimentoBalcao(int numeroBalcao, String letra, String codigo,
                              String nomeServico, Timestamp horaChamada) {
        this.numeroBalcao = numeroBalcao;
        this.letra        = letra;
        this.codigo       = codigo;
        this.nomeServico  = nomeServico;
        this.horaChamada  = horaChamada;
    }

    public int       getNumeroBalcao() { return numeroBalcao; }
    public String    getLetra()        { return letra; }
    public String    getCodigo()       { return codigo; }
    public String    getNomeServico()  { return nomeServico; }
    public Timestamp getHoraChamada()  { return horaChamada; }

    /** Número da senha sem a letra (ex: "A008" → "008"). */
    public String getNumero() {
        if (codigo == null || codigo.length() <= 1) return codigo;
        return codigo.substring(1);
    }
}
