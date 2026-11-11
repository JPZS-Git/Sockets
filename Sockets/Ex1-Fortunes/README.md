# Ex1 – Servidor de Fortunes (Sockets TCP)

Servidor que guarda frases e atende clientes pela rede. O cliente envia comandos em texto e o servidor responde.

## Estrutura de pastas

```
Ex1-Fortunes/
├── src/
│   ├── comum/Protocolo.java          comandos do protocolo e porta padrão (usados pelos dois lados)
│   ├── servidor/Servidor.java        abre o ServerSocket e aceita conexões (uma thread por cliente)
│   ├── servidor/AtendeCliente.java   atende UM cliente: lê o comando, executa e responde
│   ├── servidor/BancoFortunes.java   banco de frases (recurso compartilhado, synchronized, salvo em arquivo)
│   └── cliente/Cliente.java          cliente de console
├── dados/fortunes.txt                frases do servidor (uma por linha, UTF-8)
├── scripts/                          compilar / servidor / cliente (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

## Protocolo

O cliente envia uma linha de texto (`writeUTF`) e o servidor responde com uma linha de texto (`writeUTF`).

| Comando | Resposta |
|---|---|
| `GET-FORTUNE` | uma frase sorteada |
| `ADD-FORTUNE <nova frase>` | `OK: frase adicionada na posição N.` |
| `UPD-FORTUNE <pos> <nova frase>` | `OK: frase <pos> atualizada.` (a posição é a mostrada no LST, começando em 1) |
| `LST-FORTUNE` | todas as frases, numeradas |
| `SAIR` | `Até logo!` e a conexão é encerrada |

Erros começam com `ERRO:` (comando desconhecido, argumento faltando, posição inexistente etc.). Os comandos aceitam maiúsculas ou minúsculas.

## Como executar

Precisa do JDK 8 ou superior. Os scripts mudam para a pasta do projeto sozinhos, então podem ser chamados de qualquer lugar.

**Windows**
```
scripts\compilar.bat
scripts\servidor.bat            (um terminal)
scripts\cliente.bat             (outro terminal; pode abrir vários)
```
**Linux / Mac**
```
sh scripts/compilar.sh
sh scripts/servidor.sh
sh scripts/cliente.sh
```
Porta diferente: `servidor.bat 8000` e `cliente.bat 8000` (a padrão é 7000).
Outro arquivo de frases: `servidor.bat 7000 meu-arquivo.txt`.

Exemplo de sessão do cliente:
```
IP do servidor [127.0.0.1]:
Conectado ao Servidor de Fortunes. Comandos: GET-FORTUNE, ADD-FORTUNE <frase>, ...
> GET-FORTUNE
Devagar se vai ao longe.
> ADD-FORTUNE Quem tem socket tem tudo.
OK: frase adicionada na posição 11.
> UPD-FORTUNE 1 Primeira frase trocada
OK: frase 1 atualizada.
> SAIR
Até logo!
```

## Testando em computadores diferentes

1. Rode o servidor no computador A. Ele mostra os IPs da máquina, por exemplo `Clientes de outros computadores: 192.168.0.15:7000`.
2. No computador B, rode o cliente e digite esse IP quando ele perguntar o "IP do servidor".
3. Os dois precisam estar na mesma rede. Se aparecer `Connection refused` ou `timed out`, confira: o servidor está rodando? o IP está certo? o firewall do computador A liberou o Java/porta 7000? (o Windows costuma perguntar na primeira vez: aceite). Em algumas redes Wi-Fi (como a do campus) os computadores não enxergam uns aos outros; nesse caso use o hotspot do celular ou um cabo.
4. O servidor mostra o IP de cada cliente que conecta, o que comprova que a conexão veio de outro computador.

## Decisões de projeto

- **Vários clientes ao mesmo tempo:** o `accept()` devolve um socket por cliente e o servidor cria uma thread (`AtendeCliente`) para cada um. O enunciado não limita a um cliente por vez.
- **Banco compartilhado:** como todas as threads usam o mesmo `BancoFortunes`, os métodos são `synchronized` (monitor). Duas pessoas adicionando ao mesmo tempo não corrompem a lista nem recebem a mesma posição.
- **Persistência:** o banco é lido de `dados/fortunes.txt` ao iniciar e regravado a cada ADD ou UPD. As frases sobrevivem a reiniciar o servidor.
- **`writeUTF`/`readUTF`:** como no exemplo da aula. Cada mensagem é um texto de até 64 KB. As frases são limitadas a 500 caracteres e a resposta do LST é cortada, com aviso, se ficar grande demais.
- **Caracteres acentuados:** os arquivos estão em UTF-8. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
