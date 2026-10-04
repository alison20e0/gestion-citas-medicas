package com.citasmedicas.paciente.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.citasmedicas.shared.domain.BaseEntity;

@Entity
@Table(name = "paciente", uniqueConstraints = {
        @UniqueConstraint(name = "uk_paciente_documento", columnNames = "documento")
})
public class Paciente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "documento", nullable = false, length = 32)
    private String documento;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", length = 32)
    private String telefono;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    protected Paciente() {
    }

    public static Paciente crear(String documento, String nombres, String apellidos,
            LocalDate fechaNacimiento, String telefono, String email) {
        Paciente paciente = new Paciente();
        paciente.documento = documento;
        paciente.nombres = nombres;
        paciente.apellidos = apellidos;
        paciente.fechaNacimiento = fechaNacimiento;
        paciente.telefono = telefono;
        paciente.email = email;
        paciente.activo = true;
        return paciente;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public UUID getId() {
        return id;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActivo() {
        return activo;
    }

    public void desactivar() {
        this.activo = false;
    }
}