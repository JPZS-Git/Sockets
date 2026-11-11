package comum;

/**
 * Parte comum ao cliente e ao servidor.
 *
 * Protocolo (DataInputStream/DataOutputStream: texto com writeUTF, tamanhos com writeLong, arquivos em bytes):
 *
 *   LOGIN     C: "LOGIN", usuario, senha                     S: "OK: ..." ou "ERRO: ..."   (até 3 tentativas)
 *   UPLOAD    C: "UPLOAD", nome, tamanho(long)               S: "PRONTO" ou "ERRO: ..."
 *             C: <tamanho bytes do arquivo>                  S: "OK: ..." ou "ERRO: ..."
 *   DOWNLOAD  C: "DOWNLOAD", nome                            S: "OK", tamanho(long), <tamanho bytes>   ou   "ERRO: ..."
 *   SAIR      C: "SAIR"                                      S: "Até logo!"
 *
 * O cliente só envia os bytes do upload DEPOIS de receber "PRONTO": assim o servidor pode recusar o arquivo
 * (nome inválido, grande demais) sem que os bytes se misturem com os próximos comandos.
 * São só dois comandos de arquivo, como pede o enunciado: não há listar nem apagar.
 */
public final class Protocolo {
    public static final int PORTA_PADRAO = 7000;

    public static final String LOGIN = "LOGIN";
    public static final String UPLOAD = "UPLOAD";
    public static final String DOWNLOAD = "DOWNLOAD";
    public static final String SAIR = "SAIR";

    public static final String PRONTO = "PRONTO";
    public static final String OK = "OK";

    public static final long MAX_ARQUIVO = 100L * 1024 * 1024;   // 100 MB por arquivo
    public static final int TAM_BUFFER = 8192;                    // os arquivos viajam em pedaços de 8 KB
    public static final int MAX_TENTATIVAS_LOGIN = 3;

    private Protocolo() { }
}
