package com.citasmedicas.seguridad;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Roles autorizados a invocar el endpoint. Si no se anota, basta con estar
 * identificado. Un rol fuera de la lista recibe 403 y el intento queda
 * registrado en la auditoria de accesos denegados.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permitido {

    Rol[] value();
}