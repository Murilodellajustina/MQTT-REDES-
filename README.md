# MQTT-REDES

Projeto acadêmico desenvolvido para a disciplina de **Redes de Computadores**, implementando uma arquitetura de comunicação baseada no modelo **Publish/Subscribe (Pub/Sub)**, semelhante ao funcionamento do MQTT.

O sistema possui um **Broker** central, clientes que se conectam ao Broker e um conjunto de **tópicos** utilizados para organizar a troca de mensagens.

Além da comunicação, o projeto implementa mecanismos de autenticação e segurança utilizando **RSA, AES e certificados digitais**.

---

## 📌 Objetivo

O objetivo do trabalho é colocar em prática conceitos de Redes de Computadores por meio da implementação de um sistema de mensageria baseado em tópicos.

O projeto trabalha principalmente com:

- comunicação cliente-servidor;
- sockets TCP;
- arquitetura Broker/Cliente;
- modelo Publish/Subscribe;
- criação e gerenciamento de tópicos;
- inscrição e cancelamento de inscrição;
- publicação de mensagens;
- autenticação por certificados;
- criptografia RSA;
- criptografia AES;
- gerenciamento de chaves;
- histórico de mensagens;
- tratamento de clientes conectados e desconectados.

> **Observação:** o projeto implementa o modelo de comunicação do MQTT/Pub-Sub de forma própria, utilizando sockets e comandos definidos pela aplicação. Ele não utiliza uma biblioteca MQTT pronta para realizar a comunicação.

---

# 🏗️ Arquitetura

O sistema é organizado em torno de um **Broker**, que centraliza a comunicação entre os clientes.

```text
                         ┌──────────────────────┐
                         │        BROKER        │
                         │                      │
                         │    BrokerServer      │
                         │    BrokerCore        │
                         │    ClienteHandler    │
                         │    Gerenciamento     │
                         │    de usuários       │
                         │    e tópicos         │
                         └──────────┬───────────┘
                                    │
                   ┌────────────────┼────────────────┐
                   │                │                │
                   ▼                ▼                ▼
             ┌──────────┐     ┌──────────┐     ┌──────────┐
             │ Cliente  │     │ Cliente  │     │ Cliente  │
             │    A     │     │    B     │     │    C     │
             └──────────┘     └──────────┘     └──────────┘
```

O cliente não envia uma mensagem diretamente para outro cliente.

Em vez disso:

1. o cliente publica a mensagem em um tópico;
2. o Broker recebe a publicação;
3. o Broker identifica os usuários inscritos no tópico;
4. a mensagem é encaminhada aos clientes inscritos.

---

# 📡 Modelo Publish/Subscribe

O funcionamento segue o conceito de **Publish/Subscribe**.

Um cliente pode se inscrever em um tópico:

```text
Cliente A
    │
    │ SUB esportes
    ▼
  Broker
```

Outro cliente pode publicar uma mensagem nesse mesmo tópico:

```text
Cliente B
    │
    │ PUB esportes mensagem
    ▼
  Broker
    │
    ▼
Cliente A
```

Dessa forma, o Publisher e o Subscriber não precisam conhecer diretamente um ao outro.

O Broker é responsável por intermediar toda a comunicação.

---

# 🖥️ Componentes principais

## BrokerServer

É o ponto de entrada do servidor.

O `BrokerServer` cria um `ServerSocket` na porta **9090** e aguarda conexões de clientes.

Para cada nova conexão, é criado um `ClienteHandler` em uma nova thread.

```text
Porta: 9090
```

Estrutura simplificada:

```text
BrokerServer
    │
    ├── aceita conexão
    │
    └── cria ClienteHandler
             │
             └── processa comandos do cliente
```

---

## BrokerCore

O `BrokerCore` concentra as regras principais do Broker.

Ele mantém estruturas para:

- usuários;
- tópicos;
- inscritos;
- histórico de mensagens;
- sentinelas dos tópicos.

Entre as principais operações estão:

```text
registrarUsuario()
getUsuario()
CriarTopicos()
listarTopicos()
subscribe()
unsubscribe()
publish()
enviarChaveTopico()
getHistoricoTopico()
```

O armazenamento é feito em memória utilizando estruturas como `ConcurrentHashMap` e conjuntos/listas concorrentes.

---

## ClienteHandler

Cada cliente conectado possui um `ClienteHandler`.

Essa classe:

- recebe comandos;
- interpreta as mensagens;
- executa operações no `BrokerCore`;
- envia respostas ao cliente;
- controla a conexão;
- realiza o processo de login;
- envia chaves dos tópicos;
- entrega mensagens pendentes.

Os comandos são recebidos por meio do socket utilizando leitura de linhas.

---

# 👤 Usuários

A classe `Usuario` representa um usuário registrado no Broker.

Cada usuário possui informações como:

- nome;
- estado online/offline;
- chave pública;
- conexão atual;
- mensagens/histórico associados aos tópicos.

Quando o cliente realiza login, o Broker registra ou recupera o usuário correspondente.

---

# 🔑 Autenticação

O projeto utiliza certificados para validar a identidade dos usuários.

O cliente possui um certificado associado ao seu nome, por exemplo:

```text
certificados/
├── Lucas.crt
└── Murilo.crt
```

Durante o login, o cliente envia o certificado em Base64:

```text
LOGIN nome certificado
```

O Broker recebe o certificado e realiza a validação antes de permitir o acesso.

Se a validação for bem-sucedida:

```text
LOGIN OK
```

Caso contrário:

```text
LOGIN RECUSADO!
```

---

# 🔐 Certificados do Broker

Além da autenticação do usuário, o cliente também verifica o certificado do Broker.

Após o login, o cliente solicita a validação do Broker:

```text
VALID_BROKER nome
```

O Broker responde enviando o certificado:

```text
BROKER_CERT <certificado>
```

O cliente verifica o certificado utilizando a chave pública da autoridade certificadora (`AC_Pub.key`).

O certificado também passa por uma verificação de validade.

O fluxo fica:

```text
Cliente
   │
   │ LOGIN + certificado
   ▼
Broker
   │
   │ valida certificado
   ▼
LOGIN OK
   │
   │ VALID_BROKER
   ▼
Broker
   │
   │ BROKER_CERT
   ▼
Cliente
   │
   │ valida certificado do Broker
   ▼
Acesso liberado
```

---

# 🔏 RSA

O RSA é utilizado no projeto para operações relacionadas às chaves e certificados.

Cada usuário possui um par de chaves:

```text
nomePub.key
nomePvd.key
```

O par é composto por:

- chave pública;
- chave privada.

A chave pública pode ser utilizada para criptografar informações que somente a chave privada correspondente consegue descriptografar.

No projeto, esse mecanismo é utilizado principalmente na distribuição das chaves AES dos tópicos.

---

# 🔒 Criptografia AES

As mensagens dos tópicos são criptografadas utilizando **AES**.

A classe responsável por isso é:

```text
CryptoAES
```

Ela possui operações de:

```text
criptografar()
descriptografar()
```

Quando um usuário envia uma mensagem, ela é criptografada antes de ser publicada:

```text
Mensagem original
       │
       ▼
     AES
       │
       ▼
Mensagem criptografada
       │
       ▼
     Broker
```

O Broker encaminha o conteúdo recebido aos participantes do tópico.

O cliente destinatário utiliza a chave AES do tópico para descriptografar a mensagem.

---

# 🛡️ Sentinela de tópico

Cada tópico criado pelo Broker possui um objeto `SentinelaTopico`.

O Sentinela é responsável pelo gerenciamento da chave AES daquele tópico.

Quando um tópico é criado:

```text
CREATE esportes
```

é criada uma nova chave AES de 256 bits para o tópico.

A estrutura é aproximadamente:

```text
Tópico
   │
   └── SentinelaTopico
          │
          ├── nome do tópico
          ├── chave AES
          └── membros
```

O Sentinela também mantém os usuários que fazem parte daquele tópico.

---

# 🔑 Distribuição da chave do tópico

A chave AES do tópico não é enviada diretamente em texto aberto.

Quando um usuário realiza:

```text
SUB esportes
```

o Broker obtém a chave AES do tópico e utiliza a **chave pública RSA do usuário** para criptografá-la.

O cliente recebe:

```text
TOPIC_KEY esportes <chave>
```

Em seguida:

1. o cliente utiliza sua chave privada RSA;
2. descriptografa a chave AES;
3. transforma os bytes recuperados em uma `SecretKey`;
4. armazena a chave para utilizar nas mensagens do tópico.

O fluxo é:

```text
                 Broker
                   │
          chave AES do tópico
                   │
                   ▼
            RSA + chave pública
                   │
                   ▼
        chave AES criptografada
                   │
                   ▼
                Cliente
                   │
            chave privada RSA
                   │
                   ▼
              chave AES
                   │
                   ▼
          descriptografia das
               mensagens
```

---

# 📨 Publicação de mensagens

Para publicar uma mensagem, o cliente precisa estar inscrito no tópico.

Na interface principal, o usuário seleciona um tópico e escreve a mensagem.

O cliente:

1. recupera a chave AES do tópico;
2. criptografa a mensagem;
3. envia o comando `PUB`.

Exemplo conceitual:

```text
PUB esportes <mensagem_criptografada>
```

O Broker recebe a mensagem e cria um pacote no formato:

```text
[MSG][remetente topico] mensagem
```

Depois, encaminha esse pacote aos usuários inscritos.

---

# 📥 Recebimento de mensagens

Quando uma mensagem chega ao cliente, a aplicação identifica:

- remetente;
- tópico;
- conteúdo criptografado.

A chave AES correspondente ao tópico é recuperada e utilizada para descriptografar o conteúdo.

Depois disso, a mensagem é exibida na interface.

Exemplo:

```text
[MSG][Murilo esportes] Olá, pessoal!
```

---

# 📴 Usuários offline

O sistema também possui tratamento para usuários que estão desconectados.

A classe `Usuario` mantém informações relacionadas às mensagens armazenadas para o usuário.

Quando o cliente retorna à comunicação, o `ClienteHandler` pode entregar as mensagens pendentes.

O projeto também possui os comandos:

```text
ONLINE
OFFLINE
```

O fluxo pode ser representado por:

```text
Cliente conectado
      │
      │ OFFLINE
      ▼
Usuário offline
      │
      │ mensagens recebidas
      ▼
Mensagens armazenadas
      │
      │ ONLINE
      ▼
Mensagens entregues
      │
      ▼
Cliente
```

---

# 🗂️ Histórico de tópicos

O Broker mantém um histórico das mensagens publicadas nos tópicos.

Esse histórico é armazenado pelo `BrokerCore` em memória.

Quando um cliente realiza novamente a validação do Broker, o sistema pode recuperar o histórico dos tópicos em que aquele usuário está inscrito.

Isso permite que o cliente tenha acesso às mensagens anteriores do tópico.

---

# 📋 Comandos

A comunicação entre cliente e Broker utiliza comandos definidos pelo próprio projeto.

| Comando | Função |
|---|---|
| `LOGIN` | Realiza a autenticação do usuário |
| `CREATE` | Cria um novo tópico |
| `LIST` | Lista os tópicos existentes |
| `SUB` | Inscreve o usuário em um tópico |
| `UNSUB` | Remove a inscrição do usuário |
| `PUB` | Publica uma mensagem |
| `ONLINE` | Marca o usuário como online |
| `OFFLINE` | Marca o usuário como offline |
| `CERTIFICADO` | Solicita/realiza o processo de geração de certificado |
| `VALID_BROKER` | Solicita o certificado do Broker |

---

# 🖥️ Interface gráfica

A interface foi desenvolvida utilizando **Java Swing**.

O sistema possui principalmente três telas:

### TelaLogin

Permite informar o nome do usuário e iniciar o processo de autenticação.

```text
┌────────────────────────────┐
│       INSIRA SEU NOME      │
│                            │
│     [      Murilo      ]   │
│                            │
│  [APAGAR]        [ENTRAR] │
└────────────────────────────┘
```

---

### TelaPrincipal

É a tela utilizada para a comunicação.

Ela permite:

- visualizar tópicos disponíveis;
- visualizar tópicos inscritos;
- entrar em um tópico;
- sair de um tópico;
- visualizar mensagens;
- enviar mensagens;
- atualizar a lista de tópicos;
- criar novos tópicos.

Estrutura simplificada:

```text
┌───────────────────────────────────────────────┐
│ TÓPICOS DISPONÍVEIS      NOME DO TÓPICO       │
│ ┌───────────────┐       ┌─────────────────┐  │
│ │ esportes      │       │ mensagens       │  │
│ │ jogos         │       │                 │  │
│ │ faculdade     │       │                 │  │
│ └───────────────┘       └─────────────────┘  │
│                                               │
│ TÓPICOS INSCRITOS                             │
│ ┌───────────────┐       [ mensagem ] [ENVIAR]│
│ │ esportes      │                             │
│ │ faculdade     │                             │
│ └───────────────┘                             │
└───────────────────────────────────────────────┘
```

---

### TelaCriarTopico

Permite criar um novo tópico.

Ao criar um tópico, o cliente envia:

```text
CREATE nome_do_topico
```

e posteriormente realiza a inscrição nesse tópico.

---

# 📁 Estrutura do projeto

A estrutura principal é semelhante a:

```text
Trabalho_Redes/
│
├── pom.xml
│
├── broker.csr
├── Broker.jks
│
├── certificados_recebidos/
│   ├── Lucas.crt
│   └── Murilo.crt
│
├── src/
│   └── main/
│       └── java/
│           ├── certificados/
│           │   ├── broker.cer
│           │   ├── Lucas.crt
│           │   └── Murilo.crt
│           │
│           ├── keys/
│           │   ├── AC_Pub.key
│           │   ├── brokerPub.key
│           │   ├── brokerPvd.key
│           │   ├── LucasPub.key
│           │   ├── LucasPvd.key
│           │   ├── MuriloPub.key
│           │   └── MuriloPvd.key
│           │
│           └── com/
│               └── mycompany/
│                   └── trabalho_redes/
│                       ├── BrokerCore.java
│                       ├── BrokerServer.java
│                       ├── Cliente.java
│                       ├── ClienteHandler.java
│                       ├── CryptoAES.java
│                       ├── CryptoRSA.java
│                       ├── SentinelaTopico.java
│                       ├── Usuario.java
│                       ├── TelaLogin.java
│                       ├── TelaPrincipal.java
│                       └── TelaCriarTopico.java
```

---

# 🧩 Tecnologias utilizadas

| Tecnologia | Utilização |
|---|---|
| **Java** | Desenvolvimento da aplicação |
| **Java Swing** | Interface gráfica |
| **Sockets TCP** | Comunicação entre clientes e Broker |
| **Maven** | Gerenciamento do projeto |
| **RSA** | Criptografia de chaves e assinaturas |
| **AES** | Criptografia das mensagens |
| **Certificados digitais** | Autenticação e validação |
| **ConcurrentHashMap** | Gerenciamento concorrente dos dados |
| **Threads** | Atendimento simultâneo dos clientes |

O projeto está configurado no Maven para utilizar **Java 21**.

---

# ▶️ Execução

## 1. Requisitos

Para executar o projeto, é necessário ter:

- Java 21;
- Maven;
- uma IDE compatível com Java, como NetBeans, IntelliJ IDEA ou Eclipse.

---

## 2. Abrir o projeto

Abra a pasta:

```text
Trabalho_Redes/
```

como um projeto Maven.

O arquivo responsável pela configuração é:

```text
pom.xml
```

---

## 3. Iniciar o Broker

Execute:

```text
BrokerServer.java
```

O servidor será iniciado na porta:

```text
9090
```

No console deverá aparecer uma mensagem semelhante a:

```text
Broker rodando na porta 9090
```

---

## 4. Iniciar o cliente

Execute:

```text
TelaLogin.java
```

Informe o nome do usuário que possui certificado configurado.

Após a autenticação e validação do Broker, será aberta a:

```text
TelaPrincipal
```

---

# 🔄 Exemplo de utilização

Supondo dois clientes:

```text
Murilo
Lucas
```

## 1. Cliente Murilo cria um tópico

```text
CREATE redes
```

O Broker cria o tópico e sua respectiva chave AES.

---

## 2. Lucas entra no tópico

```text
SUB redes
```

O Broker adiciona Lucas como membro do tópico e envia a chave AES protegida pela chave pública RSA de Lucas.

---

## 3. Murilo publica uma mensagem

Murilo escreve:

```text
Olá, Lucas!
```

A aplicação criptografa a mensagem com AES e envia:

```text
PUB redes <mensagem_criptografada>
```

---

## 4. Broker distribui

O Broker recebe a mensagem e encaminha o pacote aos usuários inscritos:

```text
[MSG][Murilo redes] <mensagem_criptografada>
```

---

## 5. Lucas descriptografa

Lucas utiliza a chave AES correspondente ao tópico para recuperar:

```text
Olá, Lucas!
```

---

# 🔐 Fluxo completo de segurança

O sistema combina diferentes mecanismos criptográficos.

```text
                AUTENTICAÇÃO
                     │
                     ▼
             Certificado digital
                     │
                     ▼
                 LOGIN OK
                     │
                     ▼
              Chave pública RSA
                     │
                     ▼
       Distribuição segura da chave AES
                     │
                     ▼
              Chave AES do tópico
                     │
                     ▼
            Criptografia da mensagem
                     │
                     ▼
                   PUB
                     │
                     ▼
                  BROKER
                     │
                     ▼
              Cliente inscrito
                     │
                     ▼
            Descriptografia AES
                     │
                     ▼
              Mensagem original
```

---

# 🎯 Conceitos demonstrados

O projeto reúne diversos conceitos importantes de Redes de Computadores e Segurança:

### Comunicação em rede

A comunicação entre os componentes ocorre por meio de sockets TCP.

### Concorrência

O Broker utiliza threads para atender diferentes clientes simultaneamente.

### Publish/Subscribe

Os clientes se comunicam por meio de tópicos, sem necessidade de conexão direta entre Publisher e Subscriber.

### Broker

O Broker centraliza o gerenciamento da comunicação, dos tópicos e dos usuários.

### Autenticação

O acesso é protegido por certificados digitais.

### Criptografia assimétrica

RSA é utilizado para proteger a distribuição das chaves dos tópicos e para assinaturas.

### Criptografia simétrica

AES é utilizado para proteger o conteúdo das mensagens.

### Gerenciamento de chaves

Cada tópico possui uma chave AES própria, administrada pelo `SentinelaTopico`.

### Tolerância a desconexões

O projeto possui mecanismos para manter informações de usuários offline e entregar mensagens posteriormente.

---

# 📚 Organização das responsabilidades

```text
BrokerServer
     │
     │ inicia servidor
     ▼
ClienteHandler
     │
     │ interpreta comandos
     ▼
BrokerCore
     │
     ├── usuários
     ├── tópicos
     ├── inscrições
     ├── histórico
     └── Sentinelas
              │
              └── chaves AES
```

Enquanto isso, no cliente:

```text
TelaLogin
     │
     │ autenticação
     ▼
TelaPrincipal
     │
     ├── tópicos
     ├── inscrições
     ├── mensagens
     └── publicação
          │
          ▼
      CryptoAES
          │
          ▼
       Socket
```

---

# 👨‍💻 Autor

**Murilo Della Justina**  

Projeto acadêmico desenvolvido para a disciplina de **Redes de Computadores**.

---

# 📄 Considerações finais

O projeto demonstra a construção de um sistema de comunicação distribuída baseado no modelo Publish/Subscribe, implementando o próprio protocolo de comunicação entre clientes e Broker.

Além da troca de mensagens, o trabalho incorpora mecanismos de segurança com certificados digitais, RSA e AES, permitindo estudar não somente a comunicação em rede, mas também aspectos de autenticação, confidencialidade e gerenciamento de chaves.

O resultado é uma aplicação cliente-servidor em Java na qual múltiplos usuários podem criar tópicos, realizar inscrições e trocar mensagens de maneira intermediada pelo Broker.
