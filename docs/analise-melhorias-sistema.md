---
title: "Análise de Pontos de Melhoria e Robustez do VoiceFinance"
date: "2026-10-13 14:16:27"
tags: [voicefinance, spring-ai, arquitetura, seguranca, resiliencia, code-review]
---

Uma análise aprofundada da base de código do **VoiceFinance** revela uma base técnica sólida (Java 21, Spring Boot, Spring AI, PostgreSQL, Flyway, Resilience4j), mas com diversas **suposições de "caminho feliz" (happy path)** e simplificações que tornam o sistema ingênuo diante do uso no mundo real.

Abaixo estão os principais pontos de fragilidade identificados e as recomendações práticas para tornar o sistema robusto, seguro e resiliente.

---

### 1. Falha Crítica de Segurança e Multitenancy (IDOR nas Ferramentas)

* **Onde está:** `ExpenseService.java` nas chamadas de `FinanceTools.java`.
* **O problema:**
  Enquanto os endpoints REST tradicionais utilizam `expenseRepository.findByIdAndUserId(id, userId)` para garantir que o usuário só altere o que é seu, as ferramentas da IA (`fillExpense`, `createExpenseFieldConfirmation`, `confirmExpense`, `cancelExpense`) executam:
  ```java
  Expense expense = expenseRepository.findById(cmd.expenseId()).orElse(null);
  ```
  Isso não valida se o registro pertence ao `currentUserProvider.getCurrentUserId()`. Qualquer alucinação do modelo ou injeção de prompt que informe outro UUID pode atualizar, confirmar ou cancelar despesas de terceiros (**IDOR - Insecure Direct Object References**).
* **Como melhorar:**
  Exigir `findByIdAndUserId(cmd.expenseId(), currentUserId)` em todos os métodos do serviço expostos às ferramentas.

---

### 2. Ciclo de Vida da Despesa Prematuro (Máquina de Estados Ingênua)

* **Onde está:** `ExpenseService.java` e `Expense.java`.
* **O problema:**
  * **Persistência imediata no banco:** Na primeira menção de qualquer texto em linguagem natural, o sistema já faz um `INSERT` em `expenses` com status `PENDING_CONFIRMATION`, mesmo com quase todos os campos nulos. Se o usuário mandar um *"Bom dia"*, desistir ou mudar de ideia na primeira frase, um registro órfão permanece gravado no banco de dados.
  * **Falta de TTL / Expiração:** Não existe limpeza automática nem prazo de expiração para registros em `PENDING_CONFIRMATION`. Com o tempo, a base se enche de gastos incompletos e abandonados.
  * **Falta de detecção de duplicidade (Idempotência):** Se o usuário reenviar a mensagem ou enviar um áudio parecido, o sistema cria dois registros pendentes idênticos sem alertar sobre duplicidade.
* **Como melhorar:**
  * Manter o gasto em **estado de rascunho na sessão/memória conversacional** enquanto os dados estiverem sendo coletados, persistindo em banco apenas quando os requisitos mínimos existirem.
  * Implementar uma rotina (ex: `@Scheduled`) para expirar despesas pendentes após 24h ou 48h.

---

### 3. Inconsistência no Mecanismo de Confirmação de Campos

* **Onde está:** `ConfirmableFields.java`, `Expense.java` e `ConfirmationField.java`.
* **O problema:**
  Existe um desacoplamento confuso entre três formas de validação de campos pendentes:
  1. `Expense.missingFields()` checa explicitamente `amount`, `category` e `occurredOnDate`.
  2. `ConfirmableFields` faz reflexão procurando anotações `@Confirmable` na classe `Expense`. Porém, o atributo `amount` é um `@Embedded Money amount`, e **não** está anotado com `@Confirmable` na entidade `Expense`. Portanto, `confirmableFields.missing(expense)` nunca acusa ausência de valor monetário!
  3. Ao validar campos em `createExpenseFieldConfirmation`, `confirmableFields.isValid(Expense.class, command.missingFields())` compara contra os nomes literais dos atributos Java (`category`, `description`, `occurredOn`). Se a IA enviar `"amount"`, a validação diz que o campo é inválido; se enviar `"occurredOnDate"`, também é inválido.
* **Como melhorar:**
  Eliminar essa camada reflexiva frágil e unificar as regras de campos obrigatórios no próprio enum `ConfirmationField` ou em um validador de domínio único.

---

### 4. Anti-pattern de Conexão de Banco em Chamadas Externas de IA

* **Onde está:** `AudioExpenseService.java`.
* **O problema:**
  O método `processAudio` está anotado com `@Transactional`. Dentro dele ocorrem duas chamadas remotas para APIs de IA externas (Google Gemini):
  1. Transcrição do áudio via Gemini (`transcriptionService.transcrever`);
  2. Execução do agente de raciocínio e ferramentas via Gemini (`expenseClient.executeAction`).
  
  Essas chamadas externas levam entre 3 a 15 segundos. Enquanto isso, a transação do banco fica aberta segurando uma conexão do pool (HikariCP). Bastam 10 a 20 requisições simultâneas de áudio para provocar **Connection Pool Exhaustion** e travar toda a aplicação.
* **Como melhorar:**
  Remover a anotação `@Transactional` do método orquestrador de áudio. Isolar as operações de banco em métodos transacionais curtos e pontuais antes e depois das chamadas de rede da IA.

---

### 5. Memória Conversacional Volátil e Sem Contexto de Sessão

* **Onde está:** `ChatMemoryConfig.java` e `ExpenseClient.java`.
* **O problema:**
  * **Volatilidade:** Utiliza `InMemoryChatMemoryRepository`. Um simples restart da aplicação apaga todo o histórico de conversas ativas.
  * **Ausência de Sessão (`sessionId` / `threadId`):** O ID da conversa é fixado como o próprio `userId`:
    ```java
    String conversationId = currentUserProvider.getCurrentUserId().toString();
    ```
    Isso significa que **todas** as conversas de toda a vida do usuário vão para a mesma janela deslizante de 30 mensagens. Se o usuário registrou um gasto ontem e vier registrar outro hoje, mensagens antigas de gastos já confirmados ou cancelados competem pelo contexto, induzindo a IA a alucinar e reaproveitar IDs de gastos passados.
* **Como melhorar:**
  * Persistir a memória conversacional no próprio PostgreSQL (via JDBC/JPA para `ChatMemory`).
  * Adicionar um identificador de sessão (`sessionId`) no `ChatRequest` para separar threads de conversa.

---

### 6. Engenharia de Prompts e Capacidade do Agente Muito Básica

* **Onde está:** `ChatClientConfig.java`.
* **O problema:**
  * O system prompt possui apenas 6 linhas genéricas:
    ```java
    "Você registra gastos usando as ferramentas disponíveis... Se a data do gasto não for citada, considere como hoje."
    ```
  * **Contradição de negócio:** O prompt diz para assumir a data como hoje se não for citada, mas o fluxo da arquitetura documentada diz que a data ausente deve gerar pergunta de confirmação.
  * **Falta de suporte a múltiplos gastos:** Frases do tipo *"Gastei 30 no almoço e 15 na farmácia"* não são orientadas no prompt.
  * **Ausência de Few-Shot e Guardrails:** Não há instruções contra prompt injection, mensagens fora de contexto ("Qual é a previsão do tempo?") ou mensagens ambíguas.
  * **DateTimeTools subutilizado:** O prompt não orienta o modelo a consultar `DateTimeTools` para resolver termos como *"ontem"*, *"segunda passada"* ou *"dia 5 deste mês"*.

---

### 7. Contrato das Ferramentas e Respostas da API (Texto vs JSON)

* **Onde está:** `FinanceTools.java` e `ChatResponse.java`.
* **O problema:**
  * **Ferramentas retornam texto plano para a IA:** As tools respondem com frases como `"Expense saved"`, `"Expense confirmed"`. A IA é obrigada a inferir o resultado por parsing de texto, o que aumenta alucinações. Ferramentas devem retornar objetos estruturados (ex.: `{ "status": "PENDING", "expenseId": "...", "missingFields": ["category"] }`).
  * **API responde apenas texto plano para o cliente:** O `ChatResponse` entrega apenas `message`. O frontend não recebe o `expenseId` nem o status da despesa manipulada, impedindo a renderização de **Cards Interativos** com botões rápidos de ação (`[Confirmar]`, `[Descartar]`, `[Editar]`).

---

### 8. Domínio Financeiro Ingênuo

* **Onde está:** `Money.java`, `Category.java` e `ExpenseService.java`.
* **O problema:**
  * **Valores negativos e datas absurdas:** Não há validação impedindo a IA de persistir valores `<= 0` ou datas no ano de 2099 / 1850.
  * **Meio de Pagamento ausente:** No mundo real, além de categoria e valor, uma gestão financeira exige o método de pagamento (Cartão de Crédito, Débito, Pix, Dinheiro).
  * **Mistura de Moedas:** Se o usuário cadastrar um gasto em `USD` e outro em `BRL`, o endpoint de resumo financeiro (`getExpenseSummary`) soma os valores diretamente sem conversão de câmbio nem segregação por moeda.

---

### 9. Frontend e Voz (Lacuna de Implementação)

* **Onde está:** `app.js` e `index.html`.
* **O problema:**
  Apesar de a aplicação se chamar **VoiceFinance** e possuir infraestrutura para áudio no backend, a interface web só possui um formulário de texto para o chat. Não há botão de microfone usando a API nativa do navegador (`navigator.mediaDevices.getUserMedia` / `MediaRecorder`) para gravar e enviar o áudio via multipart para `AudioController`.

---

### 📋 Resumo das Prioridades de Evolução

| Prioridade | Área | Ação Recomendada |
| :--- | :--- | :--- |
| **P0 (Crítica)** | Segurança | Adicionar validação de `userId` em todas as ferramentas do `ExpenseService` chamadas pelo agente. |
| **P0 (Crítica)** | Resiliência | Remover `@Transactional` do fluxo de áudio em `AudioExpenseService` para evitar esgotamento de conexões. |
| **P1 (Alta)** | Agente & Prompts | Enriquecer o System Prompt com few-shots, tratamento de múltiplos gastos e orientações claras de uso do `DateTimeTools`. |
| **P1 (Alta)** | Ciclo de Vida | Unificar validação de campos pendentes e adotar rascunhos ou rotina de expiração para gastos em `PENDING_CONFIRMATION`. |
| **P2 (Média)** | Memória | Migrar o `ChatMemory` para repositório persistente no banco de dados e suportar `sessionId`. |
| **P2 (Média)** | Contratos & UX | Retornar metadados estruturados no `ChatResponse` e implementar o botão de gravação de áudio no frontend. |
