# Fluxo de Mensagens e Arquitetura do Agente: Fase 1 (Texto)

Este documento define a arquitetura, o ciclo de vida do gasto e a sequência de mensagens do agente no **VoiceFinance** para a Fase 1 (entrada puramente textual).

---

## 1. Ciclo de Vida do Gasto (Máquina de Estados)

Um registro de gasto transita por três estados ao longo da conversa:

```mermaid
stateDiagram-v2
    [*] --> PENDING_CONFIRMATION: Usuário menciona gasto (startExpense)
    
    PENDING_CONFIRMATION --> PENDING_CONFIRMATION: Esclarecimento de pendências (fillExpense / confirmações)
    
    PENDING_CONFIRMATION --> CONFIRMED: Usuário aprova ("Sim, confirma") (confirmExpense)
    PENDING_CONFIRMATION --> CANCELED: Usuário descarta ("Cancela esse gasto") (cancelExpense)
    
    CONFIRMED --> [*]
    CANCELED --> [*]
```

### Regras de Transição
- **`PENDING_CONFIRMATION`**:
  - Estado inicial atribuído no momento da criação do gasto.
  - Permite edições incrementais (`isEditable() == true`).
  - Pode ter uma ou mais confirmações vinculadas com status `PENDING`.
- **`CONFIRMED`**:
  - Transição permitida somente quando todos os dados obrigatórios estão preenchidos e o usuário deu confirmação expressa.
  - O registro torna-se imutável (`isEditable() == false`).
- **`CANCELED`**:
  - Acionado se o usuário manifestar arrependimento ou desistência do lançamento. Também imutável.

---

## 2. Arquitetura da Fase 1

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

## 3. Cenários Conversacionais Passo a Passo

### Cenário A: Gasto com Dados Completos em Mensagem Única
O usuário fornece todos os dados necessários de forma direta.

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
    DB-->>T: Expense persistido (id=UUID-1, sem campos pendentes)
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

### Cenário B: Gasto com Dados Incompletos (Rodadas de Esclarecimento)
O usuário fala apenas o valor, omitindo categoria ou data.

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
    Note over A: Consulta DateTimeTools: "ontem" = 2026-10-06. Categoria = MERCADO
    A->>T: fillExpense(expenseId=UUID-2, category=MERCADO, occurredOn=2026-10-06)
    T->>DB: UPDATE Expense (preenche dados)
    T-->>A: "Expense saved" (sem pendências restantes)
    A-->>U: "Atualizado: R$ 80,00 em Mercado no dia 06/10. Confirma esse registro?"

    U->>C: "Confirmo"
    A->>T: confirmExpense(expenseId=UUID-2)
    T->>DB: UPDATE Expense (status=CONFIRMED)
    A-->>U: "Gasto confirmado com sucesso!"
```

---

### Cenário C: Descarte ou Cancelamento
O usuário decide não registrar o gasto que estava em andamento.

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

## 4. Conjunto de Ferramentas (Tools) da Fase 1

O conjunto completo de ferramentas em [`FinanceTools`](file:///C:/Users/guima/OneDrive/Documentos/projects/voicefinance/src/main/java/com/voicefinance/tools/FinanceTools.java):

| Ferramenta | Parâmetros | Finalidade |
| :--- | :--- | :--- |
| **`startExpense`** | `amount`, `currency`, `description`, `category`, `occurredOn` (todos opcionais) | Inicia um novo gasto no banco com status `PENDING_CONFIRMATION` e devolve o `expenseId` gerado. Chamada no primeiro contato de uma nova despesa. |
| **`createExpenseFieldConfirmation`** | `expenseId`, `missingFields`, `question` | Registra no banco uma dúvida formal do agente sobre campos pendentes antes de perguntar ao usuário. |
| **`fillExpense`** | `expenseId`, `amount`, `currency`, `description`, `category`, `occurredOn` | Aplica novos dados trazidos pelo usuário em um gasto já existente. |
| **`confirmExpense`** | `expenseId` | Efetiva o gasto, alterando o status para `CONFIRMED` e gravando o timestamp de confirmação. |
| **`cancelExpense`** | `expenseId` | Cancela o gasto pendente, alterando o status para `CANCELED`. |

---

## 5. Roteiro de Implementação da Fase 1

1. **Ferramenta `startExpense`**:
   - Criação de `StartExpenseCommand`.
   - Implementação de `startExpense(...)` em `ExpenseService`.
   - Adição da `@Tool startExpense` em `FinanceTools`.
2. **Ferramentas `confirmExpense` e `cancelExpense`**:
   - Métodos em `ExpenseService` para transição de status (`CONFIRMED` com timestamp e `CANCELED`).
   - Tools correspondentes em `FinanceTools`.
3. **Atualização do System Prompt do Agente**:
   - Ajustar as instruções do modelo em `ChatClientConfig` para orquestrar as ferramentas seguindo o ciclo:
     1. Menção a novo gasto ➔ chamar `startExpense`.
     2. Dados faltantes ➔ chamar `createExpenseFieldConfirmation` e perguntar ao usuário.
     3. Dados complementados ➔ chamar `fillExpense`.
     4. Aprovação do usuário ➔ chamar `confirmExpense`.
     5. Rejeição do usuário ➔ chamar `cancelExpense`.
4. **Testes Automatizados**:
   - Testes unitários para as novas tools e métodos de serviço.
   - Testes simulando os fluxos conversacionais dos Cenários A, B e C.
