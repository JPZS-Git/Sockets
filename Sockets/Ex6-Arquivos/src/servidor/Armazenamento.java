package servidor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Onde ficam os arquivos: uma pasta por usuário, dentro da pasta base. Cada usuário só enxerga a própria pasta.
 * Também decide se o nome de arquivo enviado pelo cliente é aceitável (para ninguém sair da própria pasta com "../").
 */
public class Armazenamento {
    private final Path base;

    public Armazenamento(Path base) {
        this.base = base.toAbsolutePath().normalize();
    }

    public Path base() { return base; }

    /** A pasta do usuário (criada se ainda não existir). */
    public Path pastaDe(String usuario) throws IOException {
        Path pasta = base.resolve(usuario).normalize();
        Files.createDirectories(pasta);
        return pasta;
    }

    /**
     * Nome aceito: só letras, números, espaço, ponto, "_", "-", "(" e ")"; até 100 caracteres;
     * não pode começar com ponto (arquivos temporários) nem terminar com ponto ou espaço.
     * Barras, ".." e outros caracteres especiais ficam de fora, então o nome nunca aponta para outra pasta.
     */
    public static boolean nomeValido(String nome) {
        return nome != null
                && !nome.isEmpty()
                && nome.length() <= 100
                && nome.matches("[\\p{L}\\p{N} ._()\\-]+")
                && !nome.startsWith(".")
                && !nome.endsWith(".")
                && !nome.endsWith(" ");
    }

    /** O caminho do arquivo do usuário. Confere de novo se continua dentro da pasta do usuário (defesa extra). */
    public Path caminho(String usuario, String nome) throws IOException {
        Path pasta = pastaDe(usuario);
        Path arquivo = pasta.resolve(nome).normalize();
        if (!arquivo.startsWith(pasta)) throw new IOException("caminho fora da pasta do usuário");
        return arquivo;
    }
}
