package cliente;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Cliente do jogo da forca (TCP).
 *   uso: java cliente.Cliente [porta]
 * Mostra o que o servidor devolve e envia cada letra digitada. Termina quando o servidor diz VITORIA ou DERROTA.
 */
public class Cliente {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        Scanner teclado = new Scanner(System.in);

        System.out.print("IP do servidor [127.0.0.1]: ");
        String host = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        if (host.isEmpty()) host = "127.0.0.1";

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 5000);       // espera no máximo 5 s
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            boolean fim = mostra(in.readUTF());                               // mensagem de INICIO (tamanho da palavra)
            while (!fim) {
                System.out.print("> ");
                if (!teclado.hasNextLine()) {                                 // fim da entrada: desiste educadamente
                    out.writeUTF(Protocolo.SAIR);
                    mostra(in.readUTF());
                    break;
                }
                out.writeUTF(teclado.nextLine());                             // envia a letra
                fim = mostra(in.readUTF());                                   // bloqueia até o servidor responder
            }
        } catch (IOException e) {
            System.out.println("Falha na comunicação com " + host + ":" + porta + " -> " + e.getMessage());
            System.out.println("Confira: o servidor está rodando? IP e porta corretos? O firewall liberou a porta?");
        }
    }

    /** Imprime a mensagem sem a etiqueta (1ª linha). Devolve true se o jogo acabou (VITORIA ou DERROTA). */
    private static boolean mostra(String mensagem) {
        int quebra = mensagem.indexOf('\n');
        String etiqueta = quebra < 0 ? mensagem : mensagem.substring(0, quebra);
        String texto = quebra < 0 ? "" : mensagem.substring(quebra + 1);
        System.out.println(texto);
        return etiqueta.equals(Protocolo.VITORIA) || etiqueta.equals(Protocolo.DERROTA);
    }
}
