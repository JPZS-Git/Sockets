package comum;

/**
 * Parte comum ao cliente e ao servidor: os comandos do protocolo (texto) e a porta padrão.
 *
 * Protocolo: o cliente envia UMA linha de texto (writeUTF) e o servidor responde com UMA linha (writeUTF).
 *   LOGIN <conta>       -> entra na conta (ex.: LOGIN 1001); vários terminais podem entrar na MESMA conta
 *   DEPOSITO <valor>    -> soma o valor ao saldo (ex.: DEPOSITO 100,50)
 *   SAQUE <valor>       -> subtrai o valor, se houver saldo suficiente
 *   SALDO               -> mostra o saldo atual
 *   SAIR                -> encerra a conexão
 * As respostas começam com "OK:" ou "ERRO:".
 */
public final class Protocolo {
    public static final int PORTA_PADRAO = 7000;

    public static final String LOGIN = "LOGIN";
    public static final String DEPOSITO = "DEPOSITO";
    public static final String SAQUE = "SAQUE";
    public static final String SALDO = "SALDO";
    public static final String SAIR = "SAIR";

    private Protocolo() { }
}
