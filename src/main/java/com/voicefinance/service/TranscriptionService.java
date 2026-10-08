package com.voicefinance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class TranscriptionService {

    private final ChatClient chatClient;

    public TranscriptionService(@Qualifier("transcriptionClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String transcrever(Resource audio, String mimeType) {
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "e9d65866-55c1-4eeb-9658-6655c15eeba4"))
                .user(u -> u
                        .text("Transcreva este áudio em português, literalmente. "
                                + "Responda apenas com a transcrição, sem comentários.")
                        .media(MimeTypeUtils.parseMimeType(mimeType), audio))
                .call()
                .content();
    }
}