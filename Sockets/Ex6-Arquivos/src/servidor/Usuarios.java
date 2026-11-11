package servidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Os usuários do servidor (arquivo com "usuario;senha" por linha).
 * As senhas ficam em texto puro: simplificação didática. Um servidor de verdade guardaria só um hash (ex.: bcrypt).
 */
public class Usuarios {
    private final Map<String, String> senhas = new HashMap<>();

    public Usuarios(Path arquivo) {
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String[] p = linha.split(";", 2);
                if (p.length == 2 && p[0].trim().matches("[A-Za-z0-9_]+")) senhas.put(p[0].trim(), p[1].trim());
            }
        } catch (IOException e) {
            System.out.println("Não consegui ler " + arquivo + ": " + e.getMessage());
        }
    }

    public int quantidade() { return senhas.size(); }

    public boolean confere(String usuario, String senha) {
        String certa = senhas.get(usuario);
        return certa != null && certa.equals(senha);
    }
}
