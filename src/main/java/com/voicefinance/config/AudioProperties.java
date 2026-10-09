package com.voicefinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "voicefinance.audio")
public record AudioProperties(
        @DefaultValue("./storage/audio") String storageDir,
        @DefaultValue("10485760") long maxSizeBytes,
        @DefaultValue("60") int maxDurationSeconds,
        @DefaultValue({
                "audio/mpeg",
                "audio/mp3",
                "audio/wav",
                "audio/x-wav",
                "audio/ogg",
                "audio/webm",
                "audio/mp4",
                "audio/x-m4a",
                "audio/aac"
        }) List<String> allowedContentTypes
) {}
