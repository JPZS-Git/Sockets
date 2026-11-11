package servidor;

import comum.Dinheiro;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * O "sistema central": junta os dados de todas as filiais. É o RECURSO COMPARTILHADO deste exercício:
 * várias threads (uma por filial) usam este mesmo objeto ao mesmo tempo, então todos os métodos são
 * synchronized (monitor). O mapa de filiais e o conjunto de filiais ativas são acessados por todas as threads,
 * e o relatório lê os totais de todas as filiais enquanto elas ainda estão somando.
 */
public class Consolidado {
    private final Map<String, Totais> porFilial = new TreeMap<>();    // ordenado pelo nome da filial
    private final Set<String> ativas = new HashSet<>();                // filiais conectadas agora

    /** Começa o dia de uma filial (zera seus totais). Devolve false se já houver uma filial ativa com esse nome. */
    public synchronized boolean abre(String filial) {
        if (ativas.contains(filial)) return false;
        ativas.add(filial);
        porFilial.put(filial, new Totais());
        return true;
    }

    public synchronized void registra(String filial, boolean venda, long centavos) {
        Totais t = porFilial.get(filial);
        if (venda) {
            t.vendas += centavos;
            t.nVendas++;
        } else {
            t.compras += centavos;
            t.nCompras++;
        }
    }

    public synchronized void fecha(String filial) {
        ativas.remove(filial);
    }

    /** Uma cópia dos totais da filial (para ler fora do bloqueio sem risco). */
    public synchronized Totais totaisDe(String filial) {
        return porFilial.get(filial).copia();
    }

    public synchronized int ativas() {
        return ativas.size();
    }

    /** Tabela com todas as filiais e o total geral. */
    public synchronized String relatorio() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== RELATÓRIO CONSOLIDADO =====\n");
        sb.append(String.format("%-12s %8s %18s %18s %18s%n", "Filial", "Ocorr.", "Vendas", "Compras", "Resultado"));
        Totais geral = new Totais();
        for (Map.Entry<String, Totais> e : porFilial.entrySet()) {
            Totais t = e.getValue();
            sb.append(String.format("%-12s %8d %18s %18s %18s%n", e.getKey(), t.ocorrencias(),
                    Dinheiro.formata(t.vendas), Dinheiro.formata(t.compras), Dinheiro.formata(t.resultado())));
            geral.vendas += t.vendas;
            geral.compras += t.compras;
            geral.nVendas += t.nVendas;
            geral.nCompras += t.nCompras;
        }
        sb.append(String.format("%-12s %8d %18s %18s %18s%n", "TOTAL", geral.ocorrencias(),
                Dinheiro.formata(geral.vendas), Dinheiro.formata(geral.compras), Dinheiro.formata(geral.resultado())));
        sb.append("Filiais ainda enviando: ").append(ativas.size());
        return sb.toString();
    }
}
