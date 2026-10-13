package com.voicefinance.service.audio;

import com.voicefinance.config.AudioProperties;
import com.voicefinance.exception.AudioDurationExceededException;
import com.voicefinance.exception.AudioTooLargeException;
import com.voicefinance.exception.EmptyAudioException;
import com.voicefinance.exception.InvalidAudioFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioMediaValidator {

    private final AudioProperties audioProperties;

    public record DetectedFormat(String mimeType, String extension) {}

    public ValidatedAudioMetadata validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new EmptyAudioException();
        }

        if (file.getSize() > audioProperties.maxSizeBytes()) {
            throw new AudioTooLargeException(file.getSize(), audioProperties.maxSizeBytes());
        }

        byte[] headerBytes = readHeaderBytes(file, 4096);
        DetectedFormat detectedFormat = detectFormat(headerBytes);

        if (detectedFormat == null || !isContentTypeAllowed(detectedFormat.mimeType())) {
            log.warn("Formato de áudio rejeitado para arquivo: {}. Mime detectado: {}",
                    file.getOriginalFilename(), detectedFormat != null ? detectedFormat.mimeType() : "desconhecido");
            throw new InvalidAudioFormatException();
        }

        BigDecimal durationSeconds = extractDuration(file, detectedFormat, headerBytes);

        if (durationSeconds != null && durationSeconds.compareTo(BigDecimal.valueOf(audioProperties.maxDurationSeconds())) > 0) {
            log.warn("Duração do áudio excedida: {}s (limite: {}s)", durationSeconds, audioProperties.maxDurationSeconds());
            throw new AudioDurationExceededException(durationSeconds, audioProperties.maxDurationSeconds());
        }

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "audio." + detectedFormat.extension();

        return new ValidatedAudioMetadata(
                detectedFormat.mimeType(),
                detectedFormat.extension(),
                file.getSize(),
                durationSeconds,
                originalFilename
        );
    }

    public DetectedFormat detectFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }

        // 1. WAV (RIFF .... WAVE)
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E') {
            return new DetectedFormat("audio/wav", "wav");
        }

        // 2. OGG (OggS)
        if (bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S') {
            return new DetectedFormat("audio/ogg", "ogg");
        }

        // 3. WebM / Matroska (0x1A 0x45 0xDF 0xA3)
        if ((bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45
                && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3) {
            return new DetectedFormat("audio/webm", "webm");
        }

        // 4. MP4 / M4A (ftyp em offset 4)
        if (bytes.length >= 8 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
            return new DetectedFormat("audio/mp4", "m4a");
        }

        // 5. MP3 (ID3 tag)
        if (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') {
            return new DetectedFormat("audio/mpeg", "mp3");
        }

        // 6. MP3 (MPEG audio frame sync 11 bits 0xFFE)
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0) {
            return new DetectedFormat("audio/mpeg", "mp3");
        }

        // 7. AAC ADTS (0xFF 0xF0)
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xF6) == 0xF0) {
            return new DetectedFormat("audio/aac", "aac");
        }

        return null;
    }

    private boolean isContentTypeAllowed(String mimeType) {
        if (mimeType == null) return false;
        if (audioProperties.allowedContentTypes() == null || audioProperties.allowedContentTypes().isEmpty()) {
            return true;
        }
        for (String allowed : audioProperties.allowedContentTypes()) {
            if (allowed.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    private byte[] readHeaderBytes(MultipartFile file, int maxBytes) {
        try (InputStream in = file.getInputStream()) {
            byte[] buffer = new byte[maxBytes];
            int read = in.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }
            if (read < maxBytes) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buffer, 0, trimmed, 0, read);
                return trimmed;
            }
            return buffer;
        } catch (IOException e) {
            log.warn("Erro ao ler cabeçalho do arquivo de áudio: {}", e.getMessage());
            throw new InvalidAudioFormatException();
        }
    }

    private BigDecimal extractDuration(MultipartFile file, DetectedFormat format, byte[] headerBytes) {
        try {
            if ("audio/wav".equals(format.mimeType())) {
                BigDecimal dur = extractWavDuration(headerBytes, file.getSize());
                if (dur != null) return dur;
            }

            if ("audio/mp4".equals(format.mimeType())) {
                byte[] allBytes = file.getBytes();
                BigDecimal dur = extractMp4Duration(allBytes);
                if (dur != null) return dur;
            }

            if ("audio/mpeg".equals(format.mimeType())) {
                BigDecimal dur = extractMp3Duration(headerBytes, file.getSize());
                if (dur != null) return dur;
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair a duração exata do cabeçalho ({}), aplicando estimativa segura", e.getMessage());
        }

        // Estimativa padrão baseada em taxa média de 128 kbps (16 KB/s)
        double estimatedSeconds = (double) file.getSize() / 16000.0;
        return BigDecimal.valueOf(estimatedSeconds).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal extractWavDuration(byte[] headerBytes, long fileSize) {
        if (headerBytes.length < 36) return null;
        ByteBuffer bb = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
        int byteRate = bb.getInt(28);
        if (byteRate <= 0) return null;

        // Procura chunk 'data' nos cabeçalhos
        int dataSize = -1;
        for (int i = 12; i <= headerBytes.length - 8; i++) {
            if (headerBytes[i] == 'd' && headerBytes[i + 1] == 'a' && headerBytes[i + 2] == 't' && headerBytes[i + 3] == 'a') {
                dataSize = bb.getInt(i + 4);
                break;
            }
        }

        long actualDataSize = (dataSize > 0) ? dataSize : (fileSize - 44);
        if (actualDataSize <= 0) return null;
        double seconds = (double) actualDataSize / byteRate;
        return BigDecimal.valueOf(seconds).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal extractMp4Duration(byte[] bytes) {
        // Procura átomo 'mvhd' na estrutura ISOBMFF
        for (int i = 0; i <= bytes.length - 28; i++) {
            if (bytes[i] == 'm' && bytes[i + 1] == 'v' && bytes[i + 2] == 'h' && bytes[i + 3] == 'd') {
                int pos = i + 4;
                int version = bytes[pos] & 0xFF;
                ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
                long timeScale;
                long duration;

                if (version == 0) {
                    timeScale = Integer.toUnsignedLong(bb.getInt(pos + 12));
                    duration = Integer.toUnsignedLong(bb.getInt(pos + 16));
                } else if (version == 1) {
                    timeScale = Integer.toUnsignedLong(bb.getInt(pos + 20));
                    duration = bb.getLong(pos + 24);
                } else {
                    continue;
                }

                if (timeScale > 0 && duration > 0) {
                    double seconds = (double) duration / timeScale;
                    return BigDecimal.valueOf(seconds).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        return null;
    }

    private BigDecimal extractMp3Duration(byte[] headerBytes, long fileSize) {
        // Se houver ID3v2 tag, extrai tamanho da tag e calcula pela taxa do frame
        int id3Size = 0;
        if (headerBytes.length >= 10 && headerBytes[0] == 'I' && headerBytes[1] == 'D' && headerBytes[2] == '3') {
            id3Size = ((headerBytes[6] & 0x7F) << 21) |
                      ((headerBytes[7] & 0x7F) << 14) |
                      ((headerBytes[8] & 0x7F) << 7)  |
                      (headerBytes[9] & 0x7F);
            id3Size += 10;
        }

        long audioBytes = Math.max(0, fileSize - id3Size);
        if (audioBytes <= 0) return null;
        // Estimativa padrão para áudio de voz em MP3 (~64 kbps a 128 kbps)
        double seconds = (double) audioBytes / 16000.0;
        return BigDecimal.valueOf(seconds).setScale(2, RoundingMode.HALF_UP);
    }
}
