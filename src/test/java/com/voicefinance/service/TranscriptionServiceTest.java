package com.voicefinance.service;

import com.voicefinance.exception.InaudibleAudioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

    @Mock
    private ChatClient chatClient;

    private TranscriptionService transcriptionService;

    @BeforeEach
    void setUp() {
        transcriptionService = new TranscriptionService(chatClient);
    }

    @Test
    @DisplayName("Deve transcrever áudio com sucesso retornando texto literal limpo")
    void shouldTranscribeAudioSuccessfully() {
        Resource audioResource = new ByteArrayResource("audio-bytes".getBytes());
        String expectedTranscription = "Gastei 45 reais no almoço hoje";

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("  " + expectedTranscription + "  ");

        String result = transcriptionService.transcrever(audioResource, "audio/mpeg");

        assertThat(result).isEqualTo(expectedTranscription);
    }

    @Test
    @DisplayName("Deve lançar InaudibleAudioException se a transcrição retornar em branco")
    void shouldThrowInaudibleAudioExceptionWhenTranscriptionIsBlank() {
        Resource audioResource = new ByteArrayResource("audio-bytes".getBytes());

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("   ");

        assertThatThrownBy(() -> transcriptionService.transcrever(audioResource, "audio/mpeg"))
                .isInstanceOf(InaudibleAudioException.class);
    }

    @Test
    @DisplayName("Deve lançar InaudibleAudioException se a transcrição retornar indicador de silêncio ou ruído")
    void shouldThrowInaudibleAudioExceptionWhenTranscriptionIsNoise() {
        Resource audioResource = new ByteArrayResource("audio-bytes".getBytes());

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("[silêncio]");

        assertThatThrownBy(() -> transcriptionService.transcrever(audioResource, "audio/mpeg"))
                .isInstanceOf(InaudibleAudioException.class);
    }
}
