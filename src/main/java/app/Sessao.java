package app;

/**
 * Singleton que guarda o utilizador autenticado durante a sessão.
 */
public class Sessao {

    public enum Nivel { ADMINISTRADOR, SUPERVISOR, ATENDENTE, DESCONHECIDO }

    private static Sessao instancia;

    private int    idUtilizador;
    private String nome;
    private String username;
    private Nivel  nivel;

    private Sessao() {}

    public static Sessao get() {
        if (instancia == null) instancia = new Sessao();
        return instancia;
    }

    public void iniciar(int id, String nome, String username, String nivelNome) {
        this.idUtilizador = id;
        this.nome         = nome;
        this.username     = username;
        this.nivel        = parsearNivel(nivelNome);
    }

    public void terminar() {
        idUtilizador = 0;
        nome = null;
        username = null;
        nivel = Nivel.DESCONHECIDO;
    }

    private Nivel parsearNivel(String n) {
        if (n == null) return Nivel.DESCONHECIDO;
        return switch (n.trim().toUpperCase()) {
            case "ADMINISTRADOR" -> Nivel.ADMINISTRADOR;
            case "SUPERVISOR"    -> Nivel.SUPERVISOR;
            case "ATENDENTE"     -> Nivel.ATENDENTE;
            default              -> Nivel.DESCONHECIDO;
        };
    }

    public int    getIdUtilizador() { return idUtilizador; }
    public String getNome()         { return nome; }
    public String getUsername()     { return username; }
    public Nivel  getNivel()        { return nivel; }

    public boolean isGerente() {
        return nivel == Nivel.ADMINISTRADOR || nivel == Nivel.SUPERVISOR;
    }
}
