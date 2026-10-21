# Foody Order Tracker

Mini rastreador de pedidos de delivery: cadastro e login de usuários, criação de pedidos e acompanhamento do status de cada um, do recebimento até a entrega.

## Funcionalidades

- Cadastro e login com autenticação JWT; só usuários autenticados acessam os pedidos.
- Criação de pedido com cliente, endereço de entrega e itens (produto e quantidade).
- Lista de pedidos com abas por status e contagem, do mais recente para o mais antigo.
- Detalhes do pedido com botão para avançar o status e opção de cancelar.
- Atualização automática da lista a cada 10 segundos (polling).

## Stack

| Camada | Tecnologias |
| --- | --- |
| Back-end | Java 21, Spring Boot 4.0.8, Spring Security + JWT (JJWT 0.13.0), Spring Data JPA/Hibernate, Flyway, Bean Validation, SQLite |
| Testes e qualidade do back-end | JUnit 6, Mockito, testes de integração HTTP, JaCoCo (cobertura), SpotBugs (análise estática) |
| Front-end | React 19, TypeScript, Vite 8, MUI 9, React Router 8, ESLint |

## Estrutura do repositório

```text
foody-order-tracker/
├── backend/    API REST (Spring Boot)
│   └── src/main/java/com/foody/ordertracker/
│       ├── controller/   endpoints REST
│       ├── service/      regras de negócio
│       ├── repository/   acesso a dados (Spring Data JPA)
│       ├── entity/       entidades JPA
│       ├── dto/          objetos de entrada e saída da API
│       ├── security/     JWT, filtro e configuração do Spring Security
│       └── exception/    exceções e tratamento padronizado de erros
└── frontend/   interface (React + Vite + MUI)
    └── src/
        ├── api/      cliente HTTP e tipos da API
        ├── auth/     contexto e armazenamento do token
        ├── pages/    telas de login, cadastro e pedidos
        └── orders/   componentes e regras de exibição dos pedidos
```

O back-end segue a arquitetura em camadas (controller -> service -> repository). As entidades nunca são expostas diretamente: a API usa DTOs separados.

## Pré-requisitos

- JDK 21 ou superior, com a variável `JAVA_HOME` apontando para a raiz do JDK.
- Node.js 20.19+ ou 22.12+ (e npm).
- Portas 8080 (back-end) e 5173 (front-end) livres.

## Como rodar

### 1. Back-end

```powershell
cd backend
.\mvnw spring-boot:run
```

No Linux, macOS ou Git Bash, use `./mvnw spring-boot:run`.

O servidor sobe em `http://localhost:8080`. Na primeira execução, o Flyway aplica as migrations e cria as tabelas no banco SQLite. O arquivo `orders.db` é criado no diretório de execução da aplicação e está ignorado pelo Git.

**Segredo do JWT.** A variável de ambiente `JWT_SECRET` define o segredo de assinatura dos tokens. Para desenvolvimento local ela é opcional: sem ela, o app usa um valor padrão de desenvolvimento, que está público neste repositório. Em qualquer ambiente real, defina um segredo longo e aleatório antes de iniciar o back-end, no mesmo terminal:

```powershell
$env:JWT_SECRET = "troque-por-um-segredo-longo-e-aleatorio-com-mais-de-64-caracteres"
```

```bash
export JWT_SECRET="troque-por-um-segredo-longo-e-aleatorio-com-mais-de-64-caracteres"
```

### 2. Front-end

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O front usa `http://localhost:8080` como URL da API. O CORS do back-end só libera a origem `http://localhost:5173`.

### 3. Primeiro acesso

O banco nasce vazio: não existe usuário nem pedido pré-cadastrado. Crie sua conta na tela `/register` (nome, e-mail e senha), entre pelo login e crie o primeiro pedido.

## API

Todas as rotas, exceto `/auth/*`, exigem o header `Authorization: Bearer <token>`.

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| POST | `/auth/register` | não | Cadastra um usuário (201) |
| POST | `/auth/login` | não | Autentica e devolve `token` e `type` (200) |
| POST | `/orders` | sim | Cria um pedido (201); nasce como `RECEBIDO` |
| GET | `/orders` | sim | Lista pedidos, mais recentes primeiro; filtro opcional `?status=` |
| GET | `/orders/{id}` | sim | Busca um pedido por ID |
| PATCH | `/orders/{id}/status` | sim | Atualiza o status do pedido |

Cadastro (`/auth/register`): `name` (obrigatório, até 100 caracteres), `email` (obrigatório, válido, até 150) e `password` (obrigatória, 8 a 72 caracteres).

Login (`POST /auth/login`):

```json
{ "email": "maria@exemplo.com", "password": "senha-12345" }
```

Criar pedido (`POST /orders`):

```json
{
  "customerName": "Maria Souza",
  "deliveryAddress": "Rua das Flores, 123",
  "items": [
    { "productName": "Pizza grande", "quantity": 1 },
    { "productName": "Refrigerante 2L", "quantity": 2 }
  ]
}
```

Atualizar status (`PATCH /orders/{id}/status`):

```json
{ "status": "EM_PREPARO" }
```

A resposta de um pedido (`OrderResponse`) traz `id`, `customerName`, `deliveryAddress`, `status`, `createdAt`, `updatedAt` e `items`.

### Erros

Os erros seguem um formato padronizado, com `status`, `message` e `timestamp`:

| Código | Quando |
| --- | --- |
| 400 | Validação, corpo malformado, status inválido (inclusive em `?status=`) |
| 401 | Sem token, token inválido ou login com credenciais erradas |
| 404 | Pedido não encontrado |
| 409 | E-mail já cadastrado ou transição de status inválida |

### Regras de status

```text
RECEBIDO -> EM_PREPARO -> SAIU_PARA_ENTREGA -> ENTREGUE
```

- O status avança um passo por vez.
- `CANCELADO` é permitido a partir de qualquer status que não seja final.
- `ENTREGUE` e `CANCELADO` são finais, e repetir o status atual é proibido.
- Qualquer transição inválida devolve `409 Conflict`.

## Testes e qualidade

```powershell
cd backend
.\mvnw test
```

São 83 testes entre unitários (regras de status, serviços, JWT) e de integração (endpoints, segurança e persistência). A máquina de estados do status e a API foram desenvolvidas com TDD.

Ao rodar `mvnw test`, o JaCoCo gera o relatório de cobertura em `backend/target/site/jacoco/index.html` (a pasta `target/` não é versionada). Na última medição: 98% das instruções e 97% dos branches.

Análise estática: `.\mvnw spotbugs:check` (na pasta `backend`) roda o SpotBugs, que na última execução fechou sem achados. Há uma única exclusão, documentada em `backend/spotbugs-exclude.xml`: o `ObjectMapper` injetado pelo Spring é um singleton compartilhado, então o alerta de exposição da referência é um falso positivo.

O front-end não tem testes automatizados; a qualidade é verificada com `npm run lint` e `npm run build`.

## Decisões e tradeoffs

- **Ferramenta interna, sem perfis de acesso:** qualquer usuário autenticado lista, consulta, cria e atualiza todos os pedidos. Cada pedido guarda quem o criou (`created_by`), mas isso ainda não é exposto pela API.
- **SQLite:** zero configuração e fácil de rodar. As tabelas são responsabilidade das migrations do Flyway (`ddl-auto=none`) e o pool de conexões tem uma única conexão, adequado ao modelo de escrita única do SQLite.
- **JWT stateless com validade de 60 minutos:** sem sessão no servidor, sem refresh token. O "Sair" apenas apaga o token do navegador.
- **Token no `localStorage`:** simples e compatível com o header `Authorization`, mas exposto a roubo por XSS; a alternativa mais segura seria um cookie `httpOnly`, que exigiria mudar o back-end.
- **Polling de 10 segundos:** mais simples que WebSocket/SSE; o custo é um atraso de até 10 segundos e, se uma requisição em andamento terminar logo após uma ação do usuário, a tela pode mostrar o dado antigo até o próximo ciclo.
- **Filtro e contagem das abas no cliente:** o front busca todos os pedidos e filtra no navegador. O back-end também aceita `?status=`.
- **Itens sem preço:** só nome do produto e quantidade, por decisão de escopo; não há total do pedido.
- **PATCH e 409:** atualizar só o status é uma alteração parcial (PATCH, não PUT), e transição inválida é conflito com o estado atual do recurso (409, não 400).
- **Fetch nativo e Context API:** sem axios e sem Redux, para manter o front enxuto.

## Fluxo de Git

- `master`: versão estável; recebe a `release` no fechamento e é marcada com tag (`v1.0.0`).
- `release`: integração; recebe as features com `git merge --no-ff`.
- `feature/*`: uma branch por funcionalidade, criada a partir da `release`.