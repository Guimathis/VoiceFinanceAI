package com.voicefinance.controller;

import com.voicefinance.domain.dto.AudioChatResponse;
import com.voicefinance.exception.*;
import com.voicefinance.service.audio.AudioExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AudioControllerTest {

    @Mock
    private AudioExpenseService audioExpenseService;

    @InjectMocks
    private AudioController audioController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(audioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/voice/audio - Deve processar áudio com sucesso e retornar AudioChatResponse")
    void shouldProcessAudioSuccessfully() throws Exception {
        UUID captureId = UUID.randomUUID();
        AudioChatResponse response = new AudioChatResponse(
                captureId,
                "Almocei no restaurante hoje, deu 45 reais",
                "Gasto de R$ 45,00 em Alimentação registrado com sucesso! Posso confirmar?",
                BigDecimal.valueOf(4.15),
                205721L,
                "audio/mpeg",
                Instant.now()
        );

        when(audioExpenseService.processAudio(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "audio.mp3",
                "audio/mpeg",
                "dummy audio content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/voice/audio").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captureId").value(captureId.toString()))
                .andExpect(jsonPath("$.transcript").value("Almocei no restaurante hoje, deu 45 reais"))
                .andExpect(jsonPath("$.message").value("Gasto de R$ 45,00 em Alimentação registrado com sucesso! Posso confirmar?"))
                .andExpect(jsonPath("$.durationSeconds").value(4.15))
                .andExpect(jsonPath("$.contentType").value("audio/mpeg"));
    }

    @Test
    @DisplayName("POST /api/v1/voice/audio - Deve retornar 415 quando o formato do áudio for inválido")
    void shouldReturn415WhenInvalidAudioFormat() throws Exception {
        when(audioExpenseService.processAudio(any())).thenThrow(new InvalidAudioFormatException());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "executavel.exe",
                "application/octet-stream",
                "MZ header".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/voice/audio").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.errorCode").value("AUDIO_FORMAT_UNSUPPORTED"));
    }

    @Test
    @DisplayName("POST /api/v1/voice/audio - Deve retornar 413 quando o áudio exceder o tamanho limite")
    void shouldReturn413WhenAudioTooLarge() throws Exception {
        when(audioExpenseService.processAudio(any())).thenThrow(new AudioTooLargeException(15000000L, 10485760L));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gigante.mp3",
                "audio/mpeg",
                "conteudo grande".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/voice/audio").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.title").value("Payload Too Large"))
                .andExpect(jsonPath("$.errorCode").value("AUDIO_TOO_LARGE"));
    }

    @Test
    @DisplayName("POST /api/v1/voice/audio - Deve retornar 400 quando o arquivo de áudio estiver vazio")
    void shouldReturn400WhenAudioIsEmpty() throws Exception {
        when(audioExpenseService.processAudio(any())).thenThrow(new EmptyAudioException());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vazio.mp3",
                "audio/mpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/voice/audio").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Empty Audio"))
                .andExpect(jsonPath("$.errorCode").value("AUDIO_EMPTY"));
    }

    @Test
    @DisplayName("POST /api/v1/voice/audio - Deve retornar 400 quando o áudio for inaudível ou sem fala")
    void shouldReturn400WhenAudioIsInaudible() throws Exception {
        when(audioExpenseService.processAudio(any())).thenThrow(new InaudibleAudioException());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "silencio.mp3",
                "audio/mpeg",
                "silence".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/voice/audio").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Transcription Inaudible"))
                .andExpect(jsonPath("$.errorCode").value("AUDIO_INAUDIBLE"));
    }

    @Test
    @DisplayName("POST /api/v1/voice/audio - Deve retornar 400 quando a duração do áudio exceder o teto permitido")
    void shouldReturn400WhenDurationExceeded() throws Exception {
        when(audioExpenseService.processAudio(any()))
                .thenThrow(new AudioDurationExceededException(BigDecimal.valueOf(75.5), 60));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "muito_longo.mp3",
                "audio/mpeg",
                "long audio".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/voice/audio").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Audio Duration Exceeded"))
                .andExpect(jsonPath("$.errorCode").value("AUDIO_DURATION_EXCEEDED"));
    }
}
