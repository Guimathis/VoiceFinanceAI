package com.voicefinance.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(name = "securityCurrentUserProvider")
public class DevCurrentUserProvider implements CurrentUserProvider {

    // UUID mock já existente no db
    private static final UUID DEFAULT_DEV_USER_ID = UUID.fromString("e9d65866-55c1-4eeb-9658-6655c15eeba4");

    @Override
    public UUID getCurrentUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserId = request.getHeader("X-User-Id");
            if (headerUserId != null && !headerUserId.isBlank()) {
                try {
                    return UUID.fromString(headerUserId);
                } catch (IllegalArgumentException ignored) {
                    // Se o header for inválido, cai no fallback
                }
            }
        }

        return DEFAULT_DEV_USER_ID;
    }
}