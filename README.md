## Como compilar e executar

Precisa do JDK 8 ou superior. Em todos os exercícios, abra o terminal na pasta do exercício e compile primeiro:

- **Windows:** `scripts\compilar.bat`
- **Linux/Mac:** `sh scripts/compilar.sh`

Os exercícios 1 a 6 usam a porta 7000. Para testar em outro computador, rode o servidor no computador A e, no B, informe o IP do A quando o cliente pedir (ou aperte Enter para usar `127.0.0.1`).

### Ex1 – Fortunes
1. Terminal 1, servidor: `scripts\servidor.bat` ou `sh scripts/servidor.sh`
2. Terminal 2 em diante, um por cliente: `scripts\cliente.bat` ou `sh scripts/cliente.sh`

### Ex2 – Operações
1. Terminal 1, servidor: `scripts\servidor.bat` ou `sh scripts/servidor.sh`
2. Terminal 2, cliente: `scripts\cliente.bat` ou `sh scripts/cliente.sh`

### Ex3 – Forca
1. Terminal 1, servidor: `scripts\servidor.bat` ou `sh scripts/servidor.sh`
2. Terminal 2 em diante, um por jogador: `scripts\cliente.bat` ou `sh scripts/cliente.sh`

### Ex4 – Banco
1. Terminal 1, servidor: `scripts\servidor.bat` ou `sh scripts/servidor.sh`
2. Terminal 2 em diante, um por cliente: `scripts\cliente.bat` ou `sh scripts/cliente.sh`

### Ex5 – Rede de lojas
1. Terminal 1, sistema central: `scripts\servidor.bat` ou `sh scripts/servidor.sh`
2. Terminal 2, simulador (sobe 5 filiais de uma vez): `scripts\simulador.bat` ou `sh scripts/simulador.sh`
3. Ou uma filial por terminal, com nomes diferentes: `scripts\filial.bat Centro IP-do-servidor` ou `sh scripts/filial.sh Centro IP-do-servidor`

### Ex6 – Servidor de arquivos
1. Terminal 1, servidor: `scripts\servidor.bat` ou `sh scripts/servidor.sh`
2. Terminal 2 em diante, um por usuário: `scripts\cliente.bat` ou `sh scripts/cliente.sh`
3. Faça login com `ana` / `123`, `bruno` / `456` ou `carla` / `789`

### Ex7 – Chat com multicast
1. Não há servidor. Abra um terminal por participante: `scripts\chat.bat` ou `sh scripts/chat.sh`
2. Informe o apelido e, se houver mais de uma placa de rede, escolha a da rede em comum
3. Todos precisam estar na mesma rede (grupo `226.1.1.1`, porta `6789`)
