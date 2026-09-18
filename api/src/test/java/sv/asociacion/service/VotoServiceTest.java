package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.OpcionVotacionDAO;
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.dao.PersistenciaException;
import sv.asociacion.dao.VotoDAO;
import sv.asociacion.dao.VotoDuplicadoException;
import sv.asociacion.domain.dto.EmitirVotoRequest;
import sv.asociacion.domain.dto.EmitirVotoResponse;
import sv.asociacion.domain.dto.EstadoParticipacionResponse;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.OpcionVotacion;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.domain.entity.Voto;

class VotoServiceTest {
    private MemoryVotoDAO votoDAO;
    private MemoryVotacionDAO votacionDAO;
    private MemoryOpcionDAO opcionDAO;
    private MemoryMiembroDAO miembroDAO;
    private VotoService service;

    private Miembro miembroActivo;
    private Miembro miembroInactivo;
    private Votacion votacionAbierta;
    private Votacion votacionBorrador;
    private OpcionVotacion opcion1;
    private OpcionVotacion opcion2;
    private OpcionVotacion opcionOtraVotacion;

    @BeforeEach
    void setUp() {
        votoDAO = new MemoryVotoDAO();
        votacionDAO = new MemoryVotacionDAO();
        opcionDAO = new MemoryOpcionDAO();
        miembroDAO = new MemoryMiembroDAO();
        service = new VotoService(votoDAO, votacionDAO, opcionDAO, miembroDAO);

        miembroActivo = new Miembro();
        miembroActivo.setIdMiembro(1);
        miembroActivo.setNombres("Carlos");
        miembroActivo.setApellidos("Perez");
        miembroActivo.setEstado(Miembro.Estado.ACTIVO);
        miembroDAO.save(miembroActivo);

        miembroInactivo = new Miembro();
        miembroInactivo.setIdMiembro(2);
        miembroInactivo.setNombres("Maria");
        miembroInactivo.setApellidos("Lopez");
        miembroInactivo.setEstado(Miembro.Estado.INACTIVO);
        miembroDAO.save(miembroInactivo);

        votacionAbierta = new Votacion(10, null, "Presupuesto Comunal", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), Votacion.Estado.ABIERTA);
        votacionDAO.save(votacionAbierta);

        votacionBorrador = new Votacion(20, null, "Borrador sin abrir", LocalDateTime.now(), LocalDateTime.now().plusDays(2), Votacion.Estado.BORRADOR);
        votacionDAO.save(votacionBorrador);

        opcion1 = new OpcionVotacion(101, 10, "A favor", (short) 1);
        opcionDAO.save(opcion1);

        opcion2 = new OpcionVotacion(102, 10, "En contra", (short) 2);
        opcionDAO.save(opcion2);

        opcionOtraVotacion = new OpcionVotacion(201, 20, "Opción Borrador", (short) 1);
        opcionDAO.save(opcionOtraVotacion);
    }

    // El identificador del miembro viaja ahora como identidad de sesión, no en el
    // cuerpo. Las pruebas existentes conservan intactas sus aserciones; solo cambia de
    // sitio el dato, porque el contrato anterior permitía suplantar al votante.

    @Test
    void emitsVoteSuccessfullyWhenActiveAndOpen() {
        EmitirVotoRequest req = new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null);

        EmitirVotoResponse res = service.emitirVoto(req, miembroActivo.getIdMiembro());
        assertNotNull(res);
        assertNotNull(res.idVoto());
        assertEquals("Voto registrado exitosamente.", res.mensaje());
        assertEquals(1, votoDAO.countByOpcion(opcion1.getIdOpcion()));
        assertEquals(1, votoDAO.countByVotacion(votacionAbierta.getIdVotacion()));
    }

    @Test
    void rejectsVoteWhenMiembroIsInactive() {
        EmitirVotoRequest req = new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null);

        assertThrows(IllegalStateException.class, () -> service.emitirVoto(req, miembroInactivo.getIdMiembro()));
    }

    @Test
    void rejectsVoteWhenVotacionIsNotOpen() {
        EmitirVotoRequest req = new EmitirVotoRequest(votacionBorrador.getIdVotacion(), opcionOtraVotacion.getIdOpcion(), null);

        assertThrows(IllegalStateException.class, () -> service.emitirVoto(req, miembroActivo.getIdMiembro()));
    }

    @Test
    void rejectsVoteWhenOptionBelongsToDifferentVotacion() {
        // Opción pertenece a la votación 20, pero intentamos votar en la 10
        EmitirVotoRequest req = new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcionOtraVotacion.getIdOpcion(), null);

        assertThrows(IllegalArgumentException.class, () -> service.emitirVoto(req, miembroActivo.getIdMiembro()));
    }

    @Test
    void rejectsDuplicateVoteFromSameMember() {
        EmitirVotoRequest req1 = new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null);
        service.emitirVoto(req1, miembroActivo.getIdMiembro());

        // Segundo intento con otra opción en la misma votación
        EmitirVotoRequest req2 = new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion2.getIdOpcion(), null);
        assertThrows(IllegalStateException.class, () -> service.emitirVoto(req2, miembroActivo.getIdMiembro()));
    }

    @Test
    void verifiesParticipationCorrectly() {
        EstadoParticipacionResponse antes = service.verificarParticipacion(votacionAbierta.getIdVotacion(), miembroActivo.getIdMiembro());
        assertFalse(antes.yaVoto());

        service.emitirVoto(new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null), miembroActivo.getIdMiembro());

        EstadoParticipacionResponse despues = service.verificarParticipacion(votacionAbierta.getIdVotacion(), miembroActivo.getIdMiembro());
        assertTrue(despues.yaVoto());
        assertNotNull(despues.fechaHoraVoto());
    }

    // ------------------------------------------------------------------
    // SCRUM-273 — la identidad del votante la fija la sesión
    // ------------------------------------------------------------------

    @Test
    void jamasRegistraElVotoANombreDelMiembroDelBody() {
        // Sesión del miembro A (1), cuerpo con el miembro B (2). El voto no debe
        // registrarse como B bajo ninguna circunstancia.
        Integer miembroA = miembroActivo.getIdMiembro();
        Integer miembroB = miembroInactivo.getIdMiembro();
        EmitirVotoRequest suplantacion =
            new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), miembroB);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.emitirVoto(suplantacion, miembroA));
        assertTrue(error.getMessage().contains("en nombre de otro miembro"),
            "El mensaje debe señalar la suplantación: " + error.getMessage());

        // Ni como B, ni como A, ni de ninguna forma: no se registró nada.
        assertFalse(votoDAO.existsByVotacionAndMiembro(votacionAbierta.getIdVotacion(), miembroB),
            "No debe existir un voto a nombre del miembro suplantado.");
        assertFalse(votoDAO.existsByVotacionAndMiembro(votacionAbierta.getIdVotacion(), miembroA),
            "Tampoco debe registrarse a nombre del miembro de la sesión.");
        assertEquals(0, votoDAO.countByVotacion(votacionAbierta.getIdVotacion()));
    }

    @Test
    void aceptaElBodyCuandoCoincideConLaSesion() {
        EmitirVotoRequest req = new EmitirVotoRequest(
            votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), miembroActivo.getIdMiembro());

        EmitirVotoResponse res = service.emitirVoto(req, miembroActivo.getIdMiembro());
        assertNotNull(res.idVoto());
        assertTrue(votoDAO.existsByVotacionAndMiembro(votacionAbierta.getIdVotacion(), miembroActivo.getIdMiembro()));
    }

    @Test
    void usaSiempreLaIdentidadDeSesionCuandoElBodyNoTraeMiembro() {
        service.emitirVoto(
            new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null),
            miembroActivo.getIdMiembro());

        assertTrue(votoDAO.existsByVotacionAndMiembro(votacionAbierta.getIdVotacion(), miembroActivo.getIdMiembro()));
    }

    @Test
    void rechazaVotoSinSesionConMiembroAsociado() {
        // Aunque el cuerpo traiga un identificador válido, sin sesión no hay voto.
        EmitirVotoRequest req = new EmitirVotoRequest(
            votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), miembroActivo.getIdMiembro());

        assertThrows(IllegalArgumentException.class, () -> service.emitirVoto(req, null));
        assertEquals(0, votoDAO.countByVotacion(votacionAbierta.getIdVotacion()));
    }

    // ------------------------------------------------------------------
    // SCRUM-273 — la restricción de unicidad es la autoridad
    // ------------------------------------------------------------------

    @Test
    void traduceLaViolacionDeUnicidadAConflictoDeNegocio() {
        // Simula la carrera que gana la base de datos: el chequeo previo no ve nada,
        // pero la restricción uk_voto_votacion_miembro rechaza la inserción.
        votoDAO.simularDuplicadoEnRegistro = true;

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.emitirVoto(
                new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null),
                miembroActivo.getIdMiembro()));

        assertTrue(error.getMessage().contains("ya ha emitido su voto"),
            "Debe presentarse como conflicto de negocio: " + error.getMessage());
    }

    @Test
    void propagaElFalloDePersistenciaEnLugarDeInformarExito() {
        votoDAO.simularFalloDePersistencia = true;

        assertThrows(PersistenciaException.class,
            () -> service.emitirVoto(
                new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null),
                miembroActivo.getIdMiembro()));
    }

    @Test
    void elVotoRegistradoSiempreLlevaIdentificador() {
        // Guardián contra el voto fantasma: antes, si la inserción fallaba, el DAO
        // devolvía la entidad sin id y la API respondía 201.
        EmitirVotoResponse res = service.emitirVoto(
            new EmitirVotoRequest(votacionAbierta.getIdVotacion(), opcion1.getIdOpcion(), null),
            miembroActivo.getIdMiembro());

        assertNotNull(res.idVoto(), "Un voto informado como exitoso debe tener identificador.");
    }

    private static final class MemoryVotoDAO extends VotoDAO {
        private final List<Voto> store = new ArrayList<>();
        private long seq = 1L;

        /** Fuerza la carrera que gana la restricción de unicidad en la base de datos. */
        boolean simularDuplicadoEnRegistro = false;

        /** Fuerza un fallo de persistencia ajeno a la unicidad. */
        boolean simularFalloDePersistencia = false;

        @Override
        public Voto save(Voto entity) {
            entity.setIdVoto(seq++);
            store.add(entity);
            return entity;
        }

        /**
         * Reproduce el contrato real de {@link VotoDAO#registrarUnico(Voto)}: la
         * unicidad la impone el almacén, nunca se devuelve una entidad sin id, y los
         * fallos se lanzan en lugar de tragarse.
         */
        @Override
        public Voto registrarUnico(Voto entity) {
            if (simularFalloDePersistencia) {
                throw new PersistenciaException("Fallo simulado al registrar el voto.", null);
            }
            if (simularDuplicadoEnRegistro
                || existsByVotacionAndMiembro(entity.getIdVotacion(), entity.getIdMiembro())) {
                throw new VotoDuplicadoException(
                    "El miembro ya ha emitido su voto en esta votación. Solo se permite un voto por persona.", null);
            }
            entity.setIdVoto(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public boolean existsByVotacionAndMiembro(Integer idVotacion, Integer idMiembro) {
            return store.stream().anyMatch(v -> v.getIdVotacion().equals(idVotacion) && v.getIdMiembro().equals(idMiembro));
        }

        @Override
        public Optional<Voto> findByVotacionAndMiembro(Integer idVotacion, Integer idMiembro) {
            return store.stream().filter(v -> v.getIdVotacion().equals(idVotacion) && v.getIdMiembro().equals(idMiembro)).findFirst();
        }

        @Override
        public int countByVotacion(Integer idVotacion) {
            return (int) store.stream().filter(v -> v.getIdVotacion().equals(idVotacion)).count();
        }

        @Override
        public int countByOpcion(Integer idOpcion) {
            return (int) store.stream().filter(v -> v.getIdOpcion().equals(idOpcion)).count();
        }
    }

    private static final class MemoryVotacionDAO extends VotacionDAO {
        private final List<Votacion> store = new ArrayList<>();

        @Override
        public Votacion save(Votacion entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Votacion> findById(Integer id) {
            return store.stream().filter(v -> v.getIdVotacion().equals(id)).findFirst();
        }
    }

    private static final class MemoryOpcionDAO extends OpcionVotacionDAO {
        private final List<OpcionVotacion> store = new ArrayList<>();

        @Override
        public OpcionVotacion save(OpcionVotacion entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<OpcionVotacion> findById(Integer id) {
            return store.stream().filter(o -> o.getIdOpcion().equals(id)).findFirst();
        }
    }

    private static final class MemoryMiembroDAO extends MiembroDAO {
        private final List<Miembro> store = new ArrayList<>();

        @Override
        public Miembro save(Miembro entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Miembro> findById(Integer id) {
            return store.stream().filter(m -> m.getIdMiembro().equals(id)).findFirst();
        }
    }
}
