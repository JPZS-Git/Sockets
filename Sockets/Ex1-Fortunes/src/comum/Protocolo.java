package comum;

/**
 * Parte comum ao cliente e ao servidor: os comandos do protocolo (texto) e a porta padrão.
 *
 * Protocolo: o cliente envia UMA linha de texto (writeUTF) e o servidor responde com UMA linha (writeUTF).
 *   GET-FORTUNE                      -> uma frase sorteada
 *   ADD-FORTUNE <nova frase>         -> adiciona a frase ao banco
 *   UPD-FORTUNE <pos> <nova frase>   -> troca a frase da posição <pos> (a numeração é a do LST-FORTUNE, começa em 1)
 *   LST-FORTUNE                      -> todas as frases numeradas
 *   SAIR                             -> encerra a conexão
 */
public final class Protocolo {
    public static final int PORTA_PADRAO = 7000;

    public static final String GET = "GET-FORTUNE";
    public static final String ADD = "ADD-FORTUNE";
    public static final String UPD = "UPD-FORTUNE";
    public static final String LST = "LST-FORTUNE";
    public static final String SAIR = "SAIR";

    public static final int MAX_FRASE = 500;   // tamanho máximo de uma frase (caracteres)

    private Protocolo() { }
}
