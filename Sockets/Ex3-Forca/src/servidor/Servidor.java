package servidor;

import comum.Protocolo;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Servidor do jogo da forca (TCP).
 *   uso: java servidor.Servidor [porta] [arquivo-de-palavras]
 * Cada conexão é uma partida de 1 jogador. Há uma thread por jogador, e as partidas são independentes.
 */
public class Servidor {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        String arquivo = args.length > 1 ? args[1] : "dados/palavras.txt";

        BancoPalavras banco = new BancoPalavras(Paths.get(arquivo));

        try (ServerSocket serverSocket = new ServerSocket(porta)) {      // escuta em TODAS as interfaces de rede
            System.out.println("Servidor da forca iniciado na porta " + porta + " (" + banco.quantidade() + " palavras em " + arquivo + ")");
            mostraEnderecos(porta);

            while (true) {
                Socket jogador = serverSocket.accept();                   // bloqueia até chegar um jogador
                new Thread(new Partida(jogador, banco)).start();         // uma thread por partida
            }
        } catch (IOException e) {
            System.out.println("Não foi possível iniciar o servidor na porta " + porta + ": " + e.getMessage());
        }
    }

    /** Mostra os IPs desta máquina, para digitar no cliente quando ele roda em OUTRO computador. */
    private static void mostraEnderecos(int porta) {
        System.out.println("Clientes desta máquina:        127.0.0.1:" + porta);
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        System.out.println("Clientes de outros computadores: " + a.getHostAddress() + ":" + porta + "  (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (IOException e) {
            // só informativo
        }
    }
}
