package cliente;

import comum.Protocolo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Chat simples com multicast (UDP). Cada participante roda uma cópia deste programa; não há servidor.
 *   uso: java cliente.Chat [ip-da-interface-de-rede]
 *
 * Duas partes rodam ao mesmo tempo:
 *   - a thread principal lê o que você digita e ENVIA ao grupo;
 *   - uma segunda thread fica em receive() e MOSTRA tudo o que chega do grupo (inclusive as suas mensagens).
 */
public class Chat {
    public static void main(String[] args) throws IOException {
        Scanner teclado = new Scanner(System.in);

        System.out.print("Seu apelido: ");
        String apelido = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        if (apelido.isEmpty()) apelido = "anonimo";

        NetworkInterface rede = escolheInterface(args, teclado);
        if (rede == null) return;

        InetAddress grupo = InetAddress.getByName(Protocolo.GRUPO);
        InetSocketAddress enderecoDoGrupo = new InetSocketAddress(grupo, Protocolo.PORTA);

        MulticastSocket socket = new MulticastSocket(Protocolo.PORTA);
        socket.setNetworkInterface(rede);                      // as mensagens SAEM por esta placa de rede
        socket.joinGroup(enderecoDoGrupo, rede);               // e esta placa passa a RECEBER as mensagens do grupo

        System.out.println("Você entrou no grupo " + Protocolo.GRUPO + ":" + Protocolo.PORTA + " pela interface " + rede.getDisplayName());
        System.out.println("Digite suas mensagens. " + Protocolo.SAIR + " encerra.");

        Thread receptor = new Thread(() -> recebe(socket));
        receptor.setDaemon(true);                              // não impede o programa de terminar
        receptor.start();

        envia(socket, enderecoDoGrupo, "** " + apelido + " entrou **");
        while (teclado.hasNextLine()) {
            String linha = teclado.nextLine().trim();
            if (linha.isEmpty()) continue;
            if (linha.equalsIgnoreCase(Protocolo.SAIR)) break;
            envia(socket, enderecoDoGrupo, apelido + ": " + linha);
        }

        envia(socket, enderecoDoGrupo, "** " + apelido + " saiu **");
        try { Thread.sleep(300); } catch (InterruptedException e) { }   // dá tempo de o aviso de saída ser entregue
        socket.leaveGroup(enderecoDoGrupo, rede);
        socket.close();                                        // isto também faz o receive() da outra thread terminar
    }

    /** Envia um texto ao grupo: um único datagrama. */
    private static void envia(MulticastSocket socket, InetSocketAddress grupo, String texto) {
        byte[] dados = texto.getBytes(StandardCharsets.UTF_8);
        if (dados.length > Protocolo.MAX_MENSAGEM) {
            System.out.println("(mensagem grande demais: no máximo " + Protocolo.MAX_MENSAGEM + " bytes)");
            return;
        }
        try {
            socket.send(new DatagramPacket(dados, dados.length, grupo));
        } catch (IOException e) {
            System.out.println("(não consegui enviar: " + e.getMessage() + ")");
        }
    }

    /** Roda na segunda thread: espera datagramas do grupo e imprime cada um. */
    private static void recebe(MulticastSocket socket) {
        SimpleDateFormat hora = new SimpleDateFormat("HH:mm:ss");
        byte[] buffer = new byte[1024];
        while (!socket.isClosed()) {
            try {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote);                        // bloqueia até chegar uma mensagem
                String texto = new String(pacote.getData(), 0, pacote.getLength(), StandardCharsets.UTF_8);
                System.out.println("[" + hora.format(new Date()) + "] " + texto + "   (de " + pacote.getAddress().getHostAddress() + ")");
            } catch (SocketException e) {
                break;                                         // o socket foi fechado: fim
            } catch (IOException e) {
                System.out.println("(erro ao receber: " + e.getMessage() + ")");
            }
        }
    }

    /**
     * Descobre por qual placa de rede entrar no grupo. Um computador costuma ter várias (Wi-Fi, VPN, máquinas virtuais...)
     * e o multicast só funciona se for pela placa que está na mesma rede dos outros participantes.
     * Se o IP da placa vier como argumento, usa essa; senão lista as placas e pergunta.
     */
    private static NetworkInterface escolheInterface(String[] args, Scanner teclado) throws IOException {
        List<NetworkInterface> placas = new ArrayList<>();
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (ni.isUp() && !ni.isLoopback() && ni.supportsMulticast() && ipv4De(ni) != null) placas.add(ni);
        }
        if (placas.isEmpty()) {
            System.out.println("Nenhuma interface de rede com multicast encontrada. Conecte-se a uma rede e tente de novo.");
            return null;
        }

        // a ordem que o sistema devolve as placas pode mudar de uma execução para outra:
        // ordena pelo IP (como NÚMERO, não como texto), para a lista aparecer sempre na mesma ordem
        Collections.sort(placas, (a, b) -> {
            int porIp = Long.compare(ipComoNumero(ipv4De(a)), ipComoNumero(ipv4De(b)));
            return porIp != 0 ? porIp : a.getDisplayName().compareTo(b.getDisplayName());
        });

        if (args.length > 0) {                                  // IP informado na linha de comando
            for (NetworkInterface ni : placas) {
                if (ipv4De(ni).equals(args[0])) return ni;
            }
            System.out.println("Nenhuma interface com o IP " + args[0] + ". As disponíveis são:");
        }

        if (placas.size() == 1 && args.length == 0) return placas.get(0);    // só uma opção: nem pergunta

        for (int i = 0; i < placas.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + ipv4De(placas.get(i)) + "   " + placas.get(i).getDisplayName());
        }
        System.out.print("Interface de rede [1]: ");
        String resposta = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        int escolha = 1;
        try {
            if (!resposta.isEmpty()) escolha = Integer.parseInt(resposta);
        } catch (NumberFormatException e) {
            escolha = 0;
        }
        if (escolha < 1 || escolha > placas.size()) {
            System.out.println("Opção inválida.");
            return null;
        }
        return placas.get(escolha - 1);
    }

    /** "10.51.2.183" -> 10*256^3 + 51*256^2 + 2*256 + 183. Comparar o número ordena os IPs corretamente. */
    static long ipComoNumero(String ip) {
        long numero = 0;
        for (String parte : ip.split("\\.")) numero = numero * 256 + Integer.parseInt(parte);
        return numero;
    }

    /** O primeiro endereço IPv4 da placa, ou null. */
    private static String ipv4De(NetworkInterface ni) {
        for (InetAddress a : Collections.list(ni.getInetAddresses())) {
            if (a instanceof Inet4Address) return a.getHostAddress();
        }
        return null;
    }
}