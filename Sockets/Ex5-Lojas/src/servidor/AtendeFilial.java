package servidor;

import comum.Dinheiro;
import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/** Atende UMA filial (roda em uma thread própria): recebe as ocorrências do dia e soma no Consolidado. */
public class AtendeFilial implements Runnable {
    private final Socket socket;
    private final Consolidado consolidado;

    public AtendeFilial(Socket socket, Consolidado consolidado) {
        this.socket = socket;
        this.consolidado = consolidado;
    }

    @Override
    public void run() {
        String quem = "[" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "]";
        String filial = null;          // só passa a valer depois que a central aceita o nome
        int recebidas = 0;
        int ignoradas = 0;
        try (Socket s = socket) {
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            // 1) identificação: FILIAL <nome>
            String[] inicio = in.readUTF().trim().split("\\s+");
            if (inicio.length != 2 || !inicio[0].equalsIgnoreCase(Protocolo.FILIAL) || !inicio[1].matches("[A-Za-z0-9_-]{1,20}")) {
                out.writeUTF("ERRO: comece com FILIAL <nome> (letras, números, _ ou -; até 20 caracteres)");
                return;
            }
            if (!consolidado.abre(inicio[1])) {
                out.writeUTF("ERRO: a filial '" + inicio[1] + "' já está enviando dados");
                return;
            }
            filial = inicio[1];
            out.writeUTF("OK: filial " + filial + " registrada");
            System.out.println("[" + filial + "] conectou de " + quem);

            // 2) o dia da filial: ocorrências até chegar o FIM
            while (true) {
                String[] p = in.readUTF().trim().split("\\s+");
                String cmd = p[0].toUpperCase();

                if (cmd.equals(Protocolo.VENDA) || cmd.equals(Protocolo.COMPRA)) {
                    long centavos = valor(p);
                    if (centavos <= 0) { ignoradas++; continue; }
                    consolidado.registra(filial, cmd.equals(Protocolo.VENDA), centavos);   // soma protegida (synchronized)
                    recebidas++;
                    if (recebidas % 500 == 0) System.out.println("[" + filial + "] " + recebidas + " ocorrências recebidas");

                } else if (cmd.equals(Protocolo.FIM)) {
                    int declaradas = p.length > 1 ? inteiro(p[1]) : -1;
                    if (declaradas != recebidas || ignoradas > 0) {
                        out.writeUTF("ERRO: a central recebeu " + recebidas + " ocorrências (" + ignoradas
                                + " ignoradas) e a filial declarou " + declaradas);
                    } else {
                        Totais t = consolidado.totaisDe(filial);
                        out.writeUTF("RESUMO " + t.ocorrencias() + " " + t.vendas + " " + t.compras + " " + t.nVendas + " " + t.nCompras);
                    }
                    consolidado.fecha(filial);        // a filial terminou: sai da lista de "enviando" antes do relatório
                    Totais t = consolidado.totaisDe(filial);
                    System.out.println("[" + filial + "] dia concluído: " + t.ocorrencias() + " ocorrências, vendas "
                            + Dinheiro.formata(t.vendas) + ", compras " + Dinheiro.formata(t.compras));
                    System.out.println(consolidado.relatorio());
                    break;

                } else {
                    ignoradas++;                      // linha que não é do protocolo: ignora (e o FIM acusa o problema)
                }
            }
        } catch (EOFException e) {
            if (filial != null) System.out.println("[" + filial + "] desconectou no meio do dia (após " + recebidas + " ocorrências)");
        } catch (IOException e) {
            System.out.println(quem + " falhou: " + e.getMessage());
        } finally {
            if (filial != null) consolidado.fecha(filial);     // libera o nome para um próximo dia
        }
    }

    private static long valor(String[] p) {
        if (p.length != 2) return -1;
        try { return Long.parseLong(p[1]); } catch (NumberFormatException e) { return -1; }
    }

    private static int inteiro(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }
}
