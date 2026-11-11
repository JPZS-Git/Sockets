package servidor;

import comum.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor de operações sobre inteiros (TCP) - atende UM CLIENTE DE CADA VEZ.
 *   uso: java servidor.Servidor [porta] [tempo-limite-em-segundos]
 *
 * Só UM cliente por vez: enquanto há um cliente conectado, qualquer outro que tente conectar é RECUSADO
 * na hora, com a mensagem "servidor ocupado". Quando o cliente atual termina, o servidor fica livre de novo.
 *
 * Como: um indicador "ocupado" (AtomicBoolean). A thread principal só aceita conexões e consulta o indicador:
 * se estiver livre, marca como ocupado e entrega o cliente a uma thread que o atende; se estiver ocupado,
 * responde "ocupado" e fecha a ligação. Nunca há dois clientes sendo atendidos ao mesmo tempo.
 */
public class Servidor {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        int tempoLimiteS = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.TEMPO_LIMITE_PADRAO_S;
        int tempoLimiteMs = tempoLimiteS * 1000;
        AtomicBoolean ocupado = new AtomicBoolean(false);                 // true enquanto há um cliente sendo atendido

        try (ServerSocket serverSocket = new ServerSocket(porta)) {      // escuta em TODAS as interfaces de rede
            System.out.println("Servidor de operações iniciado na porta " + porta
                    + " (um cliente por vez, os outros são recusados; desiste de cliente mudo após " + tempoLimiteS + " s)");
            mostraEnderecos(porta);

            while (true) {
                Socket cliente = serverSocket.accept();                   // bloqueia até chegar um cliente
                if (!ocupado.compareAndSet(false, true)) {                // tenta passar de "livre" para "ocupado", de uma vez só
                    recusa(cliente);                                      // já havia alguém: recusa
                    continue;
                }
                new Thread(() -> {                                        // livre: atende este cliente
                    try {
                        atende(cliente, tempoLimiteMs);                   // (não fecha o socket)
                    } finally {
                        ocupado.set(false);                               // 1º libera o servidor...
                        fecha(cliente);                                   // 2º só então fecha a ligação: quando o cliente perceber
                    }                                                     //    o fim, o servidor já está livre para o próximo
                }).start();
            }
        } catch (IOException e) {
            System.out.println("Não foi possível iniciar o servidor na porta " + porta + ": " + e.getMessage());
        }
    }

    /** Responde "ocupado" a um cliente que chegou durante outro atendimento e fecha a ligação. */
    private static void recusa(Socket cliente) {
        String quem = "[" + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort() + "]";
        try (Socket s = cliente) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(Protocolo.OCUPADO);
        } catch (IOException e) {
            // o cliente já foi embora: nada a fazer
        }
        System.out.println(quem + " recusado (servidor ocupado)");
    }

    private static void fecha(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // já estava fechado
        }
    }

    /** Uma ligação inteira: diz que está pronto, lê até o EOF, calcula e responde. Quem fecha o socket é quem chamou. */
    private static void atende(Socket cliente, int tempoLimiteMs) {
        String quem = "[" + cliente.getInetAddress().getHostAddress() + ":" + cliente.getPort() + "]";
        System.out.println(quem + " conectou");

        try {
            cliente.setSoTimeout(tempoLimiteMs);                          // readLine não pode travar para sempre
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(Protocolo.PRONTO);                                // primeira linha: "pode enviar"

            List<String> linhas = new ArrayList<>();
            String erro = null;
            try {
                String linha;
                while ((linha = in.readLine()) != null) {                 // readLine devolve null no EOF = "o cliente terminou"
                    linha = linha.trim();
                    if (linha.isEmpty()) continue;
                    if (linhas.size() >= Protocolo.MAX_LINHAS) {
                        erro = "ERRO: mais de " + Protocolo.MAX_LINHAS + " linhas";
                        break;
                    }
                    linhas.add(linha);
                }
            } catch (SocketTimeoutException e) {
                erro = "ERRO: tempo esgotado, o servidor não recebeu o fim dos dados";
            }

            if (erro == null && linhas.isEmpty()) {                       // conectou e saiu sem mandar nada
                System.out.println(quem + " desconectou sem enviar nada");
                return;
            }

            String resposta = (erro != null) ? erro : Calculadora.calcula(linhas);   // aqui a operação é executada
            out.println(resposta);                                        // responde (println envia na hora: autoflush)
            System.out.println(quem + " recebeu " + linhas.size() + " linha(s) -> " + resposta);
        } catch (IOException e) {
            System.out.println(quem + " falhou: " + e.getMessage());     // ex.: cliente caiu no meio; o servidor segue
        }
        System.out.println(quem + " desconectou");
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
