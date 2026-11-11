package comum;

/** Valores em dinheiro: tudo é guardado em CENTAVOS (long), então a soma nunca tem erro de arredondamento. */
public final class Dinheiro {
    private Dinheiro() { }

    /** 1234567 -> "R$ 12345,67" (também funciona para negativos: -500 -> "-R$ 5,00") */
    public static String formata(long centavos) {
        String sinal = centavos < 0 ? "-" : "";
        long abs = Math.abs(centavos);
        return sinal + String.format("R$ %d,%02d", abs / 100, abs % 100);
    }
}
