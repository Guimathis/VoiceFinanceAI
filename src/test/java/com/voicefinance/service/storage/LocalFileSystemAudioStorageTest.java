package com.voicefinance.service.storage;

import com.voicefinance.config.AudioProperties;
import com.voicefinance.exception.AudioStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileSystemAudioStorageTest {

    @TempDir
    Path tempStorageDir;

    private LocalFileSystemAudioStorage storage;
    private AudioProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AudioProperties(
                tempStorageDir.toString(),
                10 * 1024 * 1024L,
                60,
                List.of("audio/mpeg", "audio/mp4")
        );
        storage = new LocalFileSystemAudioStorage(properties);
    }

    @Test
    @DisplayName("Deve salvar o arquivo em disco e retornar StoredAudio com caminho correto")
    void shouldSaveAudioFileSuccessfully() throws IOException {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] content = "conteudo binario de teste".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "gravacao.mp3", "audio/mpeg", content);

        StoredAudio stored = storage.save(captureId, userId, file, "mp3", "audio/mpeg");

        assertThat(stored).isNotNull();
        assertThat(stored.sizeBytes()).isEqualTo(content.length);
        assertThat(stored.detectedMimeType()).isEqualTo("audio/mpeg");
        assertThat(stored.originalFilename()).isEqualTo("gravacao.mp3");

        Path savedPath = Path.of(stored.storagePath());
        assertThat(Files.exists(savedPath)).isTrue();
        assertThat(Files.readAllBytes(savedPath)).isEqualTo(content);
    }

    @Test
    @DisplayName("Deve carregar o arquivo salvo via recurso Spring Resource")
    void shouldLoadSavedAudioResource() throws IOException {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] content = "audio data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "teste.m4a", "audio/mp4", content);

        StoredAudio stored = storage.save(captureId, userId, file, "m4a", "audio/mp4");
        Resource resource = storage.load(stored.storagePath());

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("Deve excluir o arquivo em disco com sucesso")
    void shouldDeleteAudioFileSuccessfully() {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "remover.mp3", "audio/mpeg", "dados".getBytes());

        StoredAudio stored = storage.save(captureId, userId, file, "mp3", "audio/mpeg");
        Path savedPath = Path.of(stored.storagePath());
        assertThat(Files.exists(savedPath)).isTrue();

        storage.delete(stored.storagePath());
        assertThat(Files.exists(savedPath)).isFalse();
    }

    @Test
    @DisplayName("Deve lançar AudioStorageException ao tentar salvar arquivo vazio")
    void shouldThrowExceptionWhenSavingEmptyFile() {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "vazio.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> storage.save(captureId, userId, emptyFile, "mp3", "audio/mpeg"))
                .isInstanceOf(AudioStorageException.class);
    }

    @Test
    @DisplayName("Deve lançar AudioStorageException ao carregar caminho inexistente")
    void shouldThrowExceptionWhenLoadingNonExistentFile() {
        String nonExistentPath = tempStorageDir.resolve("inexistente.mp3").toString();

        assertThatThrownBy(() -> storage.load(nonExistentPath))
                .isInstanceOf(AudioStorageException.class);
    }
}
