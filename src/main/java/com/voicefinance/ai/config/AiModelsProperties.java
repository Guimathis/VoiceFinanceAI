package com.voicefinance.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "voicefinance.ai")
public record AiModelsProperties(
        @DefaultValue("gemini-3.5-transcription") String transcriptionModel,
        @DefaultValue("gemini-3.1-flash-lite") String agentModel
) {}

