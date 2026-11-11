package servidor;

import comum.Dinheiro;
import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;

/** Atende UM cliente (roda em uma thread própria): lê o comando, executa no banco e responde. */
public class AtendeCliente implements Runnable {
    private final Socket socket;
    private final Banco banco;
    private Conta conta = null;           // conta em que este cliente entrou (null = ainda não fez LOGIN)

    public AtendeCliente(Socket socket, Banco banco) {
        this.socket = socket;
        this.banco = banco;
    }

    @Override
    public void run() {
        String quem = "[" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "]";
        System.out.println(quem + " conectou");
        try (Socket s = socket) {
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            out.writeUTF("Bem-vindo ao Banco! Comandos: LOGIN <conta>, DEPOSITO <valor>, SAQUE <valor>, SALDO, SAIR");

            while (true) {
                String linha = in.readUTF().trim();
                if (linha.isEmpty()) continue;
                if (linha.equalsIgnoreCase(Protocolo.SAIR)) {
                    out.writeUTF("Até logo!");
                    break;
                }
                String resposta = processa(linha);
                System.out.println(quem + (conta != null ? " conta " + conta.numero() : "") + " '" + linha + "' -> " + resposta);
                out.writeUTF(resposta);
            }
        } catch (EOFException e) {
            // cliente fechou sem dizer SAIR: normal
        } catch (IOException e) {
            System.out.println(quem + " falhou: " + e.getMessage());
        }
        System.out.println(quem + " desconectou");
    }

    private String processa(String linha) {
        String[] partes = linha.split("\\s+", 2);
        String comando = partes[0].toUpperCase(Locale.ROOT);
        String argumento = partes.length > 1 ? partes[1].trim() : "";

        if (comando.equals(Protocolo.LOGIN)) return login(argumento);

        boolean conhecido = comando.equals(Protocolo.DEPOSITO) || comando.equals(Protocolo.SAQUE) || comando.equals(Protocolo.SALDO);
        if (!conhecido) return "ERRO: comando desconhecido. Use LOGIN, DEPOSITO, SAQUE, SALDO ou SAIR";
        if (conta == null) return "ERRO: faça LOGIN <conta> primeiro";

        switch (comando) {
            case Protocolo.SALDO:
                return "OK: saldo atual " + Dinheiro.formata(conta.saldo());
            case Protocolo.DEPOSITO:
                return deposito(argumento);
            default:
                return saque(argumento);
        }
    }

    private String login(String numero) {
        if (numero.isEmpty()) return "ERRO: use LOGIN <conta>";
        Conta c = banco.busca(numero);
        if (c == null) return "ERRO: conta '" + numero + "' não existe";
        conta = c;
        return "OK: conta " + c.numero() + ", titular " + c.titular() + ". Saldo atual " + Dinheiro.formata(c.saldo());
    }

    private String deposito(String argumento) {
        long valor;
        try {
            valor = Dinheiro.paraCentavos(argumento);
        } catch (IllegalArgumentException e) {
            return "ERRO: " + e.getMessage();
        }
        long novo = conta.deposita(valor);                 // operação protegida (synchronized) na conta
        banco.salvar();
        return "OK: depósito de " + Dinheiro.formata(valor) + ". Saldo atual " + Dinheiro.formata(novo);
    }

    private String saque(String argumento) {
        long valor;
        try {
            valor = Dinheiro.paraCentavos(argumento);
        } catch (IllegalArgumentException e) {
            return "ERRO: " + e.getMessage();
        }
        long novo = conta.saca(valor);
        if (novo < 0) return "ERRO: saldo insuficiente (saldo atual " + Dinheiro.formata(conta.saldo()) + ")";
        banco.salvar();
        return "OK: saque de " + Dinheiro.formata(valor) + ". Saldo atual " + Dinheiro.formata(novo);
    }
}
