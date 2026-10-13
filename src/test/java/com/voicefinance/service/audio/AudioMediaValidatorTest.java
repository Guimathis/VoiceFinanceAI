package com.voicefinance.service.audio;

import com.voicefinance.config.AudioProperties;
import com.voicefinance.exception.AudioDurationExceededException;
import com.voicefinance.exception.AudioTooLargeException;
import com.voicefinance.exception.EmptyAudioException;
import com.voicefinance.exception.InvalidAudioFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioMediaValidatorTest {

    private AudioMediaValidator validator;
    private AudioProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AudioProperties(
                "./target/test-storage/audio",
                10 * 1024 * 1024L,
                60,
                List.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/webm", "audio/mp4", "audio/x-m4a", "audio/aac")
        );
        validator = new AudioMediaValidator(properties);
    }

    @Test
    @DisplayName("Deve validar com sucesso o arquivo de teste real assets/audio.mp3")
    void shouldValidateRealAudioAssetSuccessfully() throws IOException {
        File file = new File("assets/audio.mp3");
        assertThat(file).exists();

        byte[] content = Files.readAllBytes(file.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "audio.mp3",
                "audio/mpeg",
                content
        );

        ValidatedAudioMetadata metadata = validator.validate(multipartFile);

        assertThat(metadata).isNotNull();
        assertThat(metadata.sizeBytes()).isEqualTo(content.length);
        assertThat(metadata.detectedMimeType()).isEqualTo("audio/mp4");
        assertThat(metadata.extension()).isEqualTo("m4a");
        assertThat(metadata.durationSeconds()).isNotNull();
        assertThat(metadata.durationSeconds().doubleValue()).isLessThanOrEqualTo(60.0);
    }

    @Test
    @DisplayName("Deve validar com sucesso áudio com cabeçalho WAV legítimo")
    void shouldValidateWavAudioSuccessfully() {
        byte[] wavHeader = new byte[44];
        // RIFF
        wavHeader[0] = 'R'; wavHeader[1] = 'I'; wavHeader[2] = 'F'; wavHeader[3] = 'F';
        // WAVE
        wavHeader[8] = 'W'; wavHeader[9] = 'A'; wavHeader[10] = 'V'; wavHeader[11] = 'E';
        // fmt
        wavHeader[12] = 'f'; wavHeader[13] = 'm'; wavHeader[14] = 't'; wavHeader[15] = ' ';
        // byteRate = 32000 (offset 28)
        wavHeader[28] = (byte) 0x00; wavHeader[29] = (byte) 0x7D; wavHeader[30] = 0x00; wavHeader[31] = 0x00;
        // data chunk (offset 36)
        wavHeader[36] = 'd'; wavHeader[37] = 'a'; wavHeader[38] = 't'; wavHeader[39] = 'a';
        // dataSize = 64000 (offset 40) -> 2.0 segundos
        wavHeader[40] = (byte) 0x00; wavHeader[41] = (byte) 0xFA; wavHeader[42] = 0x00; wavHeader[43] = 0x00;

        MockMultipartFile file = new MockMultipartFile("file", "teste.wav", "audio/wav", wavHeader);

        ValidatedAudioMetadata metadata = validator.validate(file);

        assertThat(metadata.detectedMimeType()).isEqualTo("audio/wav");
        assertThat(metadata.extension()).isEqualTo("wav");
        assertThat(metadata.durationSeconds().doubleValue()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Deve validar com sucesso áudio com assinatura ID3 de MP3")
    void shouldValidateMp3WithId3Tag() {
        byte[] mp3Bytes = new byte[128];
        mp3Bytes[0] = 'I'; mp3Bytes[1] = 'D'; mp3Bytes[2] = '3';
        mp3Bytes[3] = 0x03; mp3Bytes[4] = 0x00;

        MockMultipartFile file = new MockMultipartFile("file", "gravacao.mp3", "audio/mpeg", mp3Bytes);

        ValidatedAudioMetadata metadata = validator.validate(file);

        assertThat(metadata.detectedMimeType()).isEqualTo("audio/mpeg");
        assertThat(metadata.extension()).isEqualTo("mp3");
    }

    @Test
    @DisplayName("Deve validar com sucesso áudio com cabeçalho OGG")
    void shouldValidateOggAudio() {
        byte[] oggBytes = new byte[64];
        oggBytes[0] = 'O'; oggBytes[1] = 'g'; oggBytes[2] = 'g'; oggBytes[3] = 'S';

        MockMultipartFile file = new MockMultipartFile("file", "fala.ogg", "audio/ogg", oggBytes);

        ValidatedAudioMetadata metadata = validator.validate(file);

        assertThat(metadata.detectedMimeType()).isEqualTo("audio/ogg");
        assertThat(metadata.extension()).isEqualTo("ogg");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo nulo ou vazio com EmptyAudioException")
    void shouldThrowEmptyAudioExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "vazio.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validate(emptyFile))
                .isInstanceOf(EmptyAudioException.class);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo que ultrapassa o tamanho máximo permitido")
    void shouldThrowAudioTooLargeExceptionWhenFileExceedsLimit() {
        AudioProperties smallLimitProps = new AudioProperties(
                "./storage",
                100L,
                60,
                List.of("audio/mpeg")
        );
        AudioMediaValidator smallValidator = new AudioMediaValidator(smallLimitProps);

        byte[] bigContent = new byte[200];
        bigContent[0] = 'I'; bigContent[1] = 'D'; bigContent[2] = '3';
        MockMultipartFile bigFile = new MockMultipartFile("file", "grande.mp3", "audio/mpeg", bigContent);

        assertThatThrownBy(() -> smallValidator.validate(bigFile))
                .isInstanceOf(AudioTooLargeException.class);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo com extensão .mp3 mas com conteúdo de texto simples")
    void shouldThrowInvalidAudioFormatExceptionWhenContentIsNotAudio() {
        byte[] textBytes = "Este é um arquivo de texto fingindo ser áudio".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("file", "ataque.mp3", "audio/mpeg", textBytes);

        assertThatThrownBy(() -> validator.validate(fakeFile))
                .isInstanceOf(InvalidAudioFormatException.class);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo de áudio cuja duração ultrapassa o limite máximo")
    void shouldThrowAudioDurationExceededExceptionWhenDurationIsTooLong() {
        AudioProperties strictDurationProps = new AudioProperties(
                "./storage",
                10 * 1024 * 1024L,
                1, // máximo 1 segundo
                List.of("audio/wav")
        );
        AudioMediaValidator strictValidator = new AudioMediaValidator(strictDurationProps);

        byte[] wavHeader = new byte[44];
        wavHeader[0] = 'R'; wavHeader[1] = 'I'; wavHeader[2] = 'F'; wavHeader[3] = 'F';
        wavHeader[8] = 'W'; wavHeader[9] = 'A'; wavHeader[10] = 'V'; wavHeader[11] = 'E';
        // byteRate = 16000
        wavHeader[28] = (byte) 0x80; wavHeader[29] = (byte) 0x3E; wavHeader[30] = 0x00; wavHeader[31] = 0x00;
        wavHeader[36] = 'd'; wavHeader[37] = 'a'; wavHeader[38] = 't'; wavHeader[39] = 'a';
        // dataSize = 80000 -> 5.0 segundos (> 1 segundo)
        wavHeader[40] = (byte) 0x80; wavHeader[41] = (byte) 0x38; wavHeader[42] = (byte) 0x01; wavHeader[43] = 0x00;

        MockMultipartFile longAudio = new MockMultipartFile("file", "longo.wav", "audio/wav", wavHeader);

        assertThatThrownBy(() -> strictValidator.validate(longAudio))
                .isInstanceOf(AudioDurationExceededException.class);
    }
}
