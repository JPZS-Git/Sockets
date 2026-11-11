# Ex7 – Chat simples com multicast (Sockets UDP/Multicast)

> "Crie um chat simples utilizando multicast."

Cada participante roda uma cópia do programa e escolhe um apelido. Tudo o que alguém digita é enviado ao **grupo multicast** `226.1.1.1` e a rede entrega a mensagem a todos que estão no grupo. **Não existe servidor.**

## Estrutura de pastas

```
Ex7-ChatMulticast/
├── src/
│   ├── comum/Protocolo.java      grupo, porta e tamanho máximo da mensagem
│   └── cliente/Chat.java         o chat (envia o que você digita e mostra o que chega)
├── scripts/                      compilar / chat (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

Não há pasta `servidor`: o grupo multicast faz o papel dele.

## Como o multicast funciona (resumo)

- Um **grupo** é um endereço IP de classe D (224.0.0.0 a 239.255.255.255). Aqui, `226.1.1.1`, o mesmo do exemplo da aula.
- Quem quer receber as mensagens do grupo **entra** nele (`joinGroup`) e escuta em uma porta (`6789`). Quem quer falar **envia um datagrama** para o endereço do grupo (`send`).
- É UDP: uma mensagem enviada é entregue a todos os membros de uma só vez, sem conexão. Não há garantia de entrega nem de ordem, e o grupo é aberto (quem entra não precisa de autorização).

## Como executar

Precisa do JDK 8 ou superior. Abra **um terminal por participante** (no mesmo computador ou em computadores diferentes).

**Windows**
```
scripts\compilar.bat
scripts\chat.bat
```
**Linux / Mac**
```
sh scripts/compilar.sh
sh scripts/chat.sh
```

O programa pergunta o apelido e, se o computador tiver mais de uma placa de rede, **qual placa usar**:

```
Seu apelido: Ana
  1) 10.51.2.183   Intel(R) Wi-Fi 6E AX211 160MHz
  2) 26.195.179.93   Famatech Radmin VPN Ethernet Adapter
Interface de rede [1]: 1
Você entrou no grupo 226.1.1.1:6789 pela interface Intel(R) Wi-Fi 6E AX211 160MHz
Digite suas mensagens. /sair encerra.
[15:30:01] ** Ana entrou **   (de 10.51.2.183)
oi, pessoal
[15:30:05] Ana: oi, pessoal   (de 10.51.2.183)
[15:30:09] Bruno: oi, Ana!   (de 10.51.2.190)
```

Você também vê as suas próprias mensagens, porque elas voltam pelo grupo como as de todo mundo. Depois do horário, o texto `(de ...)` mostra o IP de quem enviou, o que comprova de qual computador a mensagem veio. `/sair` encerra.

Para pular a pergunta, passe o IP da placa: `chat.bat 10.51.2.183`.

## Testando em computadores diferentes

1. Os computadores precisam estar **na mesma rede local** (o TTL padrão do Java é 1, então o multicast não passa por roteadores).
2. Em cada um, rode o `chat.bat` e escolha a placa que está nessa rede (a do Wi-Fi ou cabo comum, e não a do VirtualBox, WSL ou outras placas virtuais).
3. Libere a porta UDP no firewall de **todos** os computadores (aqui todos recebem, e não só um), num terminal como administrador:
   ```
   netsh advfirewall firewall add rule name="ChatMulticast" dir=in action=allow protocol=UDP localport=6789
   ```
   A regra dos exercícios anteriores era TCP 7000 e não vale para este.
4. Digite uma mensagem em um computador: ela deve aparecer nos dois, com o IP de origem.

**Se os participantes não se enxergam**, confira nesta ordem:

- Eles escolheram a placa de rede certa? (É o motivo mais comum quando há várias placas.)
- O firewall do outro computador liberou a UDP 6789?
- A rede deixa passar multicast? Muitas redes Wi-Fi de instituições (como a do campus) bloqueiam multicast entre os clientes. Nesse caso, use o hotspot do celular ou um cabo e um roteador comum.
- Uma VPN como o Radmin pode ou não repassar multicast, dependendo da configuração. Teste com as duas máquinas na mesma VPN antes de contar com ela na apresentação.

Para ver o chat funcionando com certeza, abra dois ou mais terminais **no mesmo computador**: o multicast funciona entre processos da mesma máquina.

## Decisões de projeto

- **Sem servidor:** é a característica do multicast. A rede replica cada datagrama para os membros do grupo.
- **Duas threads:** a principal lê o teclado e envia; uma segunda fica em `receive()` (que bloqueia) e imprime o que chega. Sem a segunda thread, você só receberia mensagens depois de digitar alguma coisa.
- **Escolha explícita da placa de rede:** um computador com várias placas pode entrar no grupo pela errada, e aí as mensagens não chegam aos outros. `setNetworkInterface` define por onde as mensagens saem e `joinGroup(grupo, placa)` define por onde elas entram.
- **`-Djava.net.preferIPv4Stack=true`:** os scripts forçam IPv4, porque o multicast em Java pode falhar no Windows quando a pilha IPv6 é a preferida.
- **Mensagem pronta no datagrama:** o texto enviado já é o que será exibido (`Apelido: texto`), o que mantém o protocolo trivial. O limite é de 1000 bytes por mensagem; acima disso o programa avisa e não envia.
- **Entrada e saída:** ao entrar e ao sair, o programa envia um aviso ao grupo (`** Ana entrou **`). Como o grupo é aberto e não informa quem são os membros, esses avisos são a única forma de saber quem está presente.
- **Sem garantias (UDP):** uma mensagem pode se perder ou chegar fora de ordem, e quem entra depois não recebe o que foi dito antes. Para um chat simples, isso é aceitável.
- **Caracteres acentuados:** os arquivos estão em UTF-8 e as mensagens também. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
