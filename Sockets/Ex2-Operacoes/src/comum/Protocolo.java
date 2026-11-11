package comum;

/**
 * Parte comum ao cliente e ao servidor: o protocolo (texto) e as constantes.
 *
 * Protocolo (uma ligação = uma conta):
 *   1. o cliente conecta e LÊ a primeira linha do servidor:
 *        "PRONTO ..."  -> o servidor vai atender este cliente: pode enviar;
 *        "ERRO: servidor ocupado ..." -> já há outro cliente conectado: este é recusado e a ligação é fechada;
 *   2. o cliente envia os inteiros, UM POR LINHA, e por último a operação (também numa linha);
 *   3. o cliente fecha só o lado de ESCRITA do socket (shutdownOutput) -> o servidor lê "fim de arquivo" (EOF);
 *   4. ao detectar o EOF, o servidor faz a operação e responde UMA linha ("RESULTADO: ..." ou "ERRO: ...");
 *   5. o servidor fecha a ligação e fica livre para o próximo cliente.
 *
 * Exemplo do que o cliente envia (depois do "PRONTO"):      10 \n 20 \n 30 \n SOMA \n  <EOF>
 */
public final class Protocolo {
    public static final int PORTA_PADRAO = 7000;

    public static final String PRONTO = "PRONTO: servidor livre, envie os inteiros e a operação";
    public static final String OCUPADO = "ERRO: servidor ocupado atendendo outro cliente. Tente novamente mais tarde.";

    // Operações aceitas (a última linha enviada pelo cliente)
    public static final String SOMA = "SOMA";
    public static final String PRODUTO = "PRODUTO";
    public static final String MEDIA = "MEDIA";
    public static final String MAIOR = "MAIOR";
    public static final String MENOR = "MENOR";
    public static final String LISTA_OPERACOES = "SOMA, PRODUTO, MEDIA, MAIOR, MENOR";

    public static final int MAX_LINHAS = 100000;          // limite de linhas por ligação (proteção do servidor)
    public static final int TEMPO_LIMITE_PADRAO_S = 120;  // se o cliente ficar mudo por tanto tempo, o servidor desiste dele

    private Protocolo() { }
}
