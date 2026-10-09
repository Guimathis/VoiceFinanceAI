package com.voicefinance.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AudioStorage {

    StoredAudio save(UUID captureId, UUID userId, MultipartFile file, String extension, String mimeType);

    Resource load(String storagePath);

    void delete(String storagePath);
}
