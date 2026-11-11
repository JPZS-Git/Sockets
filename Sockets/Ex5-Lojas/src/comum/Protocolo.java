package comum;

/**
 * Parte comum ao cliente (filial) e ao servidor (sistema central).
 *
 * Protocolo (texto, writeUTF/readUTF):
 *   filial -> central   FILIAL <nome>           central -> filial   OK: ...   (ou ERRO: ...)
 *   filial -> central   VENDA <centavos>        (sem resposta; uma por ocorrência)
 *   filial -> central   COMPRA <centavos>       (sem resposta; uma por ocorrência)
 *   filial -> central   FIM <quantidade>        central -> filial   RESUMO <ocorrencias> <vendas> <compras> <nVendas> <nCompras>
 *                                                                    (ou ERRO: ... se a contagem não bater)
 *
 * VENDA  = a loja vendeu a um cliente (entra dinheiro).
 * COMPRA = a loja comprou mercadoria (sai dinheiro).
 * Os valores trafegam em CENTAVOS (inteiros), para a soma ser sempre exata.
 */
public final class Protocolo {
    public static final int PORTA_PADRAO = 7000;

    public static final String FILIAL = "FILIAL";
    public static final String VENDA = "VENDA";
    public static final String COMPRA = "COMPRA";
    public static final String FIM = "FIM";

    public static final int OCORRENCIAS_PADRAO = 1500;   // movimentação diária de cada filial (enunciado)
    public static final int ATRASO_PADRAO_MS = 10;       // atraso entre ocorrências, para simular o tempo real
    public static final int FILIAIS_PADRAO = 5;          // mínimo do enunciado

    private Protocolo() { }
}
