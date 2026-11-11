package cliente;

import comum.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Cliente do servidor de operações (TCP).
 *   uso: java cliente.Cliente [porta]
 * Cada conta usa uma ligação: conecta, envia os inteiros e a operação, avisa "terminei" (EOF) e lê o resultado.
 */
public class Cliente {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        Scanner teclado = new Scanner(System.in);

        System.out.print("IP do servidor [127.0.0.1]: ");
        String host = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        if (host.isEmpty()) host = "127.0.0.1";

        boolean outra = true;
        while (outra) {
            if (!umaConta(host, porta, teclado)) break;                   // falha de comunicação: para

            System.out.print("\nFazer outra conta? (s/n) ");
            outra = teclado.hasNextLine() && teclado.nextLine().trim().equalsIgnoreCase("s");
        }
    }

    /** Uma ligação completa. Devolve false se não deu para falar com o servidor. */
    private static boolean umaConta(String host, int porta, Scanner teclado) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 5000);      // espera no máximo 5 s
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // a primeira linha do servidor diz se ele vai atender este cliente ou se está ocupado com outro
            String saudacao = in.readLine();
            if (saudacao == null || !saudacao.startsWith("PRONTO")) {
                System.out.println("Servidor: " + (saudacao != null ? saudacao : "(conexão encerrada)"));
                return false;
            }

            System.out.println("Conectado. Digite inteiros e termine com a operação (" + Protocolo.LISTA_OPERACOES + ").");
            System.out.println("Exemplo: 10 20 30 SOMA   (pode ser em várias linhas)");

            boolean terminou = false;
            while (!terminou && teclado.hasNextLine()) {
                System.out.print("> ");
                String[] pedacos = teclado.nextLine().trim().split("\\s+");
                for (String p : pedacos) {
                    if (p.isEmpty()) continue;
                    out.println(p);                                        // cada pedaço vira uma linha no socket
                    if (!ehInteiro(p)) {                                   // não é número: é a operação, a última linha
                        terminou = true;
                        break;
                    }
                }
            }

            socket.shutdownOutput();                                       // fecha só a ESCRITA: o servidor lê EOF e começa a conta
            System.out.println("(fim dos dados enviado; esperando o servidor...)");

            String resposta = in.readLine();                               // bloqueia até o servidor responder
            System.out.println("Servidor: " + (resposta != null ? resposta : "(sem resposta)"));
            return true;
        } catch (IOException e) {
            System.out.println("Falha na comunicação com " + host + ":" + porta + " -> " + e.getMessage());
            System.out.println("Confira: o servidor está rodando? IP e porta corretos? O firewall liberou a porta?");
            return false;
        }
    }

    private static boolean ehInteiro(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
