package com.voicefinance.service.storage;

public record StoredAudio(
        String storagePath,
        long sizeBytes,
        String originalFilename,
        String detectedMimeType
) {}
