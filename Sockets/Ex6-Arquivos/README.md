# Ex6 – Servidor de arquivos (Sockets TCP)

> "Crie um servidor de arquivos, onde o usuário poderá logar e armazenar seus arquivos. O sistema deve permitir apenas fazer upload e download dos arquivos."

O usuário faz login com nome e senha. Depois só pode **enviar** (upload) e **baixar** (download) os seus arquivos. Cada usuário tem a sua própria pasta no servidor e não enxerga a dos outros.

## Estrutura de pastas

```
Ex6-Arquivos/
├── src/
│   ├── comum/Protocolo.java            comandos, porta, tamanho máximo e tamanho do buffer
│   ├── servidor/Servidor.java          ServerSocket; uma thread por cliente
│   ├── servidor/AtendeCliente.java     atende UM cliente: login, upload e download
│   ├── servidor/Usuarios.java          lê dados/usuarios.txt e confere usuário e senha
│   ├── servidor/Armazenamento.java     pasta de cada usuário e validação do nome do arquivo
│   └── cliente/Cliente.java            cliente de console
├── dados/usuarios.txt                  usuários do servidor: usuario;senha
├── dados/armazenamento/                criada pelo servidor: uma subpasta por usuário (os arquivos enviados)
├── scripts/                            compilar / servidor / cliente (.bat para Windows, .sh para Linux/Mac)
└── README.md
```

Usuários de exemplo: `ana` / `123`, `bruno` / `456`, `carla` / `789`.

## Protocolo

O texto usa `writeUTF`, os tamanhos usam `writeLong` e os arquivos viajam como bytes, em pedaços de 8 KB.

| Comando | O cliente envia | O servidor responde |
|---|---|---|
| Login | `"LOGIN"`, usuário, senha | `OK: bem-vindo, ana` ou `ERRO: ... (tentativa n/3)` |
| Upload | `"UPLOAD"`, nome, tamanho | `PRONTO` (ou `ERRO: ...`) |
| | os bytes do arquivo (só depois do `PRONTO`) | `OK: recebido nome (n bytes)` |
| Download | `"DOWNLOAD"`, nome | `OK`, tamanho e os bytes (ou `ERRO: ... não encontrado`) |
| Sair | `"SAIR"` | `Até logo!` |

Só existem upload e download, como pede o enunciado: não há listar nem apagar. Por isso o usuário precisa saber o nome do arquivo que quer baixar (o mesmo nome que usou no upload).

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
Porta diferente: `servidor.bat 8000` e `cliente.bat 8000`. Outro arquivo de usuários e outra pasta de arquivos: `servidor.bat 7000 meus-usuarios.txt D:\arquivos`.

Comandos do cliente:

| Comando | O que faz |
|---|---|
| `upload <caminho>` | envia um arquivo seu para o servidor (o caminho pode ter espaços e aspas) |
| `download <nome>` | baixa o arquivo para a pasta `downloads/` do cliente |
| `sair` | encerra |

Exemplo de sessão:
```
IP do servidor [127.0.0.1]:
Usuário: ana
Senha: 123
OK: bem-vindo, ana
Comandos: upload <caminho>, download <nome>, sair
> upload C:\Users\ana\Documents\relatorio.pdf
OK: recebido relatorio.pdf (482113 bytes)
> download relatorio.pdf
OK: relatorio.pdf salvo em downloads\relatorio.pdf (482113 bytes)
> sair
Até logo!
```

## Testando em computadores diferentes

Igual aos exercícios anteriores: rode o servidor no computador A (ele mostra os IPs) e, no B, rode o cliente e digite o IP do A. A porta 7000 precisa estar liberada no firewall do A (a regra `netsh advfirewall firewall add rule name="Fortunes" dir=in action=allow protocol=TCP localport=7000` dos exercícios anteriores já vale).

Uma boa demonstração: no computador B, faça login como `ana`, envie um arquivo e depois olhe a pasta `dados/armazenamento/ana/` no computador A: o arquivo está lá. Para provar que o arquivo chegou intacto, compare o hash dos dois (`certutil -hashfile arquivo SHA256` no Windows, `sha256sum arquivo` no Linux).

## Decisões de projeto

- **Vários clientes ao mesmo tempo:** uma thread por conexão (`AtendeCliente`). Cada usuário usa a sua pasta, então não há disputa entre usuários diferentes.
- **Login:** até 3 tentativas; depois o servidor encerra a conexão. Antes do login, só `LOGIN` e `SAIR` são aceitos. As senhas ficam em texto no arquivo (simplificação didática; um servidor de verdade guardaria só um hash).
- **Isolamento:** cada usuário tem uma subpasta e o servidor só monta caminhos dentro dela.
- **Nomes de arquivo seguros:** o servidor só aceita letras, números, espaço, ponto, `_`, `-`, `(` e `)`, até 100 caracteres, sem começar com ponto. Isso barra `../outra-pasta`, barras e caracteres especiais, então ninguém sai da própria pasta. Se o sistema de arquivos do servidor não aceitar o nome (por exemplo, acentos em um sistema sem UTF-8), o servidor responde `ERRO` e mantém a conexão.
- **O tamanho vai antes dos bytes:** os bytes de um arquivo não têm marcador de fim, então o receptor precisa saber quantos bytes vêm (`writeLong`) e lê exatamente esse número. O servidor recusa nomes inválidos ou arquivos acima de 100 MB **antes** de receber qualquer byte. Por isso o cliente espera o `PRONTO` antes de enviar o arquivo.
- **Upload seguro contra falhas:** os bytes chegam em um arquivo temporário e só viram o arquivo final (com `Files.move`) quando chegaram completos. Se o cliente cair no meio, o temporário é apagado e o arquivo antigo, se existir, continua intacto. O download no cliente faz o mesmo.
- **Mesmo nome sobrescreve:** enviar de novo um arquivo com o mesmo nome substitui o anterior. Dois uploads simultâneos do mesmo nome terminam com um deles inteiro, nunca uma mistura.
- **Tempo limite na transferência:** se o cliente ficar mudo no meio de um arquivo por 30 s, o servidor desiste. Entre comandos o servidor espera sem limite, para o usuário poder pensar com calma.
- **Arquivos em pedaços de 8 KB:** nem o servidor nem o cliente carregam o arquivo inteiro na memória, então o tamanho do arquivo não depende da memória disponível.
- **Caracteres acentuados:** os arquivos do projeto estão em UTF-8. Nos scripts do Windows o `chcp 65001` e o `-Dfile.encoding=UTF-8` já cuidam disso. Se rodar fora dos scripts, use `javac -encoding UTF-8`.
