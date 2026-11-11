package servidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Banco de frases do servidor. É o RECURSO COMPARTILHADO: várias threads (uma por cliente) usam o mesmo objeto.
 * Por isso todos os métodos são synchronized (monitor): só um cliente por vez mexe na lista.
 * As frases ficam no arquivo de texto (uma por linha) e o arquivo é regravado a cada alteração.
 */
class BancoFortunes {
    private final List<String> frases = new ArrayList<>();
    private final Random sorteio = new Random();
    private final Path arquivo;

    BancoFortunes(Path arquivo) {
        this.arquivo = arquivo;
        carrega();
    }

    /** Sorteia uma frase. Devolve null se o banco está vazio. */
    synchronized String sorteia() {
        if (frases.isEmpty()) return null;
        return frases.get(sorteio.nextInt(frases.size()));
    }

    /** Adiciona no fim e devolve a posição (começando em 1). */
    synchronized int adiciona(String frase) {
        frases.add(frase);
        salva();
        return frases.size();
    }

    /** Troca a frase da posição pos (1..quantidade). Devolve false se a posição não existe. */
    synchronized boolean atualiza(int pos, String nova) {
        if (pos < 1 || pos > frases.size()) return false;
        frases.set(pos - 1, nova);
        salva();
        return true;
    }

    /** Devolve uma CÓPIA da lista, para quem chamou poder percorrer sem segurar o monitor. */
    synchronized List<String> lista() {
        return new ArrayList<>(frases);
    }

    synchronized int quantidade() {
        return frases.size();
    }

    private void carrega() {
        try {
            if (!Files.exists(arquivo)) return;
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (!linha.trim().isEmpty()) frases.add(linha.trim());
            }
        } catch (IOException e) {
            System.out.println("Aviso: não consegui ler " + arquivo + " (" + e.getMessage() + "). Começando vazio.");
        }
    }

    private void salva() {   // sempre chamado de dentro de um método synchronized
        try {
            if (arquivo.getParent() != null) Files.createDirectories(arquivo.getParent());
            Files.write(arquivo, frases, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Aviso: não consegui gravar " + arquivo + " (" + e.getMessage() + ").");
        }
    }
}
