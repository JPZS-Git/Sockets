# Ex2 – Servidor de operações sobre inteiros (Sockets TCP)

> "Implemente um servidor que aceite a ligação de um cliente de cada vez. O servidor receberá de cada cliente uma sequência de inteiros (pode optar tanto pelo formato binário como de texto) terminada com uma operação e, quando detectar a situação de end of file na stream de leitura do socket, inicia a operação no servidor. O resultado do servidor é o resultado da operação."

O cliente envia vários inteiros e, por último, o nome de uma operação. Quando termina, ele avisa o servidor fechando **só o lado de escrita** do socket. O servidor lê isso como fim de arquivo (EOF), faz a conta e devolve o resultado.

O servidor atende **um cliente por vez**: enquanto há um cliente conectado, qualquer outro que tente conectar é **recusado na hora**, com a mensagem "servidor ocupado".

## Estrutura de pastas

```
Ex2-Operacoes/
├── src/
│   ├── comum/Protocolo.java          operações, porta padrão e limites (usados pelos dois lados)
│   ├── servidor/Servidor.java        ServerSocket; atende UM cliente por vez e recusa os outros; lê até o EOF
│   ├── servidor/Calculadora.java     a conta em si (recebe texto, devolve texto; não usa socket)
│   └── cliente/Cliente.java          cliente de console
├── scripts/                          compilar / servidor / cliente (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

## Protocolo (formato de texto)

Uma ligação é uma conta. Ao conectar, o cliente lê a **primeira linha** do servidor, que diz se ele vai ser atendido:

| Primeira linha do servidor | Significa |
|---|---|
| `PRONTO: servidor livre, envie os inteiros e a operação` | o servidor atende este cliente |
| `ERRO: servidor ocupado atendendo outro cliente. Tente novamente mais tarde.` | já há outro cliente conectado; este é recusado e a ligação é fechada |

Depois do `PRONTO`, o cliente envia **uma linha por item**: os inteiros e, na última linha, a operação.

```
servidor envia:    PRONTO: ...
cliente envia:     10 ⏎   20 ⏎   30 ⏎   SOMA ⏎   <EOF: shutdownOutput>
servidor responde: RESULTADO: 60 (SOMA de 3 número(s))
```

| Operação | O que faz |
|---|---|
| `SOMA` | soma de todos os inteiros |
| `PRODUTO` | produto de todos os inteiros |
| `MEDIA` | média, com 2 casas decimais |
| `MAIOR` | maior inteiro |
| `MENOR` | menor inteiro |

A operação pode ser escrita em maiúsculas ou minúsculas. Erros começam com `ERRO:` (operação desconhecida, valor que não é inteiro, nenhum inteiro antes da operação, resultado grande demais, tempo esgotado).

## Como executar

Precisa do JDK 8 ou superior. Os scripts mudam para a pasta do projeto sozinhos, então podem ser chamados de qualquer lugar.

**Windows**
```
scripts\compilar.bat
scripts\servidor.bat            (um terminal)
scripts\cliente.bat             (outro terminal)
```
**Linux / Mac**
```
sh scripts/compilar.sh
sh scripts/servidor.sh
sh scripts/cliente.sh
```
Porta diferente: `servidor.bat 8000` e `cliente.bat 8000` (a padrão é 7000).
Tempo limite diferente (em segundos): `servidor.bat 7000 30`.

Exemplo de sessão do cliente:
```
IP do servidor [127.0.0.1]:
Conectado. Digite inteiros e termine com a operação (SOMA, PRODUTO, MEDIA, MAIOR, MENOR).
Exemplo: 10 20 30 SOMA   (pode ser em várias linhas)
> 10 20
> 30 SOMA
(fim dos dados enviado; esperando o servidor...)
Servidor: RESULTADO: 60 (SOMA de 3 número(s))

Fazer outra conta? (s/n) n
```

## Testando em computadores diferentes

Igual ao Ex1: rode o servidor no computador A (ele mostra os IPs da máquina) e, no computador B, rode o cliente e digite o IP do A. Os dois precisam estar na mesma rede (ou na mesma VPN, como o Radmin), e o firewall do A precisa liberar a porta:
```
netsh advfirewall firewall add rule name="Operacoes" dir=in action=allow protocol=TCP localport=7000
```
(num terminal como administrador). Se o Ex1 estiver liberado na mesma porta 7000, a regra dele já vale.
O servidor mostra o IP de cada cliente que conecta.

## Demonstrando "um cliente de cada vez"

1. Abra o cliente A e deixe-o parado depois de `Conectado.` (sem digitar nada).
2. Abra o cliente B. Ele mostra na hora `Servidor: ERRO: servidor ocupado atendendo outro cliente...` e termina. O mesmo acontece com um cliente C, D...
3. Volte ao A e digite `5 5 SOMA`. O A recebe o resultado normalmente.
4. Depois que o A terminou, abra o B de novo: agora ele é aceito.

O log do servidor mostra `recusado (servidor ocupado)` para cada tentativa durante o atendimento do A. Se o A ficar parado por mais tempo que o limite (120 s por padrão), o servidor desiste dele com `ERRO: tempo esgotado` e fica livre de novo.

## Decisões de projeto

- **Um cliente por vez, os outros recusados:** a thread principal só faz `accept()` e consulta um indicador `ocupado` (um `AtomicBoolean`). Se o servidor está livre, ela marca `ocupado` e entrega o cliente a uma thread que o atende. Se já está ocupado, ela responde "servidor ocupado", fecha a ligação e volta ao `accept()`. Assim nunca há dois clientes sendo atendidos ao mesmo tempo, e quem chega durante um atendimento sabe na hora que foi recusado, em vez de ficar esperando sem saber.
- **`compareAndSet` e a ordem de liberar:** `ocupado.compareAndSet(false, true)` testa e marca numa única operação indivisível, então não há como dois clientes passarem juntos. Ao terminar, a thread **libera o indicador primeiro e só depois fecha o socket**: quando o cliente percebe o fim da ligação, o servidor já está livre, e quem reconectar logo em seguida não é recusado por engano.
- **Saudação (`PRONTO` ou `ERRO`):** a recusa precisa chegar ao cliente antes de ele enviar os dados. Por isso o servidor sempre começa com uma linha, e o cliente a lê antes de pedir os números.
- **Como o servidor sabe que acabou (EOF):** o cliente chama `socket.shutdownOutput()`, que fecha só a escrita. O servidor, que está em `readLine()`, recebe `null` (fim de arquivo) e começa a operação. A leitura do cliente continua aberta, e é por ela que o resultado volta. Se o cliente fechasse o socket inteiro, não conseguiria receber a resposta.
- **Texto, e não binário:** o enunciado aceita os dois. O texto é mais fácil de explicar e de depurar (dá para testar até com `telnet`/`nc`). No formato binário a leitura seria um `readInt()` em laço até a `EOFException`, e a operação seria um código inteiro no fim.
- **A operação é a última linha:** o servidor só sabe qual linha é a última quando chega o EOF. Por isso ele guarda todas as linhas e, depois do EOF, trata a última como operação e as anteriores como inteiros.
- **Tempo limite (`setSoTimeout`):** como o servidor atende um de cada vez, um cliente que conecta e fica mudo bloquearia todos os outros para sempre. Depois de 120 s sem receber nada, o servidor responde com erro, fecha a ligação e fica livre de novo.
- **Estouro numérico:** a soma e o produto usam `long` e `Math.addExact`/`Math.multiplyExact`. Se nem o `long` aguentar, o servidor responde `ERRO` em vez de devolver um número errado.
- **Cliente que cai no meio:** o servidor trata a `IOException`, registra no log e continua com o próximo.
- **Caracteres acentuados:** os arquivos estão em UTF-8. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
