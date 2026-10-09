package com.voicefinance.ai.client;

import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.domain.dto.ChatResponse;
import com.voicefinance.security.CurrentUserProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ExpenseClient {

    private final ChatClient chatClient;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseClient(@Qualifier("agentClient") ChatClient chatClient, CurrentUserProvider currentUserProvider) {
        this.chatClient = chatClient;
        this.currentUserProvider = currentUserProvider;
    }

    public ChatResponse executeAction(ChatRequest request) {
        String conversationId = currentUserProvider.getCurrentUserId().toString();

        String response = chatClient.prompt(request.action())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return new ChatResponse(response);
    }

}
