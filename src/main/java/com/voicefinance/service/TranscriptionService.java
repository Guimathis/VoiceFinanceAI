package com.voicefinance.service;

import com.voicefinance.exception.InaudibleAudioException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.Set;

@Slf4j
@Service
public class TranscriptionService {

    private final ChatClient chatClient;

    private static final Set<String> NOISE_PATTERNS = Set.of(
            "...",
            "..",
            ".",
            "[silêncio]",
            "[silencio]",
            "[ruído]",
            "[ruido]",
            "[música]",
            "[musica]",
            "[inaudível]",
            "[inaudivel]",
            "áudio inaudível",
            "audio inaudivel"
    );

    public TranscriptionService(@Qualifier("transcriptionClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Retry(name = "transcriptionRetry")
    @CircuitBreaker(name = "transcriptionCircuit")
    public String transcrever(Resource audio, String mimeType) {
        log.info("Iniciando transcrição de áudio com mimeType: {}", mimeType);

        String rawContent = chatClient.prompt()
                .user(u -> u
                        .text("Transcreva este áudio em português brasileiro, literalmente. "
                                + "Responda apenas com a transcrição textual, sem comentários.")
                        .media(MimeTypeUtils.parseMimeType(mimeType), audio))
                .call()
                .content();

        if (rawContent == null || rawContent.isBlank()) {
            log.warn("Transcrição retornou vazia ou nula pelo modelo");
            throw new InaudibleAudioException();
        }

        String cleaned = rawContent.trim();
        String lower = cleaned.toLowerCase();

        if (NOISE_PATTERNS.contains(lower)) {
            log.warn("Transcrição detectou apenas ruído ou silêncio: '{}'", cleaned);
            throw new InaudibleAudioException();
        }

        log.info("Transcrição realizada com sucesso: '{}'", cleaned);
        return cleaned;
    }
}