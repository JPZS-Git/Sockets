package servidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** As palavras que o servidor sabe (arquivo com uma palavra por linha). Escolhe uma ao acaso para cada partida. */
public class BancoPalavras {
    private final List<String> palavras = new ArrayList<>();
    private final Random sorteio = new Random();

    public BancoPalavras(Path arquivo) {
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String p = normaliza(linha);
                if (!p.isEmpty()) palavras.add(p);
            }
        } catch (IOException e) {
            System.out.println("Não consegui ler " + arquivo + ": " + e.getMessage());
        }
        if (palavras.isEmpty()) palavras.add("SOCKET");        // para o servidor nunca ficar sem palavra
    }

    public int quantidade() { return palavras.size(); }

    public synchronized String sorteia() {                      // synchronized: Random é compartilhado entre as threads
        return palavras.get(sorteio.nextInt(palavras.size()));
    }

    /** Maiúscula, sem acento, só letras A-Z ("Cão" -> "CAO"). */
    public static String normaliza(String texto) {
        String semAcento = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }
}
