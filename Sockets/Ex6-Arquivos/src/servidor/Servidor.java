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
 * Servidor de arquivos (TCP).
 *   uso: java servidor.Servidor [porta] [arquivo-de-usuarios] [pasta-dos-arquivos]
 * Aceita vários clientes ao mesmo tempo: uma thread (AtendeCliente) por conexão.
 */
public class Servidor {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        String arquivoUsuarios = args.length > 1 ? args[1] : "dados/usuarios.txt";
        String pasta = args.length > 2 ? args[2] : "dados/armazenamento";

        Usuarios usuarios = new Usuarios(Paths.get(arquivoUsuarios));
        Armazenamento armazenamento = new Armazenamento(Paths.get(pasta));

        try (ServerSocket serverSocket = new ServerSocket(porta)) {      // escuta em TODAS as interfaces de rede
            System.out.println("Servidor de arquivos iniciado na porta " + porta + " (" + usuarios.quantidade()
                    + " usuários; arquivos em " + armazenamento.base() + ")");
            mostraEnderecos(porta);

            while (true) {
                Socket cliente = serverSocket.accept();                   // bloqueia até chegar um cliente
                new Thread(new AtendeCliente(cliente, usuarios, armazenamento)).start();
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
