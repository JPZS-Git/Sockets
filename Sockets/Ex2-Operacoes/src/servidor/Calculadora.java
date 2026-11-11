package servidor;

import comum.Protocolo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A "regra de negócio" do servidor: recebe as linhas que o cliente enviou
 * (inteiros... e, na última linha, a operação) e devolve o texto da resposta.
 * Não mexe com socket nenhum: só recebe texto e devolve texto.
 */
public final class Calculadora {

    private Calculadora() { }

    /** @param linhas todas as linhas recebidas até o EOF (não vazia); a última é a operação. */
    public static String calcula(List<String> linhas) {
        String operacaoOriginal = linhas.get(linhas.size() - 1);
        String operacao = operacaoOriginal.toUpperCase(Locale.ROOT);

        if (!operacaoValida(operacao)) {
            return "ERRO: operação desconhecida '" + operacaoOriginal + "'. Use: " + Protocolo.LISTA_OPERACOES;
        }
        if (linhas.size() == 1) {
            return "ERRO: nenhum inteiro foi enviado antes da operação";
        }

        // converte as linhas anteriores à operação em inteiros
        List<Integer> numeros = new ArrayList<>();
        for (int i = 0; i < linhas.size() - 1; i++) {
            try {
                numeros.add(Integer.parseInt(linhas.get(i)));
            } catch (NumberFormatException e) {
                return "ERRO: '" + linhas.get(i) + "' não é um inteiro válido";
            }
        }

        try {
            String resultado;
            switch (operacao) {
                case Protocolo.SOMA:    resultado = String.valueOf(soma(numeros)); break;
                case Protocolo.PRODUTO: resultado = String.valueOf(produto(numeros)); break;
                case Protocolo.MEDIA:   resultado = String.format(Locale.ROOT, "%.2f", media(numeros)); break;
                case Protocolo.MAIOR:   resultado = String.valueOf(maior(numeros)); break;
                default:                resultado = String.valueOf(menor(numeros)); break;   // MENOR
            }
            return "RESULTADO: " + resultado + " (" + operacao + " de " + numeros.size() + " número(s))";
        } catch (ArithmeticException e) {
            return "ERRO: o resultado é grande demais (estouro de capacidade)";
        }
    }

    private static boolean operacaoValida(String op) {
        return op.equals(Protocolo.SOMA) || op.equals(Protocolo.PRODUTO) || op.equals(Protocolo.MEDIA)
                || op.equals(Protocolo.MAIOR) || op.equals(Protocolo.MENOR);
    }

    // long dá folga para somar/multiplicar vários int; addExact/multiplyExact avisam se mesmo o long estourar
    private static long soma(List<Integer> n) {
        long total = 0;
        for (int x : n) total = Math.addExact(total, x);
        return total;
    }

    private static long produto(List<Integer> n) {
        long total = 1;
        for (int x : n) total = Math.multiplyExact(total, x);
        return total;
    }

    private static double media(List<Integer> n) {
        return (double) soma(n) / n.size();
    }

    private static int maior(List<Integer> n) {
        int m = n.get(0);
        for (int x : n) if (x > m) m = x;
        return m;
    }

    private static int menor(List<Integer> n) {
        int m = n.get(0);
        for (int x : n) if (x < m) m = x;
        return m;
    }
}
