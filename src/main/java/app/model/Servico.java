package app.model;

public class Servico {
    private int     idServico;
    private String  nome;
    private String  letra;
    private boolean prioritario;

    public Servico() {}
    public Servico(int id, String nome, String letra, boolean prioritario) {
        this.idServico = id; this.nome = nome;
        this.letra = letra; this.prioritario = prioritario;
    }

    public int     getIdServico()  { return idServico; }
    public String  getNome()       { return nome; }
    public String  getLetra()      { return letra; }
    public boolean isPrioritario() { return prioritario; }

    @Override public String toString() { return nome; }
}
