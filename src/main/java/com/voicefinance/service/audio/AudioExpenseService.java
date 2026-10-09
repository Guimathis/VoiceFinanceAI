package com.voicefinance.service.audio;

import com.voicefinance.ai.client.ExpenseClient;
import com.voicefinance.context.CaptureContextHolder;
import com.voicefinance.domain.dto.AudioChatResponse;
import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.domain.dto.ChatResponse;
import com.voicefinance.domain.entity.AudioCapture;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.repository.AudioCaptureRepository;
import com.voicefinance.security.CurrentUserProvider;
import com.voicefinance.service.TranscriptionService;
import com.voicefinance.service.storage.AudioStorage;
import com.voicefinance.service.storage.StoredAudio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioExpenseService {

    private final AudioMediaValidator audioMediaValidator;
    private final AudioStorage audioStorage;
    private final AudioCaptureRepository audioCaptureRepository;
    private final TranscriptionService transcriptionService;
    private final ExpenseClient expenseClient;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public AudioChatResponse processAudio(MultipartFile file) {
        UUID userId = currentUserProvider.getCurrentUserId();
        log.info("Processando áudio para userId={}, arquivo={}", userId, file.getOriginalFilename());

        ValidatedAudioMetadata metadata = audioMediaValidator.validate(file);
        UUID captureId = UUID.randomUUID();

        StoredAudio stored = audioStorage.save(
                captureId,
                userId,
                file,
                metadata.extension(),
                metadata.detectedMimeType()
        );

        AudioCapture capture = AudioCapture.builder()
                .id(captureId)
                .userId(userId)
                .contentType(metadata.detectedMimeType())
                .sizeBytes(metadata.sizeBytes())
                .durationSeconds(metadata.durationSeconds())
                .originalFilename(metadata.originalFilename())
                .storagePath(stored.storagePath())
                .createdAt(Instant.now())
                .build();
        audioCaptureRepository.save(capture);

        Resource audioResource = audioStorage.load(stored.storagePath());
        String transcript = transcriptionService.transcrever(audioResource, metadata.detectedMimeType());

        capture.setTranscript(transcript);
        audioCaptureRepository.save(capture);

        CaptureContextHolder.set(captureId, ExpenseOrigin.AUDIO);
        try {
            ChatResponse chatResponse = expenseClient.executeAction(new ChatRequest(transcript));
            log.info("Processamento de áudio finalizado com sucesso para captureId={}", captureId);

            return new AudioChatResponse(
                    captureId,
                    transcript,
                    chatResponse.message(),
                    metadata.durationSeconds(),
                    metadata.sizeBytes(),
                    metadata.detectedMimeType(),
                    capture.getCreatedAt()
            );
        } finally {
            CaptureContextHolder.clear();
        }
    }
}
