package servidor;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atende UM cliente (roda em uma thread própria): faz o login e depois trata UPLOAD e DOWNLOAD. */
public class AtendeCliente implements Runnable {
    private static final int TEMPO_TRANSFERENCIA_MS = 30_000;    // se o cliente ficar mudo no meio de um arquivo, desiste

    private final Socket socket;
    private final Usuarios usuarios;
    private final Armazenamento armazenamento;

    public AtendeCliente(Socket socket, Usuarios usuarios, Armazenamento armazenamento) {
        this.socket = socket;
        this.usuarios = usuarios;
        this.armazenamento = armazenamento;
    }

    @Override
    public void run() {
        String quem = "[" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "]";
        System.out.println(quem + " conectou");
        try (Socket s = socket) {
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            String usuario = login(in, out, quem);
            if (usuario != null) {
                while (true) {
                    String comando = in.readUTF();
                    if (comando.equalsIgnoreCase(Protocolo.UPLOAD)) {
                        upload(in, out, usuario, quem);
                    } else if (comando.equalsIgnoreCase(Protocolo.DOWNLOAD)) {
                        download(in, out, usuario, quem);
                    } else if (comando.equalsIgnoreCase(Protocolo.SAIR)) {
                        out.writeUTF("Até logo!");
                        break;
                    } else {
                        // não dá para saber quantos campos vieram junto: encerra em vez de se perder no fluxo
                        out.writeUTF("ERRO: comando desconhecido. Use UPLOAD, DOWNLOAD ou SAIR. Encerrando a conexão.");
                        break;
                    }
                }
            }
        } catch (EOFException e) {
            // cliente fechou a conexão: normal
        } catch (IOException e) {
            System.out.println(quem + " falhou: " + e.getMessage());
        }
        System.out.println(quem + " desconectou");
    }

    /** Pede LOGIN (até 3 tentativas). Devolve o usuário ou null se não conseguiu entrar. */
    private String login(DataInputStream in, DataOutputStream out, String quem) throws IOException {
        for (int tentativa = 1; tentativa <= Protocolo.MAX_TENTATIVAS_LOGIN; tentativa++) {
            String comando = in.readUTF();
            if (comando.equalsIgnoreCase(Protocolo.SAIR)) {
                out.writeUTF("Até logo!");
                return null;
            }
            if (!comando.equalsIgnoreCase(Protocolo.LOGIN)) {
                out.writeUTF("ERRO: faça LOGIN primeiro. Encerrando a conexão.");
                return null;
            }
            String usuario = in.readUTF();
            String senha = in.readUTF();
            if (usuarios.confere(usuario, senha)) {
                out.writeUTF("OK: bem-vindo, " + usuario);
                System.out.println(quem + " login de " + usuario);
                return usuario;
            }
            out.writeUTF("ERRO: usuário ou senha inválidos (tentativa " + tentativa + "/" + Protocolo.MAX_TENTATIVAS_LOGIN + ")");
            System.out.println(quem + " login recusado para '" + usuario + "'");
        }
        return null;
    }

    /**
     * Monta o caminho do arquivo do usuário. Se o sistema de arquivos do servidor não aceitar o nome
     * (por exemplo, acentos quando o sistema não está configurado para UTF-8), responde ERRO e devolve null,
     * em vez de derrubar a conexão.
     */
    private Path resolveOuRecusa(String usuario, String nome, DataOutputStream out) throws IOException {
        try {
            return armazenamento.caminho(usuario, nome);
        } catch (InvalidPathException e) {
            out.writeUTF("ERRO: o servidor não aceita esse nome de arquivo neste sistema");
            return null;
        }
    }

    private void upload(DataInputStream in, DataOutputStream out, String usuario, String quem) throws IOException {
        String nome = in.readUTF();
        long tamanho = in.readLong();

        // o servidor pode recusar ANTES de receber qualquer byte
        if (!Armazenamento.nomeValido(nome)) {
            out.writeUTF("ERRO: nome de arquivo inválido (use letras, números, espaço, ponto, _ ou -)");
            return;
        }
        if (tamanho < 0 || tamanho > Protocolo.MAX_ARQUIVO) {
            out.writeUTF("ERRO: tamanho inválido ou acima do limite de " + (Protocolo.MAX_ARQUIVO / 1024 / 1024) + " MB");
            return;
        }
        Path destino = resolveOuRecusa(usuario, nome, out);
        if (destino == null) return;                                  // o servidor já respondeu ERRO
        out.writeUTF(Protocolo.PRONTO);

        // recebe os bytes para um arquivo TEMPORÁRIO e só troca pelo nome final quando chegou tudo:
        // se o cliente cair no meio, nunca fica um arquivo pela metade (nem o antigo é destruído)
        Path temporario = Files.createTempFile(destino.getParent(), ".upload-", ".tmp");
        boolean completo = false;
        try {
            socket.setSoTimeout(TEMPO_TRANSFERENCIA_MS);
            try (OutputStream arquivo = Files.newOutputStream(temporario)) {
                byte[] buffer = new byte[Protocolo.TAM_BUFFER];
                long restante = tamanho;
                while (restante > 0) {
                    int lidos = in.read(buffer, 0, (int) Math.min(buffer.length, restante));
                    if (lidos < 0) throw new EOFException("cliente fechou antes de terminar o upload");
                    arquivo.write(buffer, 0, lidos);
                    restante -= lidos;
                }
            }
            socket.setSoTimeout(0);                                   // volta a esperar comandos sem limite
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            completo = true;
        } finally {
            if (!completo) Files.deleteIfExists(temporario);         // upload interrompido: apaga o temporário
        }
        out.writeUTF("OK: recebido " + nome + " (" + tamanho + " bytes)");
        System.out.println(quem + " " + usuario + " upload de '" + nome + "' (" + tamanho + " bytes)");
    }

    private void download(DataInputStream in, DataOutputStream out, String usuario, String quem) throws IOException {
        String nome = in.readUTF();
        if (!Armazenamento.nomeValido(nome)) {
            out.writeUTF("ERRO: nome de arquivo inválido");
            return;
        }
        Path arquivo = resolveOuRecusa(usuario, nome, out);
        if (arquivo == null) return;                                  // o servidor já respondeu ERRO
        if (!Files.isRegularFile(arquivo)) {
            out.writeUTF("ERRO: arquivo '" + nome + "' não encontrado");
            return;
        }

        long tamanho = Files.size(arquivo);
        out.writeUTF(Protocolo.OK);
        out.writeLong(tamanho);                                       // o cliente precisa saber quantos bytes vêm
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            byte[] buffer = new byte[Protocolo.TAM_BUFFER];
            long restante = tamanho;
            while (restante > 0) {
                int lidos = entrada.read(buffer, 0, (int) Math.min(buffer.length, restante));
                if (lidos < 0) throw new IOException("o arquivo mudou durante o download");
                out.write(buffer, 0, lidos);
                restante -= lidos;
            }
        }
        System.out.println(quem + " " + usuario + " download de '" + nome + "' (" + tamanho + " bytes)");
    }
}
