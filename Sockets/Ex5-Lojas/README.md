# Ex5 – Rede de lojas de departamento (Sockets TCP)

> "Construa um sistema que simule uma rede de lojas de departamento. Um sistema central deve receber dados de n filiais. O sistema de cada filial deverá simular a informação de compras do dia, contendo a sequência de compras e vendas efetuadas naquele dia. Faça uma simulação com, no mínimo 5 filiais, sendo que cada filial tenha uma movimentação diária de 1500 ocorrências (para que a simulação consiga ocorrer de forma mais real, use delays para simular um tempo maior para cada ocorrência)."

O **servidor** é o sistema central. Cada **filial** é um cliente que conecta, envia as suas 1500 ocorrências do dia (vendas e compras, uma a uma, com um atraso entre elas) e, no final, confere se o resumo da central bate com os totais que ela mesma calculou.

## Estrutura de pastas

```
Ex5-Lojas/
├── src/
│   ├── comum/Protocolo.java            comandos, porta e valores padrão (1500 ocorrências, 10 ms, 5 filiais)
│   ├── comum/Dinheiro.java             formata centavos como "R$ 1234,56"
│   ├── servidor/Servidor.java          ServerSocket; uma thread por filial
│   ├── servidor/AtendeFilial.java      atende UMA filial: recebe as ocorrências e soma no Consolidado
│   ├── servidor/Consolidado.java       o sistema central: totais de todas as filiais (synchronized)
│   ├── servidor/Totais.java            totais de uma filial (vendas, compras, quantidades)
│   ├── cliente/FilialSimulada.java     o dia de UMA filial (simula, envia e confere)
│   ├── cliente/Filial.java             roda UMA filial (um programa por filial, pode ser em outro computador)
│   └── cliente/SimulaFiliais.java      atalho: sobe n filiais de uma vez neste computador
├── scripts/                            compilar / servidor / filial / simulador (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

## Protocolo

Mensagens de texto (`writeUTF`/`readUTF`). Os valores trafegam em **centavos** (inteiros).

| Quem envia | Mensagem | Resposta |
|---|---|---|
| filial | `FILIAL <nome>` | `OK: filial <nome> registrada` (ou `ERRO:` se o nome já estiver em uso) |
| filial | `VENDA <centavos>` | nenhuma (uma mensagem por ocorrência) |
| filial | `COMPRA <centavos>` | nenhuma |
| filial | `FIM <quantidade>` | `RESUMO <ocorrências> <vendas> <compras> <nVendas> <nCompras>` (ou `ERRO:` se a contagem não bater) |

`VENDA` é a loja vendendo a um cliente (entra dinheiro). `COMPRA` é a loja comprando mercadoria (sai dinheiro). O **resultado** do relatório é vendas menos compras.

## Como executar

Precisa do JDK 8 ou superior. Os scripts mudam para a pasta do projeto sozinhos.

**Windows**
```
scripts\compilar.bat
scripts\servidor.bat                    (um terminal: o sistema central)
scripts\simulador.bat                   (outro terminal: sobe 5 filiais de uma vez)
```
**Linux / Mac**
```
sh scripts/compilar.sh
sh scripts/servidor.sh
sh scripts/simulador.sh
```

Com os valores padrão, cada filial envia 1500 ocorrências com 10 ms entre elas, então a simulação leva cerca de 15 segundos. O servidor mostra o andamento (a cada 500 ocorrências) e, quando cada filial termina, imprime o **relatório consolidado**:

```
===== RELATÓRIO CONSOLIDADO =====
Filial         Ocorr.             Vendas            Compras          Resultado
Filial-1         1500       R$ 231714,02       R$ 146995,70        R$ 84718,32
...
TOTAL            7500      R$ 1151097,48       R$ 758289,50       R$ 392807,98
Filiais ainda enviando: 0
```

Cada filial também imprime `conferido: a central e a filial concordam`, depois de comparar o resumo da central com os seus próprios totais.

### Opções

- Mais filiais ou outro volume: `simulador.bat <ip> <n-filiais> <ocorrencias> <atraso-ms>`, por exemplo `simulador.bat 127.0.0.1 8 1500 5`.
- Uma filial só: `filial.bat <nome> [ip] [porta] [ocorrencias] [atraso-ms]`, por exemplo `filial.bat Centro 192.168.0.15`.
- Porta diferente no servidor: `servidor.bat 8000`, e nas filiais `filial.bat Centro 127.0.0.1 8000`. O `simulador` usa sempre a porta 7000.

## Testando em computadores diferentes

A central roda no computador A (ele mostra os IPs da máquina) e as filiais rodam no B. Duas formas:

1. **Simulador no B:** `simulador.bat <IP-do-A>` sobe as 5 filiais no computador B.
2. **Uma filial por computador (mais realista):** em cada computador, `filial.bat <nome> <IP-do-A>`, com nomes diferentes (`Centro`, `Norte`, `Sul`...). Elas podem rodar ao mesmo tempo.

A porta 7000 precisa estar liberada no firewall do A (a regra `netsh advfirewall firewall add rule name="Fortunes" dir=in action=allow protocol=TCP localport=7000` dos exercícios anteriores já vale). O servidor mostra o IP de cada filial que conecta.

## Decisões de projeto

- **n filiais ao mesmo tempo:** uma thread por conexão (`AtendeFilial`), como nos exercícios 1 e 4. Todas usam o mesmo objeto `Consolidado`.
- **Exclusão mútua:** todos os métodos de `Consolidado` são `synchronized` (monitor). O mapa de filiais e o conjunto de filiais ativas são acessados por todas as threads, e o relatório lê os totais de todas as filiais enquanto elas ainda estão somando.
- **Sem resposta a cada ocorrência:** a filial envia as ocorrências em sequência, sem esperar resposta de cada uma (o atraso entre elas é o `Thread.sleep` da simulação). O controle de integridade fica no fim: com `FIM <quantidade>` a central confere se recebeu a quantidade declarada, e o `RESUMO` devolve os totais para a filial conferir.
- **Dinheiro em centavos:** valores inteiros (`long`) somam sempre de forma exata, sem os erros de arredondamento do `double`.
- **Nome da filial único:** duas filiais com o mesmo nome ao mesmo tempo seriam misturadas, então a central recusa a segunda. Quando uma filial termina (ou cai), o nome fica livre, e uma nova conexão com esse nome começa um dia novo, com os totais zerados. Assim dá para repetir a simulação sem reiniciar o servidor.
- **Filial que cai no meio do dia:** a central registra o fato no log e libera o nome. Os totais parciais ficam no relatório até o próximo dia dessa filial.
- **Linhas fora do protocolo:** valores inválidos ou comandos desconhecidos são ignorados e contados. O `FIM` responde `ERRO` se alguma linha foi ignorada ou se a contagem não bate.
- **Caracteres acentuados:** os arquivos estão em UTF-8. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
