package cliente;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

/**
 * Cliente do servidor de arquivos (TCP).
 *   uso: java cliente.Cliente [porta]
 * Faz o login e depois aceita os comandos:
 *   upload <caminho-do-arquivo>     envia um arquivo seu para o servidor
 *   download <nome-do-arquivo>      baixa um arquivo seu do servidor para a pasta "downloads"
 *   sair
 */
public class Cliente {
    private static final String PASTA_DOWNLOADS = "downloads";

    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_PADRAO;
        Scanner teclado = new Scanner(System.in);

        System.out.print("IP do servidor [127.0.0.1]: ");
        String host = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        if (host.isEmpty()) host = "127.0.0.1";

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 5000);       // espera no máximo 5 s
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            if (!login(teclado, in, out)) return;

            System.out.println("Comandos: upload <caminho>, download <nome>, sair");
            while (true) {
                System.out.print("> ");
                if (!teclado.hasNextLine()) { sair(in, out); break; }       // fim da entrada: sai educadamente
                String linha = teclado.nextLine().trim();
                if (linha.isEmpty()) continue;

                String[] p = linha.split("\\s+", 2);
                String comando = p[0].toLowerCase();
                String argumento = p.length > 1 ? tiraAspas(p[1].trim()) : "";

                if (comando.equals("sair")) { sair(in, out); break; }
                else if (comando.equals("upload")) upload(argumento, in, out);
                else if (comando.equals("download")) download(argumento, in, out);
                else System.out.println("Comando desconhecido. Use: upload <caminho>, download <nome>, sair");
            }
        } catch (EOFException e) {
            System.out.println("O servidor encerrou a conexão.");
        } catch (IOException e) {
            System.out.println("Falha na comunicação com " + host + ":" + porta + " -> " + e.getMessage());
            System.out.println("Confira: o servidor está rodando? IP e porta corretos? O firewall liberou a porta?");
        }
    }

    private static boolean login(Scanner teclado, DataInputStream in, DataOutputStream out) throws IOException {
        for (int i = 0; i < Protocolo.MAX_TENTATIVAS_LOGIN; i++) {
            System.out.print("Usuário: ");
            if (!teclado.hasNextLine()) return false;
            String usuario = teclado.nextLine().trim();
            System.out.print("Senha: ");
            if (!teclado.hasNextLine()) return false;
            String senha = teclado.nextLine().trim();

            out.writeUTF(Protocolo.LOGIN);
            out.writeUTF(usuario);
            out.writeUTF(senha);
            String resposta = in.readUTF();
            System.out.println(resposta);
            if (resposta.startsWith(Protocolo.OK)) return true;
        }
        System.out.println("Número máximo de tentativas atingido.");
        return false;
    }

    private static void sair(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeUTF(Protocolo.SAIR);
        System.out.println(in.readUTF());
    }

    /** UPLOAD: anuncia nome e tamanho, espera "PRONTO" e então envia os bytes. */
    private static void upload(String caminho, DataInputStream in, DataOutputStream out) throws IOException {
        File arquivo = new File(caminho);
        if (caminho.isEmpty() || !arquivo.isFile()) {
            System.out.println("Arquivo não encontrado: '" + caminho + "'");
            return;
        }
        long tamanho = arquivo.length();

        out.writeUTF(Protocolo.UPLOAD);
        out.writeUTF(arquivo.getName());
        out.writeLong(tamanho);
        String resposta = in.readUTF();
        if (!resposta.equals(Protocolo.PRONTO)) {                         // o servidor recusou: não envia nada
            System.out.println(resposta);
            return;
        }

        try (InputStream entrada = Files.newInputStream(arquivo.toPath())) {
            byte[] buffer = new byte[Protocolo.TAM_BUFFER];
            int lidos;
            while ((lidos = entrada.read(buffer)) > 0) out.write(buffer, 0, lidos);
        }
        System.out.println(in.readUTF());                                 // confirmação final do servidor
    }

    /** DOWNLOAD: pede o arquivo, lê o tamanho e recebe exatamente esse número de bytes. */
    private static void download(String nome, DataInputStream in, DataOutputStream out) throws IOException {
        if (nome.isEmpty()) {
            System.out.println("Uso: download <nome-do-arquivo>");
            return;
        }
        out.writeUTF(Protocolo.DOWNLOAD);
        out.writeUTF(nome);
        String resposta = in.readUTF();
        if (!resposta.equals(Protocolo.OK)) {
            System.out.println(resposta);
            return;
        }
        long tamanho = in.readLong();

        Files.createDirectories(Paths.get(PASTA_DOWNLOADS));
        Path destino = Paths.get(PASTA_DOWNLOADS, nome);
        Path temporario = Paths.get(PASTA_DOWNLOADS, "." + nome + ".parcial");     // só vira o arquivo final se chegar completo
        try (OutputStream arquivo = Files.newOutputStream(temporario)) {
            byte[] buffer = new byte[Protocolo.TAM_BUFFER];
            long restante = tamanho;
            while (restante > 0) {
                int lidos = in.read(buffer, 0, (int) Math.min(buffer.length, restante));
                if (lidos < 0) throw new EOFException("a conexão caiu antes do fim do arquivo");
                arquivo.write(buffer, 0, lidos);
                restante -= lidos;
            }
        } catch (IOException e) {
            Files.deleteIfExists(temporario);
            throw e;
        }
        Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("OK: " + nome + " salvo em " + destino + " (" + tamanho + " bytes)");
    }

    /** Aceita caminhos arrastados para o terminal, que às vezes vêm entre aspas. */
    private static String tiraAspas(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }
}
