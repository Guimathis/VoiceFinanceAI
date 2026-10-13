package com.voicefinance.service.storage;

import com.voicefinance.config.AudioProperties;
import com.voicefinance.exception.AudioStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileSystemAudioStorage implements AudioStorage {

    private final AudioProperties audioProperties;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public StoredAudio save(UUID captureId, UUID userId, MultipartFile file, String extension, String mimeType) {
        if (file == null || file.isEmpty()) {
            throw new AudioStorageException("Não é possível salvar um arquivo de áudio vazio.");
        }

        try {
            String safeExtension = sanitizeExtension(extension);
            String yearMonth = LocalDate.now().format(YEAR_MONTH_FORMATTER);

            Path baseDir = Paths.get(audioProperties.storageDir()).toAbsolutePath().normalize();
            Path userDir = baseDir.resolve(userId.toString()).resolve(yearMonth).normalize();

            // Proteção contra path traversal
            if (!userDir.startsWith(baseDir)) {
                throw new AudioStorageException("Tentativa de gravação fora do diretório permitido.");
            }

            Files.createDirectories(userDir);

            String fileName = captureId.toString() + "." + safeExtension;
            Path targetFile = userDir.resolve(fileName).normalize();

            if (!targetFile.startsWith(userDir)) {
                throw new AudioStorageException("Caminho de arquivo inválido para gravação de áudio.");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                    ? file.getOriginalFilename()
                    : fileName;

            log.info("Áudio armazenado com sucesso em: {}", targetFile);

            return new StoredAudio(
                    targetFile.toString(),
                    file.getSize(),
                    originalFilename,
                    mimeType
            );

        } catch (IOException e) {
            log.error("Falha ao salvar arquivo de áudio em disco: {}", e.getMessage(), e);
            throw new AudioStorageException("Falha ao gravar arquivo de áudio no sistema de arquivos local.", e);
        }
    }

    @Override
    public Resource load(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new AudioStorageException("Caminho do arquivo não fornecido.");
        }

        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        Path baseDir = Paths.get(audioProperties.storageDir()).toAbsolutePath().normalize();

        if (!path.startsWith(baseDir) || !Files.exists(path)) {
            throw new AudioStorageException("Arquivo de áudio não encontrado no caminho especificado: " + storagePath);
        }

        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            Path baseDir = Paths.get(audioProperties.storageDir()).toAbsolutePath().normalize();

            if (path.startsWith(baseDir)) {
                Files.deleteIfExists(path);
                log.info("Arquivo de áudio excluído com sucesso: {}", path);
            }
        } catch (IOException e) {
            log.warn("Falha ao excluir arquivo de áudio {}: {}", storagePath, e.getMessage());
        }
    }

    private String sanitizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "audio";
        }
        String cleaned = extension.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleaned.isEmpty() ? "audio" : cleaned;
    }
}
