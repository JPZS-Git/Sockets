package servidor;

import comum.Protocolo;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;

/**
 * Sistema central da rede de lojas (TCP).
 *   uso: java servidor.Servidor [porta]
 * Recebe dados de n filiais ao mesmo tempo: uma thread (AtendeFilial) por filial conectada.
 * Todas somam no MESMO objeto Consolidado.
 */
public class Servidor {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        Consolidado consolidado = new Consolidado();

        try (ServerSocket serverSocket = new ServerSocket(porta)) {      // escuta em TODAS as interfaces de rede
            System.out.println("Sistema central iniciado na porta " + porta);
            mostraEnderecos(porta);

            while (true) {
                Socket filial = serverSocket.accept();                    // bloqueia até chegar uma filial
                new Thread(new AtendeFilial(filial, consolidado)).start();
            }
        } catch (IOException e) {
            System.out.println("Não foi possível iniciar o servidor na porta " + porta + ": " + e.getMessage());
        }
    }

    /** Mostra os IPs desta máquina, para digitar nas filiais quando elas rodam em OUTRO computador. */
    private static void mostraEnderecos(int porta) {
        System.out.println("Filiais desta máquina:        127.0.0.1:" + porta);
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        System.out.println("Filiais de outros computadores: " + a.getHostAddress() + ":" + porta + "  (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (IOException e) {
            // só informativo
        }
    }
}
