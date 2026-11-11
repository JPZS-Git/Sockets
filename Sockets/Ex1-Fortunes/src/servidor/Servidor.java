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
 * Servidor de Fortunes (TCP).
 *   uso: java servidor.Servidor [porta] [arquivo-de-frases]
 * Aceita vários clientes ao mesmo tempo: para cada conexão aceita cria uma thread (AtendeCliente).
 */
public class Servidor {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        String arquivo = args.length > 1 ? args[1] : "dados/fortunes.txt";

        BancoFortunes banco = new BancoFortunes(Paths.get(arquivo));

        try (ServerSocket serverSocket = new ServerSocket(porta)) {      // escuta em TODAS as interfaces de rede
            System.out.println("Servidor de Fortunes iniciado na porta " + porta + " (" + banco.quantidade() + " frases em " + arquivo + ")");
            mostraEnderecos(porta);

            while (true) {
                Socket cliente = serverSocket.accept();                   // bloqueia até chegar um cliente
                new Thread(new AtendeCliente(cliente, banco)).start();   // uma thread por cliente
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
