# Ex4 – Banco com n clientes (Sockets TCP)

> "Implemente um servidor de uma instituição financeira que permita a conexão de n clientes. Uma vez no sistema, o cliente poderá realizar depósito, saque, saldo ou sair da aplicação. É importante que dois ou mais terminais possam fazer alterações na mesma conta, com o mesmo cliente (ou não), tendo sua atualização on-line sem conflitos."

Vários clientes conectam ao mesmo tempo e entram em contas do banco. Dois terminais podem estar na mesma conta, e o saldo que um altera aparece na hora para o outro, sem perder nenhuma operação.

## Estrutura de pastas

```
Ex4-Banco/
├── src/
│   ├── comum/Protocolo.java          comandos do protocolo e porta padrão
│   ├── comum/Dinheiro.java           valores em centavos: converte "100,50" -> 10050 e formata "R$ 100,50"
│   ├── servidor/Servidor.java        ServerSocket; uma thread por cliente
│   ├── servidor/AtendeCliente.java   atende UM cliente: lê o comando, executa e responde
│   ├── servidor/Banco.java           as contas (lidas e salvas em arquivo)
│   ├── servidor/Conta.java           uma conta: o recurso compartilhado, com métodos synchronized
│   └── cliente/Cliente.java          cliente de console
├── dados/contas.txt                  contas do banco: numero;titular;saldo-em-centavos
├── scripts/                          compilar / servidor / cliente (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

Contas de exemplo: `1001` (Ana, R$ 1000,00), `1002` (Bruno, R$ 500,00) e `1003` (Carla, R$ 0,00).

## Protocolo

O cliente envia uma linha de texto (`writeUTF`) e o servidor responde com uma linha de texto (`writeUTF`).

| Comando | Resposta |
|---|---|
| `LOGIN <conta>` | `OK: conta 1001, titular Ana Souza. Saldo atual R$ 1000,00` |
| `DEPOSITO <valor>` | `OK: depósito de R$ 100,50. Saldo atual R$ 1100,50` |
| `SAQUE <valor>` | `OK: saque de R$ 20,00. Saldo atual R$ 1080,50` (ou `ERRO: saldo insuficiente`) |
| `SALDO` | `OK: saldo atual R$ 1080,50` |
| `SAIR` | `Até logo!` e a conexão é encerrada |

O valor pode ter ponto ou vírgula (`100.5` ou `100,50`), com no máximo 2 casas decimais, e precisa ser maior que zero. Antes do `LOGIN`, só o próprio `LOGIN` e o `SAIR` funcionam. Erros começam com `ERRO:`.

## Como executar

Precisa do JDK 8 ou superior. Os scripts mudam para a pasta do projeto sozinhos.

**Windows**
```
scripts\compilar.bat
scripts\servidor.bat            (um terminal)
scripts\cliente.bat             (outros terminais; abra quantos quiser)
```
**Linux / Mac**
```
sh scripts/compilar.sh
sh scripts/servidor.sh
sh scripts/cliente.sh
```
Porta diferente: `servidor.bat 8000` e `cliente.bat 8000`. Outro arquivo de contas: `servidor.bat 7000 minhas-contas.txt`.

Exemplo de sessão:
```
IP do servidor [127.0.0.1]:
Bem-vindo ao Banco! Comandos: LOGIN <conta>, DEPOSITO <valor>, SAQUE <valor>, SALDO, SAIR
> LOGIN 1001
OK: conta 1001, titular Ana Souza. Saldo atual R$ 1000,00
> DEPOSITO 100,50
OK: depósito de R$ 100,50. Saldo atual R$ 1100,50
> SAQUE 5000
ERRO: saldo insuficiente (saldo atual R$ 1100,50)
> SAIR
Até logo!
```

## Demonstrando "dois terminais na mesma conta"

1. Abra dois clientes (A e B) e faça `LOGIN 1001` nos dois.
2. No A, digite `DEPOSITO 100`.
3. No B, digite `SALDO`: o depósito do A já aparece.
4. Os dois terminais fazem depósitos e saques em sequência rápida: o saldo final é sempre a soma exata das operações.

## Testando em computadores diferentes

Igual aos exercícios anteriores: rode o servidor no computador A (ele mostra os IPs) e, no B, rode o cliente e digite o IP do A. A porta 7000 precisa estar liberada no firewall do A (a regra `netsh advfirewall firewall add rule name="Fortunes" dir=in action=allow protocol=TCP localport=7000` dos exercícios anteriores já vale). Os dois computadores podem entrar na mesma conta ao mesmo tempo.

## Decisões de projeto

- **n clientes:** uma thread por conexão (`AtendeCliente`), como no Ex1. Todas usam o mesmo objeto `Banco`, e portanto as mesmas `Conta`.
- **Sem conflitos (exclusão mútua):** os métodos de `Conta` são `synchronized`, então só uma thread por vez mexe no saldo de uma conta (monitor). Isso evita dois problemas:
  - *Atualização perdida:* dois depósitos leem o mesmo saldo e um deles some.
  - *Verificar e depois agir:* dois saques passam juntos na checagem "tem saldo?" e o saldo fica negativo. Por isso a verificação e a subtração estão dentro do mesmo método (`saca`).
- **Trava por conta, e não pelo banco inteiro:** depósitos em contas diferentes não esperam uns pelos outros.
- **On-line:** não há cópia do saldo no cliente. Todo comando consulta ou altera o valor que está na `Conta` do servidor, então todos os terminais enxergam o mesmo saldo atual.
- **Dinheiro em centavos (`long`):** `double` não representa 0,1 + 0,2 com exatidão. Em centavos a conta é sempre exata.
- **Persistência:** os saldos são regravados em `dados/contas.txt` depois de cada depósito ou saque (em arquivo temporário seguido de troca, para não deixar o arquivo pela metade). `Banco.salvar()` é `synchronized` e só roda depois que a operação na conta terminou, então nenhuma thread segura duas travas ao mesmo tempo (sem risco de deadlock).
- **Login simples:** o `LOGIN` só pede o número da conta, sem senha. É suficiente para o exercício; um banco de verdade pediria autenticação.
- **Caracteres acentuados:** os arquivos estão em UTF-8. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
