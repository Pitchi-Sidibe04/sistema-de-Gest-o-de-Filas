package app.model;

/**
 * Objecto imutável que encapsula os dados de um serviço seleccionado.
 * Inclui o caminho do ficheiro PDF escolhido pelo utilizador antes de
 * navegar para a tela de confirmação.
 */
public final class ServicoInfo {

    private final String  nome;
    private final String  letra;
    private final boolean prioritario;
    private final int     idServico;
    private final String  caminhoPdf;   // null → usa pasta automática

    public ServicoInfo(String nome, String letra, boolean prioritario, int idServico, String caminhoPdf) {
        this.nome        = nome;
        this.letra       = letra;
        this.prioritario = prioritario;
        this.idServico   = idServico;
        this.caminhoPdf  = caminhoPdf;
    }

    /** Com idServico mas sem caminhoPdf. */
    public ServicoInfo(String nome, String letra, boolean prioritario, int idServico) {
        this(nome, letra, prioritario, idServico, null);
    }

    /** Sem idServico — resolvido pelo DAO pela letra. */
    public ServicoInfo(String nome, String letra, boolean prioritario) {
        this(nome, letra, prioritario, -1, null);
    }

    public String  getNome()       { return nome; }
    public String  getLetra()      { return letra; }
    public boolean isPrioritario() { return prioritario; }
    public int     getIdServico()  { return idServico; }
    public String  getCaminhoPdf() { return caminhoPdf; }

    @Override
    public String toString() {
        return nome + " (" + letra + ")" + (prioritario ? " [PRIORITÁRIO]" : "");
    }
}
