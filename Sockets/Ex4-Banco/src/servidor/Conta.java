package servidor;

/**
 * Uma conta bancária. É o RECURSO COMPARTILHADO deste exercício: várias threads (vários clientes,
 * até do mesmo titular) mexem no mesmo saldo ao mesmo tempo.
 *
 * Todos os métodos são synchronized: só UMA thread por vez executa qualquer um deles em uma mesma conta (monitor).
 * Sem isso, dois depósitos simultâneos poderiam ler o mesmo saldo e um deles se perderia (lost update),
 * e dois saques simultâneos poderiam passar na verificação "tem saldo?" ao mesmo tempo (check-then-act).
 */
public class Conta {
    private final String numero;
    private final String titular;
    private long saldo;            // em centavos

    public Conta(String numero, String titular, long saldoInicial) {
        this.numero = numero;
        this.titular = titular;
        this.saldo = saldoInicial;
    }

    public String numero()  { return numero; }
    public String titular() { return titular; }

    public synchronized long saldo() {
        return saldo;
    }

    /** Soma o valor e devolve o saldo novo. */
    public synchronized long deposita(long valor) {
        saldo = saldo + valor;
        return saldo;
    }

    /** Subtrai o valor e devolve o saldo novo; devolve -1 (sem alterar nada) se o saldo não for suficiente. */
    public synchronized long saca(long valor) {
        if (valor > saldo) return -1;      // a verificação e a subtração acontecem juntas, sem ninguém no meio
        saldo = saldo - valor;
        return saldo;
    }
}
