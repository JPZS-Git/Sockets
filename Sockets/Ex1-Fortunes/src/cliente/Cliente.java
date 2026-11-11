package cliente;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Cliente do Servidor de Fortunes (TCP).
 *   uso: java cliente.Cliente [porta]
 * Pergunta o IP do servidor, conecta e envia ao servidor cada linha digitada (os comandos do protocolo).
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

            System.out.println(in.readUTF());                                 // boas-vindas do servidor
            System.out.println("(digite um comando; SAIR encerra)");

            while (true) {
                System.out.print("> ");
                if (!teclado.hasNextLine()) {                                 // fim da entrada: sai educadamente
                    out.writeUTF(Protocolo.SAIR);
                    System.out.println(in.readUTF());
                    break;
                }
                String linha = teclado.nextLine().trim();
                if (linha.isEmpty()) continue;

                out.writeUTF(linha);                                          // envia o comando
                System.out.println(in.readUTF());                             // bloqueia até a resposta chegar
                if (linha.equalsIgnoreCase(Protocolo.SAIR)) break;
            }
        } catch (IOException e) {
            System.out.println("Falha na comunicação com " + host + ":" + porta + " -> " + e.getMessage());
            System.out.println("Confira: o servidor está rodando? IP e porta corretos? O firewall liberou a porta?");
        }
    }
}
