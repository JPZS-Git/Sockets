package comum;

/**
 * Parte comum: onde o chat acontece.
 *
 * Não existe servidor. Todos os participantes entram no mesmo GRUPO MULTICAST (um endereço IP de classe D,
 * de 224.0.0.0 a 239.255.255.255) e escutam a mesma PORTA. Cada mensagem enviada ao grupo é entregue
 * pela rede a todos os que estão nele.
 *
 * Mensagem: um datagrama UDP com um texto em UTF-8, já no formato final de exibição:
 *   "Ana: oi, pessoal"            mensagem normal
 *   "** Ana entrou **"            aviso de entrada
 *   "** Ana saiu **"              aviso de saída
 */
public final class Protocolo {
    public static final String GRUPO = "226.1.1.1";    // o mesmo grupo do exemplo da aula
    public static final int PORTA = 6789;

    public static final int MAX_MENSAGEM = 1000;       // bytes por datagrama (o buffer de recepção tem 1024)
    public static final String SAIR = "/sair";

    private Protocolo() { }
}
