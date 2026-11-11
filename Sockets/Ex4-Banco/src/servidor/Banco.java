package servidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O conjunto de contas do banco, lido de um arquivo ("numero;titular;saldo-em-centavos" por linha).
 * A lista de contas é montada ao iniciar e não muda depois; o que muda é o saldo, que cada Conta protege.
 * Depois de cada depósito ou saque o arquivo é regravado, então os saldos sobrevivem a reiniciar o servidor.
 */
public class Banco {
    private final Map<String, Conta> contas = new LinkedHashMap<>();
    private final Path arquivo;

    public Banco(Path arquivo) {
        this.arquivo = arquivo;
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String[] partes = linha.split(";");
                if (partes.length != 3) continue;
                contas.put(partes[0].trim(), new Conta(partes[0].trim(), partes[1].trim(), Long.parseLong(partes[2].trim())));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Problema ao ler " + arquivo + ": " + e.getMessage());
        }
    }

    public int quantidade() { return contas.size(); }

    /** Devolve a conta ou null se não existir. */
    public Conta busca(String numero) {
        return contas.get(numero);
    }

    /**
     * Regrava o arquivo com os saldos atuais. synchronized: só uma thread grava por vez.
     * (Cada thread chama isto DEPOIS de terminar sua operação na Conta, então nunca segura dois bloqueios ao mesmo tempo.)
     */
    public synchronized void salvar() {
        List<String> linhas = new ArrayList<>();
        for (Conta c : contas.values()) {
            linhas.add(c.numero() + ";" + c.titular() + ";" + c.saldo());
        }
        try {
            Path temp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");     // grava em arquivo temporário e troca,
            Files.write(temp, linhas, StandardCharsets.UTF_8);                       // para nunca deixar o arquivo pela metade
            Files.move(temp, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Não consegui gravar " + arquivo + ": " + e.getMessage());
        }
    }
}
