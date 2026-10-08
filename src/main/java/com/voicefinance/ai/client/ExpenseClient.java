package com.voicefinance.ai.client;

import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.domain.dto.ChatResponse;
import com.voicefinance.tools.FinanceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ExpenseClient {

    private final ChatClient chatClient;

    public ExpenseClient(@Qualifier("agentClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse executeAction(ChatRequest request) {
        String response = chatClient.prompt(request.action())
                .call()
                .content();
        return new ChatResponse(response);
    }

}
