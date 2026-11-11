package cliente;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

/**
 * O dia de UMA filial: conecta na central, simula as ocorrências de compras e vendas (com um atraso entre elas)
 * e, no fim, confere o resumo da central com os totais que a própria filial calculou.
 */
public class FilialSimulada {

    /** Devolve true se tudo deu certo e a central concorda com os totais da filial. */
    public static boolean executa(String nome, String host, int porta, int ocorrencias, int atrasoMs) {
        Random sorteio = new Random();
        long vendas = 0, compras = 0;
        int nVendas = 0, nCompras = 0;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 5000);       // espera no máximo 5 s
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF(Protocolo.FILIAL + " " + nome);
            String resposta = in.readUTF();
            if (!resposta.startsWith("OK")) {
                System.out.println("[" + nome + "] recusada pela central: " + resposta);
                return false;
            }
            System.out.println("[" + nome + "] conectada a " + host + ":" + porta + ", enviando " + ocorrencias + " ocorrências");

            for (int i = 1; i <= ocorrencias; i++) {
                boolean venda = sorteio.nextInt(100) < 60;                  // 60% vendas, 40% compras
                long centavos = 500 + sorteio.nextInt(49501);               // de R$ 5,00 a R$ 500,00
                out.writeUTF((venda ? Protocolo.VENDA : Protocolo.COMPRA) + " " + centavos);
                if (venda) { vendas += centavos; nVendas++; } else { compras += centavos; nCompras++; }

                if (atrasoMs > 0) Thread.sleep(atrasoMs);                   // simula o tempo de cada ocorrência
                if (i % 500 == 0) System.out.println("[" + nome + "] " + i + "/" + ocorrencias + " enviadas");
            }

            out.writeUTF(Protocolo.FIM + " " + ocorrencias);
            String resumo = in.readUTF();                                   // espera o resumo da central

            String esperado = "RESUMO " + ocorrencias + " " + vendas + " " + compras + " " + nVendas + " " + nCompras;
            if (resumo.equals(esperado)) {
                System.out.println("[" + nome + "] conferido: a central e a filial concordam (vendas " + vendas + " e compras " + compras + " centavos)");
                return true;
            }
            System.out.println("[" + nome + "] DIVERGÊNCIA! central: '" + resumo + "' | filial: '" + esperado + "'");
            return false;
        } catch (IOException e) {
            System.out.println("[" + nome + "] falha na comunicação com " + host + ":" + porta + " -> " + e.getMessage());
            System.out.println("Confira: o servidor está rodando? IP e porta corretos? O firewall liberou a porta?");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
