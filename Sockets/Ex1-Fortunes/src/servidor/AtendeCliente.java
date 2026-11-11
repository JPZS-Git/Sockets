package servidor;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Atende UM cliente (roda numa thread própria). O servidor cria uma destas para cada conexão aceita,
 * então vários clientes podem usar o servidor ao mesmo tempo.
 */
class AtendeCliente implements Runnable {
    private static final int LIMITE_RESPOSTA = 60000;   // writeUTF aceita no máximo 65535 bytes

    private final Socket socket;
    private final BancoFortunes banco;
    private final String ip;

    AtendeCliente(Socket socket, BancoFortunes banco) {
        this.socket = socket;
        this.banco = banco;
        this.ip = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        System.out.println("[" + ip + "] conectou");
        try (Socket s = socket;
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            out.writeUTF("Conectado ao Servidor de Fortunes. Comandos: " + Protocolo.GET + ", "
                    + Protocolo.ADD + " <frase>, " + Protocolo.UPD + " <pos> <frase>, " + Protocolo.LST + ", " + Protocolo.SAIR);

            while (true) {
                String linha = in.readUTF().trim();          // bloqueia até o cliente enviar um comando
                System.out.println("[" + ip + "] " + linha);
                if (linha.equalsIgnoreCase(Protocolo.SAIR)) {
                    out.writeUTF("Até logo!");
                    break;
                }
                out.writeUTF(limita(processa(linha)));
            }
        } catch (EOFException e) {
            // o cliente fechou a conexão sem mandar SAIR: é normal
        } catch (IOException e) {
            System.out.println("[" + ip + "] erro de comunicação: " + e.getMessage());
        }
        System.out.println("[" + ip + "] desconectou");
    }

    /** Interpreta o comando e devolve o texto da resposta. */
    private String processa(String linha) {
        String[] partes = linha.split("\\s+", 2);            // [comando, resto da linha]
        String comando = partes[0].toUpperCase();
        String resto = partes.length > 1 ? partes[1].trim() : "";

        switch (comando) {
            case Protocolo.GET: {
                String frase = banco.sorteia();
                return frase != null ? frase : "ERRO: o banco de frases está vazio.";
            }
            case Protocolo.ADD: {
                if (resto.isEmpty()) return "ERRO: uso: " + Protocolo.ADD + " <nova frase>";
                if (resto.length() > Protocolo.MAX_FRASE) return "ERRO: frase muito longa (máximo " + Protocolo.MAX_FRASE + " caracteres).";
                int pos = banco.adiciona(resto);
                return "OK: frase adicionada na posição " + pos + ".";
            }
            case Protocolo.UPD: {
                String[] a = resto.split("\\s+", 2);          // [pos, nova frase]
                if (a.length < 2 || a[1].trim().isEmpty()) return "ERRO: uso: " + Protocolo.UPD + " <pos> <nova frase>";
                int pos;
                try {
                    pos = Integer.parseInt(a[0]);
                } catch (NumberFormatException e) {
                    return "ERRO: posição inválida. Use o número mostrado em " + Protocolo.LST + ".";
                }
                String nova = a[1].trim();
                if (nova.length() > Protocolo.MAX_FRASE) return "ERRO: frase muito longa (máximo " + Protocolo.MAX_FRASE + " caracteres).";
                if (!banco.atualiza(pos, nova)) return "ERRO: a posição " + pos + " não existe (há " + banco.quantidade() + " frases).";
                return "OK: frase " + pos + " atualizada.";
            }
            case Protocolo.LST: {
                List<String> frases = banco.lista();
                if (frases.isEmpty()) return "(nenhuma frase cadastrada)";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < frases.size(); i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(i + 1).append(". ").append(frases.get(i));
                }
                return sb.toString();
            }
            default:
                return "ERRO: comando desconhecido '" + comando + "'. Use " + Protocolo.GET + ", " + Protocolo.ADD
                        + ", " + Protocolo.UPD + ", " + Protocolo.LST + " ou " + Protocolo.SAIR + ".";
        }
    }

    /** writeUTF não aceita texto grande demais: se a lista passar do limite, corta e avisa. */
    private String limita(String resposta) {
        if (resposta.length() <= LIMITE_RESPOSTA / 3) return resposta;   // folga para acentos (até 3 bytes por caractere)
        return resposta.substring(0, LIMITE_RESPOSTA / 3) + "\n... (resposta cortada: muito longa)";
    }
}
