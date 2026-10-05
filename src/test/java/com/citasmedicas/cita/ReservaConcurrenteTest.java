package com.citasmedicas.cita;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Dos reservas simultaneas del mismo medico y el mismo horario.
 * Solo una debe ganar (201) y la otra debe recibir 409, gracias al bloqueo
 * pesimista del medico y a la restriccion UNIQUE (medico_id, fecha_hora, reserva_activa).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReservaConcurrenteTest {

    private static final UUID RECEPCION = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dosReservasSimultaneasDejanUnaSolaCitaGanadora() throws Exception {
        UUID medicoId = primerMedicoActivo();
        List<UUID> pacientes = primerosDosPacientes();
        String fechaHora = primerSlotLibre(medicoId);

        Map<String, Object> reserva = Map.of(
                "pacienteId", pacientes.get(0),
                "medicoId", medicoId,
                "fechaHora", fechaHora,
                "motivo", "Consulta de control");

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<ResponseEntity<String>> respuestas = enviarSimultaneamente(pool, salida, reserva, pacientes.get(0),
                    pacientes.get(1));
            List<Integer> codigos = respuestas.stream().map(ResponseEntity::getStatusCode).map(HttpStatusCode::value)
                    .toList();

            assertThat(codigos).containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());

            ResponseEntity<String> rechazado = respuestas.stream()
                    .filter(respuesta -> respuesta.getStatusCode() == HttpStatus.CONFLICT)
                    .findFirst()
                    .orElseThrow();
            assertThat(rechazado.getBody()).contains("HORARIO_OCUPADO");

            ResponseEntity<String> ganador = respuestas.stream()
                    .filter(respuesta -> respuesta.getStatusCode() == HttpStatus.CREATED)
                    .findFirst()
                    .orElseThrow();
            assertThat(ganador.getHeaders().getLocation()).isNotNull();

            assertThat(citasEnElHorario(medicoId, fechaHora)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private List<ResponseEntity<String>> enviarSimultaneamente(ExecutorService pool, CountDownLatch salida,
            Map<String, Object> reserva, UUID primerPaciente, UUID segundoPaciente) throws Exception {
        Callable<ResponseEntity<String>> primera = tareaDeReserva(salida, reserva, primerPaciente);
        Callable<ResponseEntity<String>> segunda = tareaDeReserva(salida, reserva, segundoPaciente);
        Future<ResponseEntity<String>> resultadoPrimero = pool.submit(primera);
        Future<ResponseEntity<String>> resultadoSegundo = pool.submit(segunda);
        salida.countDown();
        return List.of(resultadoPrimero.get(), resultadoSegundo.get());
    }

    private Callable<ResponseEntity<String>> tareaDeReserva(CountDownLatch salida, Map<String, Object> reserva,
            UUID pacienteId) {
        Map<String, Object> cuerpo = new java.util.HashMap<>(reserva);
        cuerpo.put("pacienteId", pacienteId);
        return () -> {
            salida.await();
            return rest.exchange("/api/v1/citas", HttpMethod.POST, new HttpEntity<>(cuerpo, encabezadosRecepcion()),
                    String.class);
        };
    }

    private List<UUID> primerosDosPacientes() throws Exception {
        JsonNode pacientes = objectMapper.readTree(
                rest.exchange("/api/v1/pacientes", HttpMethod.GET, new HttpEntity<>(encabezadosRecepcion()),
                        String.class).getBody());
        List<UUID> ids = new ArrayList<>();
        pacientes.forEach(nodo -> ids.add(UUID.fromString(nodo.get("id").asText())));
        assertThat(ids).hasSizeGreaterThanOrEqualTo(2);
        return ids;
    }

    private UUID primerMedicoActivo() throws Exception {
        JsonNode medicos = objectMapper.readTree(
                rest.exchange("/api/v1/medicos", HttpMethod.GET, new HttpEntity<>(encabezadosRecepcion()),
                        String.class).getBody());
        assertThat(medicos).isNotEmpty();
        return UUID.fromString(medicos.get(0).get("id").asText());
    }

    private String primerSlotLibre(UUID medicoId) throws Exception {
        for (int desplazamiento = 1; desplazamiento <= 7; desplazamiento++) {
            LocalDate fecha = LocalDate.now().plusDays(desplazamiento);
            ResponseEntity<String> respuesta = rest.exchange(
                    "/api/v1/citas/disponibilidad?medicoId=" + medicoId + "&fecha=" + fecha, HttpMethod.GET,
                    new HttpEntity<>(encabezadosRecepcion()), String.class);
            if (respuesta.getStatusCode() != HttpStatus.OK) {
                continue;
            }
            JsonNode disponibilidad = objectMapper.readTree(respuesta.getBody());
            for (JsonNode slot : disponibilidad.get("slots")) {
                if (slot.get("disponible").asBoolean()) {
                    return slot.get("inicio").asText();
                }
            }
        }
        throw new IllegalStateException("El medico de la prueba no tiene slots libres en los proximos 7 dias");
    }

    private int citasEnElHorario(UUID medicoId, String fechaHora) throws Exception {
        JsonNode citas = objectMapper.readTree(rest.exchange("/api/v1/citas?medicoId=" + medicoId, HttpMethod.GET,
                new HttpEntity<>(encabezadosRecepcion()), String.class).getBody());
        int total = 0;
        for (JsonNode cita : citas) {
            if (fechaHora.equals(cita.get("fechaHora").asText())) {
                total++;
            }
        }
        return total;
    }

    private HttpHeaders encabezadosRecepcion() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Usuario-Id", RECEPCION.toString());
        headers.set("X-Rol", "RECEPCION");
        return headers;
    }
}