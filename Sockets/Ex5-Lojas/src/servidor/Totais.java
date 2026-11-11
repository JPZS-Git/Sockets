package servidor;

/** Os totais de UMA filial. Quem protege o acesso concorrente é o Consolidado (ele é synchronized). */
public class Totais {
    long vendas = 0;       // soma das vendas, em centavos
    long compras = 0;      // soma das compras, em centavos
    int nVendas = 0;
    int nCompras = 0;

    int ocorrencias()  { return nVendas + nCompras; }
    long resultado()   { return vendas - compras; }

    Totais copia() {
        Totais t = new Totais();
        t.vendas = vendas;
        t.compras = compras;
        t.nVendas = nVendas;
        t.nCompras = nCompras;
        return t;
    }
}
