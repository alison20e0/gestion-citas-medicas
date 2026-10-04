package com.citasmedicas.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    public static final String USUARIO_ANONIMO = "sistema";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                            .filter(RequestAttributes.SCOPE_REQUEST::equals)
                            .orElse(null);
            if (attributes == null) {
                return Optional.of(USUARIO_ANONIMO);
            }
            String actor = attributes.getHeader(actorHeader());
            return Optional.ofNullable(actor)
                    .filter(valido -> !valido.isBlank())
                    .or(() -> Optional.of(USUARIO_ANONIMO));
        };
    }

    private String actorHeader() {
        return "X-Usuario";
    }
}