package com.voicefinance.ai.config;

import com.voicefinance.ai.advisors.SimpleLoggerAdvisor;
import com.voicefinance.ai.tools.DateTimeTools;
import com.voicefinance.ai.tools.FinanceTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(AiModelsProperties.class)
@RequiredArgsConstructor
class ChatClientConfig {

    private final DateTimeTools dateTimeTools;
    private final FinanceTools financeTools;
    private final SimpleLoggerAdvisor simpleLoggerAdvisor;

    @Bean
    ChatClient transcriptionClient(ChatClient.Builder builder, AiModelsProperties props) {
        return builder
                .defaultSystem("Você transcreve áudio em português, literalmente. "
                        + "Responda só com a transcrição.")
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(props.transcriptionModel())
                        .temperature(0.0))
                .defaultTools(dateTimeTools)
                .defaultAdvisors(
                        simpleLoggerAdvisor
                )
                .build();
    }

    @Bean
    @Primary
    ChatClient agentClient(ChatClient.Builder builder, AiModelsProperties props, ChatMemory memory) {
        return builder
                .defaultSystem("Você registra gastos usando as ferramentas disponíveis. " +
                        "Você recebe uma solicitação de gastos tem o objetivo de registrar o gasto seguindo os padrões definidos. " +
                        "Quando não tiver todas as informações necessárias, " +
                        "Você deve criar confirmações sobre o gasto usando a ferramenta createExpenseFieldConfirmation. " +
                        "Quando o usuário confirmar a despesa, use confirmExpense. " +
                        "Quando o usuário cancelar a despesa, use cancelExpense. " +
                        "Se a data do gasto não for citada, considere como hoje.")
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(props.agentModel())
                        .temperature(0.2))
                .defaultTools(dateTimeTools, financeTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(memory).order(0).build(),
                        simpleLoggerAdvisor
                )
                .build();
    }
}
