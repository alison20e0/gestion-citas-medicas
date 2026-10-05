package com.citasmedicas.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.citasmedicas.seguridad.ContextoUsuario;
import com.citasmedicas.seguridad.UsuarioActual;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    public static final String USUARIO_ANONIMO = "sistema";

    private final ContextoUsuario contextoUsuario;

    public JpaAuditingConfig(ContextoUsuario contextoUsuario) {
        this.contextoUsuario = contextoUsuario;
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> contextoUsuario.opcional()
                .map(UsuarioActual::etiqueta)
                .or(() -> Optional.of(USUARIO_ANONIMO));
    }
}