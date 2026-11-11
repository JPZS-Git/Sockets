package cliente;

import comum.Protocolo;

/**
 * UMA filial (um programa por filial; pode rodar em outro computador).
 *   uso: java cliente.Filial <nome> [ip-do-servidor] [porta] [ocorrencias] [atraso-ms]
 *   ex.:  java cliente.Filial Centro 192.168.0.15
 */
public class Filial {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("uso: Filial <nome> [ip-do-servidor] [porta] [ocorrencias] [atraso-ms]");
            return;
        }
        String nome = args[0];
        String host = args.length > 1 ? args[1] : "127.0.0.1";
        int porta = args.length > 2 ? Integer.parseInt(args[2]) : Protocolo.PORTA_PADRAO;
        int ocorrencias = args.length > 3 ? Integer.parseInt(args[3]) : Protocolo.OCORRENCIAS_PADRAO;
        int atraso = args.length > 4 ? Integer.parseInt(args[4]) : Protocolo.ATRASO_PADRAO_MS;

        FilialSimulada.executa(nome, host, porta, ocorrencias, atraso);
    }
}
