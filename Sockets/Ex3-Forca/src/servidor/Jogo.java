package servidor;

import comum.Protocolo;

import java.util.HashSet;
import java.util.Set;

/**
 * Uma partida da forca: guarda a palavra, as letras já tentadas e os erros.
 * Não usa socket: só a regra do jogo (por isso é fácil de testar e de explicar).
 */
public class Jogo {

    /** Resultado de uma jogada. */
    public enum Resultado { ACERTO, ERRO, REPETIDA }

    // as 6 partes do boneco, na ordem em que aparecem a cada erro
    private static final String[] PARTES = {
            "cabeça", "tronco", "braço esquerdo", "braço direito", "perna esquerda", "perna direita"
    };

    private final String palavra;                         // sempre MAIÚSCULA e sem acento
    private final Set<Character> tentadas = new HashSet<>();
    private int erros = 0;

    public Jogo(String palavra) {
        this.palavra = palavra;
    }

    public int tamanho()            { return palavra.length(); }
    public int erros()              { return erros; }
    public String palavra()         { return palavra; }
    public boolean perdeu()         { return erros >= Protocolo.MAX_ERROS; }
    public boolean venceu()         { return mascara().indexOf('_') < 0; }
    public boolean terminou()       { return venceu() || perdeu(); }

    /** Parte do boneco que surgiu no último erro (ex.: "cabeça"). */
    public String ultimaParte()     { return PARTES[erros - 1]; }

    /** Quantas vezes a letra aparece na palavra. */
    public int ocorrencias(char letra) {
        int n = 0;
        for (char c : palavra.toCharArray()) if (c == letra) n++;
        return n;
    }

    /** Faz uma jogada (a letra já vem normalizada: A-Z maiúscula). */
    public Resultado joga(char letra) {
        if (!tentadas.add(letra)) return Resultado.REPETIDA;
        if (palavra.indexOf(letra) >= 0) return Resultado.ACERTO;
        erros++;
        return Resultado.ERRO;
    }

    /** A palavra com as letras descobertas e "_" nas outras: "_ A S A". */
    public String mascara() {
        StringBuilder sb = new StringBuilder();
        for (char c : palavra.toCharArray()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(tentadas.contains(c) ? c : '_');
        }
        return sb.toString();
    }

    /** Desenho em texto da forca com as partes do boneco que já surgiram. */
    public String desenho() {
        char cabeca = erros >= 1 ? 'O' : ' ';
        char tronco = erros >= 2 ? '|' : ' ';
        char bracoE = erros >= 3 ? '/' : ' ';
        char bracoD = erros >= 4 ? '\\' : ' ';
        char pernaE = erros >= 5 ? '/' : ' ';
        char pernaD = erros >= 6 ? '\\' : ' ';
        return "  +---+\n"
             + "  |   |\n"
             + "  " + cabeca + "   |\n"
             + " " + bracoE + tronco + bracoD + "  |\n"
             + " " + pernaE + " " + pernaD + "  |\n"
             + "      |\n"
             + "=========";
    }
}
