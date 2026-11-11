# Sockets – Lista de exercícios (CC57E)

Sete exercícios em Java (JDK 8 ou superior), um projeto por pasta. Cada pasta tem o seu próprio `README.md` com o protocolo, como executar, como testar em dois computadores e as decisões de projeto.

| # | Exercício | Pasta | Tipo | Servidor atende | Como se demonstra |
|---|---|---|---|---|---|
| 1 | Servidor de Fortunes | `Ex1-Fortunes/` | TCP | vários clientes (uma thread por cliente) | `GET/ADD/UPD/LST-FORTUNE` de dois computadores |
| 2 | Operações sobre inteiros | `Ex2-Operacoes/` | TCP | **um cliente de cada vez** | inteiros + operação, o EOF dispara a conta |
| 3 | Jogo da forca remoto | `Ex3-Forca/` | TCP | vários jogadores (uma partida por conexão) | partida de 1 jogador, boneco em texto |
| 4 | Banco com n clientes | `Ex4-Banco/` | TCP | vários clientes na mesma conta | dois terminais na mesma conta, sem conflito |
| 5 | Rede de lojas | `Ex5-Lojas/` | TCP | n filiais enviando ao mesmo tempo | 5 filiais × 1500 ocorrências, com delay |
| 6 | Servidor de arquivos | `Ex6-Arquivos/` | TCP | vários usuários | login, upload e download (bytes) |
| 7 | Chat com multicast | `Ex7-ChatMulticast/` | UDP multicast | sem servidor | vários terminais no grupo `226.1.1.1` |

## Organização de cada projeto

```
ExN-Nome/
├── src/                 código-fonte, em pacotes: comum (protocolo), servidor e cliente
├── dados/               arquivos que o servidor lê (frases, contas, usuários...), quando existem
├── scripts/             compilar / servidor / cliente: .bat (Windows) e .sh (Linux/Mac)
└── README.md
```

## Executando qualquer exercício

Em cada projeto, compile uma vez e depois abra um terminal para o servidor e outro para o cliente:

```
scripts\compilar.bat                 (Linux/Mac: sh scripts/compilar.sh)
scripts\servidor.bat
scripts\cliente.bat
```

No Ex5 o cliente é `filial.bat` ou `simulador.bat`, e no Ex7 não há servidor: é só `chat.bat`.

## Dois computadores

1. No computador do servidor, descubra o IP (`ipconfig`; o servidor também lista os IPs ao iniciar).
2. No computador do cliente, rode o cliente e digite esse IP quando ele perguntar.
3. Libere a porta no firewall do servidor, num terminal como administrador:
   ```
   netsh advfirewall firewall add rule name="Fortunes" dir=in action=allow protocol=TCP localport=7000
   ```
   A porta **TCP 7000** vale para os exercícios 1 a 6. O Ex7 usa a **UDP 6789**, em todos os computadores.
4. Teste a conexão antes, no computador do cliente: `Test-NetConnection <IP> -Port 7000`. Precisa mostrar `TcpTestSucceeded : True`.
5. Os dois computadores precisam estar na mesma rede (ou na mesma VPN, como o Radmin). Redes Wi-Fi de instituições costumam isolar os clientes entre si e bloquear multicast; o hotspot do celular é a alternativa.

## Observações

- Os exercícios 1 a 6 usam a mesma porta padrão (7000). Rode **um servidor por vez** ou passe outra porta: `servidor.bat 8000` e `cliente.bat 8000`.
- Os arquivos estão em UTF-8. Os scripts do Windows já configuram o terminal (`chcp 65001`) e o Java (`-Dfile.encoding=UTF-8`).
- Os scripts mudam para a pasta do projeto sozinhos, então podem ser chamados de qualquer lugar.
