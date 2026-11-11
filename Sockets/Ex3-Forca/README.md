# Ex3 – Jogo da forca remoto (Sockets TCP)

> "Implemente um jogo da forca remoto com 1 jogador. A definição da palavra a ser adivinhada ficará no servidor, sendo que ao início o servidor dará o tamanho da palavra (em caracteres). Faça a simulação em texto do jogo, retornando cada parte do boneco ou o caractere da palavra do servidor para o cliente."

O servidor sorteia a palavra e o cliente tenta adivinhar letra por letra. A cada letra, o servidor responde com a palavra parcialmente descoberta (acerto) ou com a parte do boneco que surgiu (erro).

## Estrutura de pastas

```
Ex3-Forca/
├── src/
│   ├── comum/Protocolo.java          porta, etiquetas das mensagens e número máximo de erros
│   ├── servidor/Servidor.java        ServerSocket; uma thread por jogador
│   ├── servidor/Partida.java         atende UM jogador: recebe letras e responde (usa o Jogo)
│   ├── servidor/Jogo.java            a regra da forca (palavra, letras tentadas, erros, boneco); não usa socket
│   ├── servidor/BancoPalavras.java   lê dados/palavras.txt e sorteia uma palavra
│   └── cliente/Cliente.java          cliente de console
├── dados/palavras.txt                palavras do servidor (uma por linha)
├── scripts/                          compilar / servidor / cliente (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

## Protocolo

Mensagens de texto (`writeUTF`/`readUTF`). O cliente envia **uma letra** por mensagem (ou `SAIR` para desistir). O servidor responde **uma mensagem por letra**, e a primeira linha dela é uma etiqueta:

| Etiqueta | Quando | O que vem no texto |
|---|---|---|
| `INICIO` | ao conectar | tamanho da palavra e a máscara `_ _ _ _` |
| `ACERTO` | a letra existe | quantas vezes ela aparece, a palavra com as letras descobertas e `Erros: n/6` |
| `ERRO` | a letra não existe | a parte do boneco que surgiu, o desenho, a máscara e `Erros: n/6` |
| `REPETIDA` | a letra já foi tentada | aviso (não conta erro) |
| `INVALIDA` | não é uma letra | aviso (não conta erro) |
| `VITORIA` | a palavra foi descoberta | a última jogada e a palavra completa; o jogo acaba |
| `DERROTA` | 6 erros ou `SAIR` | a última jogada e a palavra revelada; o jogo acaba |

As partes do boneco surgem nesta ordem: cabeça, tronco, braço esquerdo, braço direito, perna esquerda, perna direita. Letras minúsculas e acentuadas são aceitas (`é` vale `E`).

## Como executar

Precisa do JDK 8 ou superior. Os scripts mudam para a pasta do projeto sozinhos.

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
Porta diferente: `servidor.bat 8000` e `cliente.bat 8000`. Outro arquivo de palavras: `servidor.bat 7000 minhas-palavras.txt`.

Exemplo de partida:
```
IP do servidor [127.0.0.1]:
Bem-vindo à forca! A palavra tem 4 letras.
_ _ _ _
Digite uma letra (SAIR desiste).
> a
A letra 'A' existe (2 vez(es)).
_ A _ A
Erros: 0/6
> z
A letra 'Z' não existe. Surgiu: cabeça.
  +---+
  |   |
  O   |
      |
      |
      |
=========
_ A _ A
Erros: 1/6
```

## Testando em computadores diferentes

Igual aos exercícios anteriores: rode o servidor no computador A (ele mostra os IPs) e, no B, rode o cliente e digite o IP do A. A porta 7000 precisa estar liberada no firewall do A:
```
netsh advfirewall firewall add rule name="Fortunes" dir=in action=allow protocol=TCP localport=7000
```
(terminal como administrador; é a mesma regra dos exercícios 1 e 2). O servidor mostra o IP de quem joga e a palavra sorteada em cada partida.

## Decisões de projeto

- **Quem guarda o estado é o servidor:** a palavra e as letras tentadas ficam no servidor, em um objeto `Jogo` por conexão. O cliente só mostra o que recebe, então não tem como "espiar" a palavra.
- **1 jogador por partida:** cada conexão é uma partida. O servidor usa uma thread por conexão e cada thread tem o seu próprio `Jogo`, então vários jogadores podem jogar ao mesmo tempo sem se misturar. Não há dado compartilhado entre partidas (só o sorteio, que é `synchronized`).
- **Uma resposta por letra:** o cliente envia uma letra e espera exatamente uma mensagem. Por isso, quando uma jogada acaba o jogo, o fim (`VITORIA`/`DERROTA`) vem na mesma mensagem da jogada, e não em outra separada.
- **Separar a regra do socket:** `Jogo` não conhece rede e `Partida` não conhece as regras. Isso deixa cada classe pequena e fácil de explicar.
- **Entradas inválidas não punem:** letra repetida, número ou mais de um caractere recebem aviso e o jogador não perde vida.
- **Caracteres acentuados:** os arquivos estão em UTF-8. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
