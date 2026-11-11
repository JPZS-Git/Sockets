package comum;

import java.math.BigDecimal;

/**
 * Valores em dinheiro. Guardamos tudo em CENTAVOS (long), para nunca ter erro de arredondamento
 * (com double, 0.1 + 0.2 não dá 0.3).
 */
public final class Dinheiro {
    public static final long MAX_OPERACAO = 100_000_000_000L;   // R$ 1.000.000.000,00 por operação

    private Dinheiro() { }

    /** "100", "100.5" ou "100,50" -> 10000, 10050, 10050 (centavos). Lança IllegalArgumentException se for inválido. */
    public static long paraCentavos(String texto) {
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido: '" + texto + "'");
        }
        if (valor.signum() <= 0) throw new IllegalArgumentException("o valor deve ser maior que zero");
        if (valor.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("use no máximo 2 casas decimais");
        BigDecimal centavos = valor.movePointRight(2);
        if (centavos.compareTo(BigDecimal.valueOf(MAX_OPERACAO)) > 0) throw new IllegalArgumentException("valor grande demais");
        return centavos.longValueExact();
    }

    /** 10050 -> "R$ 100,50" */
    public static String formata(long centavos) {
        String sinal = centavos < 0 ? "-" : "";
        long abs = Math.abs(centavos);
        return sinal + String.format("R$ %d,%02d", abs / 100, abs % 100);
    }
}
