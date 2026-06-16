package app.model;

import java.time.LocalDateTime;

public class LogAtividade {

    public enum Acao {
        LOGIN, LOGOUT, GERAR_SENHA, CHAMAR_SENHA,
        CLIENTE_AUSENTE, GERAR_PDF, GERAR_EXCEL, ALTERAR_PRIORIDADE
    }

    private int           idUtilizador;
    private String        nomeUtilizador;
    private Acao          acao;
    private LocalDateTime dataHora;
    private String        descricao;
    private Integer       idBalcao;

    public LogAtividade(int idUtilizador, String nomeUtilizador,
                        Acao acao, String descricao, Integer idBalcao) {
        this.idUtilizador   = idUtilizador;
        this.nomeUtilizador = nomeUtilizador;
        this.acao           = acao;
        this.descricao      = descricao;
        this.idBalcao       = idBalcao;
        this.dataHora       = LocalDateTime.now();
    }

    public int           getIdUtilizador()  { return idUtilizador; }
    public String        getNomeUtilizador(){ return nomeUtilizador; }
    public Acao          getAcao()          { return acao; }
    public LocalDateTime getDataHora()      { return dataHora; }
    public String        getDescricao()     { return descricao; }
    public Integer       getIdBalcao()      { return idBalcao; }
}
