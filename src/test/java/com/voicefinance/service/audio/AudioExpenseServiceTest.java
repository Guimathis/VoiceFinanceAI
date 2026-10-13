package com.voicefinance.service.audio;

import com.voicefinance.ai.client.ExpenseClient;
import com.voicefinance.domain.dto.AudioChatResponse;
import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.domain.dto.ChatResponse;
import com.voicefinance.domain.entity.AudioCapture;
import com.voicefinance.repository.AudioCaptureRepository;
import com.voicefinance.security.CurrentUserProvider;
import com.voicefinance.service.TranscriptionService;
import com.voicefinance.service.storage.AudioStorage;
import com.voicefinance.service.storage.StoredAudio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioExpenseServiceTest {

    @Mock
    private AudioMediaValidator audioMediaValidator;

    @Mock
    private AudioStorage audioStorage;

    @Mock
    private AudioCaptureRepository audioCaptureRepository;

    @Mock
    private TranscriptionService transcriptionService;

    @Mock
    private ExpenseClient expenseClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AudioExpenseService audioExpenseService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    @DisplayName("Deve orquestrar com sucesso validação, armazenamento, transcrição e diálogo com o agente")
    void shouldOrchestrateAudioProcessingSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "audio.mp3", "audio/mpeg", "teste".getBytes());
        ValidatedAudioMetadata metadata = new ValidatedAudioMetadata(
                "audio/mpeg",
                "mp3",
                4L,
                BigDecimal.valueOf(3.5),
                "audio.mp3"
        );
        StoredAudio stored = new StoredAudio(
                "/storage/audio/audio.mp3",
                4L,
                "audio.mp3",
                "audio/mpeg"
        );

        when(audioMediaValidator.validate(file)).thenReturn(metadata);
        when(audioStorage.save(any(UUID.class), eq(userId), eq(file), eq("mp3"), eq("audio/mpeg"))).thenReturn(stored);
        when(audioStorage.load(stored.storagePath())).thenReturn(new ByteArrayResource("teste".getBytes()));
        when(transcriptionService.transcrever(any(), eq("audio/mpeg"))).thenReturn("Gastei 50 reais no almoço");
        when(expenseClient.executeAction(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("Gasto de R$ 50,00 registrado com sucesso!"));

        AudioChatResponse response = audioExpenseService.processAudio(file);

        assertThat(response).isNotNull();
        assertThat(response.captureId()).isNotNull();
        assertThat(response.transcript()).isEqualTo("Gastei 50 reais no almoço");
        assertThat(response.message()).isEqualTo("Gasto de R$ 50,00 registrado com sucesso!");
        assertThat(response.durationSeconds()).isEqualTo(BigDecimal.valueOf(3.5));
        assertThat(response.contentType()).isEqualTo("audio/mpeg");

        // Verifica que AudioCapture foi salvo com o transcript
        ArgumentCaptor<AudioCapture> captureCaptor = ArgumentCaptor.forClass(AudioCapture.class);
        verify(audioCaptureRepository, atLeastOnce()).save(captureCaptor.capture());
        AudioCapture savedCapture = captureCaptor.getValue();
        assertThat(savedCapture.getTranscript()).isEqualTo("Gastei 50 reais no almoço");
        assertThat(savedCapture.getUserId()).isEqualTo(userId);
    }
}
