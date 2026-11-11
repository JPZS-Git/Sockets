package servidor;

import comum.Protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 * Atende UM jogador: sorteia a palavra, recebe as letras e devolve o andamento do jogo.
 * Cada jogador tem o seu próprio Jogo, então partidas simultâneas não se misturam.
 */
public class Partida implements Runnable {
    private final Socket socket;
    private final BancoPalavras banco;

    public Partida(Socket socket, BancoPalavras banco) {
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

            Jogo jogo = new Jogo(banco.sorteia());
            System.out.println(quem + " nova partida, palavra: " + jogo.palavra());

            // 1) o servidor começa informando o TAMANHO da palavra
            out.writeUTF(Protocolo.INICIO + "\nBem-vindo à forca! A palavra tem " + jogo.tamanho() + " letras.\n"
                    + jogo.mascara() + "\nDigite uma letra (SAIR desiste).");

            // 2) um laço: recebe uma letra, joga, responde
            while (true) {
                String entrada = in.readUTF().trim();

                if (entrada.equalsIgnoreCase(Protocolo.SAIR)) {
                    out.writeUTF(Protocolo.DERROTA + "\nVocê desistiu. A palavra era " + jogo.palavra() + ".");
                    break;
                }

                String letraTexto = BancoPalavras.normaliza(entrada);
                if (entrada.length() != 1 || letraTexto.length() != 1) {      // só aceita UMA letra
                    out.writeUTF(Protocolo.INVALIDA + "\nDigite apenas UMA letra (A-Z).");
                    continue;
                }
                char letra = letraTexto.charAt(0);

                // monta UMA resposta por jogada (o cliente lê uma mensagem por letra enviada)
                String etiqueta;
                String texto;
                switch (jogo.joga(letra)) {
                    case REPETIDA:
                        etiqueta = Protocolo.REPETIDA;
                        texto = "Você já tentou a letra '" + letra + "'.";
                        break;
                    case ACERTO:
                        etiqueta = Protocolo.ACERTO;
                        texto = "A letra '" + letra + "' existe (" + jogo.ocorrencias(letra) + " vez(es)).\n"
                                + jogo.mascara() + "\nErros: " + jogo.erros() + "/" + Protocolo.MAX_ERROS;
                        break;
                    default:   // ERRO
                        etiqueta = Protocolo.ERRO;
                        texto = "A letra '" + letra + "' não existe. Surgiu: " + jogo.ultimaParte() + ".\n"
                                + jogo.desenho() + "\n" + jogo.mascara() + "\nErros: " + jogo.erros() + "/" + Protocolo.MAX_ERROS;
                        break;
                }

                // se a jogada encerrou o jogo, a MESMA resposta traz o fim (VITORIA ou DERROTA)
                if (jogo.venceu()) {
                    out.writeUTF(Protocolo.VITORIA + "\n" + texto + "\nParabéns! A palavra era " + jogo.palavra() + ".");
                    break;
                }
                if (jogo.perdeu()) {
                    out.writeUTF(Protocolo.DERROTA + "\n" + texto + "\nO boneco foi enforcado. A palavra era " + jogo.palavra() + ".");
                    break;
                }
                out.writeUTF(etiqueta + "\n" + texto);
            }
            System.out.println(quem + " fim da partida (erros: " + jogo.erros() + ", " + (jogo.venceu() ? "venceu" : "perdeu") + ")");
        } catch (EOFException e) {
            System.out.println(quem + " saiu no meio da partida");
        } catch (IOException e) {
            System.out.println(quem + " falhou: " + e.getMessage());
        }
        System.out.println(quem + " desconectou");
    }
}
