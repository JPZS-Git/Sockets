package cliente;

import comum.Protocolo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atalho para a simulação: sobe n filiais de uma vez (uma thread por filial) neste mesmo computador.
 *   uso: java cliente.SimulaFiliais [ip-do-servidor] [n-filiais] [ocorrencias] [atraso-ms]
 *   ex.:  java cliente.SimulaFiliais 192.168.0.15 5
 */
public class SimulaFiliais {
    public static void main(String[] args) throws InterruptedException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int n = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.FILIAIS_PADRAO;
        int ocorrencias = args.length > 2 ? Integer.parseInt(args[2]) : Protocolo.OCORRENCIAS_PADRAO;
        int atraso = args.length > 3 ? Integer.parseInt(args[3]) : Protocolo.ATRASO_PADRAO_MS;

        System.out.println("Simulando " + n + " filiais x " + ocorrencias + " ocorrências (atraso de " + atraso + " ms cada) contra " + host);
        AtomicInteger conferidas = new AtomicInteger();                      // contador seguro entre threads
        Thread[] filiais = new Thread[n];
        for (int i = 0; i < n; i++) {
            final String nome = "Filial-" + (i + 1);
            filiais[i] = new Thread(() -> {
                if (FilialSimulada.executa(nome, host, Protocolo.PORTA_PADRAO, ocorrencias, atraso)) conferidas.incrementAndGet();
            });
            filiais[i].start();
        }
        for (Thread t : filiais) t.join();                                   // espera todas terminarem
        System.out.println("Simulação encerrada: " + conferidas.get() + " de " + n + " filiais conferidas com a central.");
    }
}
