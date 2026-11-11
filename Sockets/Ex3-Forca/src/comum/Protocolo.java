package comum;

/**
 * Parte comum ao cliente e ao servidor.
 *
 * Protocolo (texto, writeUTF/readUTF como no Ex1):
 *   - o cliente envia UMA letra por mensagem (ou SAIR para desistir);
 *   - o servidor responde com UMA mensagem cuja PRIMEIRA LINHA é uma etiqueta (tag) e as linhas seguintes são o texto.
 *
 * Etiquetas do servidor:
 *   INICIO    começo do jogo (tamanho da palavra)
 *   ACERTO    a letra existe: mostra a palavra com as letras descobertas
 *   ERRO      a letra não existe: mostra a parte do boneco que surgiu
 *   REPETIDA  a letra já foi tentada (não conta erro)
 *   INVALIDA  o que foi digitado não é uma letra
 *   VITORIA   fim do jogo: o jogador descobriu a palavra
 *   DERROTA   fim do jogo: o boneco foi completado (ou o jogador desistiu)
 */
public final class Protocolo {
    public static final int PORTA_PADRAO = 7000;

    public static final String SAIR = "SAIR";

    public static final String INICIO = "INICIO";
    public static final String ACERTO = "ACERTO";
    public static final String ERRO = "ERRO";
    public static final String REPETIDA = "REPETIDA";
    public static final String INVALIDA = "INVALIDA";
    public static final String VITORIA = "VITORIA";
    public static final String DERROTA = "DERROTA";

    public static final int MAX_ERROS = 6;   // cabeça, tronco, braço esq., braço dir., perna esq., perna dir.

    private Protocolo() { }
}
