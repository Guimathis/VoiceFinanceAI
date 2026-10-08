# VoiceFinance - Registro Inteligente de Gastos com IA

<p align="left">
  <img src="https://img.shields.io/badge/JAVA-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/SPRING_BOOT-4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/SPRING_AI-2.0.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring AI" />
  <img src="https://img.shields.io/badge/GOOGLE_GENAI-GEMINI-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Google GenAI" />
  <img src="https://img.shields.io/badge/POSTGRESQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/FLYWAY-DATABASE-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway" />
  <img src="https://img.shields.io/badge/RESILIENCE4J-CIRCUIT_BREAKER-FF5722?style=for-the-badge&logo=resilience4j&logoColor=white" alt="Resilience4j" />
  <img src="https://img.shields.io/badge/SWAGGER-OPENAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger OpenAPI" />
  <img src="https://img.shields.io/badge/APACHE_MAVEN-BUILD-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Apache Maven" />
  <img src="https://img.shields.io/badge/DOCKER-COMPOSE-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</p>

Plataforma inteligente de gestão financeira e registro de gastos via linguagem natural, orientada a agentes autônomos com Spring AI e Google Gemini. O sistema orquestra ferramentas de persistência e validação para capturar despesas via texto, levantar dúvidas sobre dados faltantes de forma interativa, manter histórico de conversação por usuário e assegurar confirmação explícita antes da gravação definitiva no PostgreSQL.

---

## 📌 Sobre o Projeto

O **VoiceFinance** simplifica a anotação de despesas do dia a dia, eliminando formulários manuais extensos. O usuário apenas envia mensagens em linguagem natural (ex.: *"Gastei 45 reais no almoço hoje"*), e o agente de IA:
1. Extrai valor, moeda, categoria, data e descrição;
2. Verifica se algum dado essencial está ausente;
3. Cria pendências formais de esclarecimento no banco caso faltem informações;
4. Pergunta de forma objetiva o que falta e assimila as respostas seguintes;
5. Solicita validação final do usuário antes de oficializar o registro.

---

## 🏗️ Arquitetura da Fase 1 (Texto)

A arquitetura organiza a separação clara entre a camada de entrada HTTP, o núcleo de IA com memória contextual e a camada de serviços e persistência:

```mermaid
flowchart TD
    subgraph Entrada[1. Entrada de Mensagens]
        A["Cliente (App / Web / Postman)"] -->|POST /action| B[ChatController]
        B -->|ChatRequest| C[ExpenseClient]
    end

    subgraph Agente[2. Núcleo de IA]
        C --> D["ChatClient (agentClient)"]
        D <-->|Histórico por userId| E[("ChatMemory (Memória)")]
        D -->|System Prompt + Tools| F{Decisão do LLM}
    end

    subgraph Ferramentas[3. Execução de Ferramentas]
        F -->|"startExpense(...)"| G[FinanceTools]
        F -->|"createExpenseFieldConfirmation(...)"| G
        F -->|"fillExpense(...)"| G
        F -->|"confirmExpense(...)"| G
        F -->|"cancelExpense(...)"| G
        G --> H[ExpenseService]
    end

    subgraph Persistencia[4. Persistência]
        H --> I[ExpenseRepository]
        I <--> J[("PostgreSQL (expenses + confirmations)")]
    end

    H -->|FillExpenseResult| G
    G -->|Texto formatado| D
    D -->|ChatResponse| C
    C -->|JSON| A
```

---

## 🔄 Fluxos de Conversação

### Fluxo 1: Gasto Completo em Mensagem Única
Quando o usuário fornece todos os atributos necessários na primeira frase:

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant C as Controller / Client
    participant A as Agente (Gemini)
    participant T as FinanceTools / Service
    participant DB as PostgreSQL

    U->>C: "Almocei no restaurante hoje, deu 45 reais"
    C->>A: Prompt com texto da mensagem
    Note over A: Identifica: valor=45.00, categoria=ALIMENTACAO, data=HOJE, desc="Almoço no restaurante"
    A->>T: startExpense(amount=45.00, category=ALIMENTACAO, occurredOn=2026-10-07, description="Almoço no restaurante")
    T->>DB: INSERT Expense (status=PENDING_CONFIRMATION)
    DB-->>T: Expense persistido (id=UUID-1, sem pendências)
    T-->>A: "Expense recorded with id UUID-1. All required fields are present."
    A-->>U: "Gasto de R$ 45,00 em Alimentação registrado com sucesso! Posso confirmar?"
    
    U->>C: "Sim, pode confirmar"
    C->>A: Prompt com mensagem + histórico da conversa (lembra do UUID-1)
    A->>T: confirmExpense(expenseId=UUID-1)
    T->>DB: UPDATE Expense (status=CONFIRMED, confirmedAt=now)
    T-->>A: "Expense confirmed successfully."
    A-->>U: "Perfeito! Seu gasto de R$ 45,00 foi confirmado."
```

---

### Fluxo 2: Gasto Incompleto e Rodada de Esclarecimento
Quando o usuário omite campos (por exemplo, diz o valor, mas não a categoria nem a data):

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant C as Controller / Client
    participant A as Agente (Gemini)
    participant T as FinanceTools / Service
    participant DB as PostgreSQL

    U->>C: "Gastei 80 reais"
    C->>A: Prompt com "Gastei 80 reais"
    Note over A: Identifica: valor=80.00. Pendentes: category, occurredOn
    A->>T: startExpense(amount=80.00, description=null, category=null, occurredOn=null)
    T->>DB: INSERT Expense (status=PENDING_CONFIRMATION, id=UUID-2)
    T-->>A: "Expense recorded with id UUID-2, but still missing: category, occurredOnDate"
    Note over A: Salva a confirmação no banco para rastrear a dúvida
    A->>T: createExpenseFieldConfirmation(expenseId=UUID-2, missingFields=["category", "occurredOn"], question="Onde foi esse gasto e em qual dia aconteceu?")
    T->>DB: INSERT ExpenseFieldConfirmation (status=PENDING)
    T-->>A: "Confirmation created for missing fields: category, occurredOn"
    A-->>U: "Anotei R$ 80,00! Em qual categoria foi esse gasto e qual foi a data?"

    U->>C: "Foi no mercado, ontem"
    C->>A: Prompt com resposta + histórico (lembra do UUID-2 e do DateTimeTools)
    Note over A: Consulta DateTimeTools: "ontem" = data correta. Categoria = MERCADO
    A->>T: fillExpense(expenseId=UUID-2, category=MERCADO, occurredOn=data_ontem)
    T->>DB: UPDATE Expense (preenche dados)
    T-->>A: "Expense saved" (sem pendências restantes)
    A-->>U: "Atualizado: R$ 80,00 em Mercado. Confirma esse registro?"

    U->>C: "Confirmo"
    A->>T: confirmExpense(expenseId=UUID-2)
    T->>DB: UPDATE Expense (status=CONFIRMED)
    A-->>U: "Gasto confirmado com sucesso!"
```

---

### Fluxo 3: Descarte ou Cancelamento
Quando o usuário opta por não prosseguir com o lançamento:

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant C as Controller / Client
    participant A as Agente (Gemini)
    participant T as FinanceTools / Service
    participant DB as PostgreSQL

    U->>C: "Cancela esse gasto, informei errado"
    C->>A: Prompt com pedido de cancelamento + histórico (identifica UUID-2 pendente)
    A->>T: cancelExpense(expenseId=UUID-2)
    T->>DB: UPDATE Expense (status=CANCELED)
    T-->>A: "Expense canceled successfully."
    A-->>U: "Gasto cancelado. Nada foi registrado."
```

---

## 🛠️ Ferramentas do Agente (Function Calling)

As ferramentas disponibilizadas para o modelo atuam como a fronteira entre a linguagem natural e o banco relacional:

| Ferramenta | Descrição |
| :--- | :--- |
| **`startExpense`** | Cria um novo registro no banco com status `PENDING_CONFIRMATION`, garantindo que o agente receba um `expenseId` real de volta. |
| **`createExpenseFieldConfirmation`** | Registra formalmente no banco as dúvidas do agente para os campos pendentes de um gasto. |
| **`fillExpense`** | Atualiza os campos de um gasto existente à medida que o usuário envia mensagens complementares. |
| **`confirmExpense`** | Efetiva o gasto, alterando seu status para `CONFIRMED` e registrando a data/hora da confirmação. |
| **`cancelExpense`** | Altera o status do gasto para `CANCELED` em caso de desistência do usuário. |
| **`DateTimeTools`** | Utilitários de data e hora para resolver referências temporais relativas (*"ontem"*, *"segunda-feira passada"*, limites de mês). |

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Java 21**
- **Docker** e **Docker Compose** (para PostgreSQL)
- Chave de API do **Google Gemini** (`GOOGLE_GENAI_APIKEY`)

### 1. Configuração de Variáveis de Ambiente
Crie um arquivo `.env` na raiz do projeto (baseando-se no `.env.example`):

```properties
GOOGLE_GENAI_APIKEY=sua_chave_gemini_aqui
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/voicefinance
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=admin
SERVER_PORT=8000
```

### 2. Subir o Banco de Dados
```powershell
docker compose up -d
```

### 3. Compilar e Executar Testes
```powershell
.\mvnw.cmd test
```

### 4. Iniciar a Aplicação
```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação estará disponível em `http://localhost:8000`.

---

## 📡 Endpoints da API (Fase 1)

### Interação com o Agente
- **`POST /action/`** (ou caminho raiz configurado)
  - **Body**:
    ```json
    {
      "action": "Gastei 32 reais na padaria hoje"
    }
    ```
  - **Response**:
    ```json
    {
      "response": "Anotei R$ 32,00 em Alimentação para a data de hoje. Posso confirmar o registro?"
    }
    ```

### Documentação Swagger / OpenAPI
Com a aplicação em execução, acesse a documentação interativa em:
`http://localhost:8000/swagger-ui.html`

---

## 🔮 Próximos Passos (Roadmap)

- **Fase 2 - Entrada por Áudio**:
  - Endpoint multipart para upload de arquivos de áudio;
  - Pipeline de validação de mídia (tamanho, formato e integridade);
  - Transcrição literal via modelo dedicado do Gemini, integrando o texto transcrito diretamente ao fluxo conversacional da Fase 1.
- **Frontend / Interface do Usuário**:
  - Interface interativa de chat (web e mobile);
  - Exibição de cards de gastos pendentes com botões de confirmação rápida;
  - Notificações de esclarecimentos pendentes.
