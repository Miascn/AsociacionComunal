package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.AportacionDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.ProyectoRequest;
import sv.asociacion.domain.dto.ProyectoResponse;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.domain.entity.Usuario;

class ProyectoServiceTest {
    private MemoryProyectoDAO proyectoDAO;
    private MemoryUsuarioDAO usuarioDAO;
    private ProyectoService service;

    @BeforeEach
    void setUp() {
        proyectoDAO = new MemoryProyectoDAO();
        usuarioDAO = new MemoryUsuarioDAO();
        service = new ProyectoService(proyectoDAO, usuarioDAO, null);

        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombreUsuario("admin");
        usuarioDAO.save(u);
    }

    @Test
    void createsProyectoSuccessfully() {
        ProyectoRequest req = new ProyectoRequest(
            "Alumbrado Solar Comunal", "Instalación de luminarias LED",
            new BigDecimal("3500.00"), 1, Proyecto.Estado.BORRADOR
        );
        ProyectoResponse res = service.create(req);

        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("Alumbrado Solar Comunal", res.nombre());
        assertEquals(new BigDecimal("3500.00"), res.presupuesto());
        assertEquals("BORRADOR", res.estado());
        assertEquals(1, res.creadoPor());
        assertEquals("admin", res.nombreCreador());
    }

    @Test
    void rejectsBlankName() {
        ProyectoRequest req = new ProyectoRequest(
            "   ", "Sin nombre", BigDecimal.ZERO, 1, Proyecto.Estado.BORRADOR
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsNegativeBudget() {
        ProyectoRequest req = new ProyectoRequest(
            "Pavimentación", "Calle 2", new BigDecimal("-100.00"), 1, Proyecto.Estado.BORRADOR
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsDuplicateName() {
        service.create(new ProyectoRequest(
            "Cancha Deportiva", "Construcción", new BigDecimal("5000.00"), 1, Proyecto.Estado.BORRADOR
        ));

        ProyectoRequest dup = new ProyectoRequest(
            "cancha deportiva", "Duplicado", new BigDecimal("2000.00"), 1, Proyecto.Estado.BORRADOR
        );
        assertThrows(IllegalStateException.class, () -> service.create(dup));
    }

    @Test
    void validatesStateMachineTransitions() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Parque Infantil", "Zona de juegos", new BigDecimal("1200.00"), 1, Proyecto.Estado.BORRADOR
        ));
        assertEquals("BORRADOR", p.estado());

        // BORRADOR -> PROPUESTO
        ProyectoResponse propuesto = service.cambiarEstado(p.id(), Proyecto.Estado.PROPUESTO);
        assertEquals("PROPUESTO", propuesto.estado());

        // PROPUESTO -> APROBADO
        ProyectoResponse aprobado = service.cambiarEstado(p.id(), Proyecto.Estado.APROBADO);
        assertEquals("APROBADO", aprobado.estado());

        // APROBADO -> EN_EJECUCION
        ProyectoResponse ejecucion = service.cambiarEstado(p.id(), Proyecto.Estado.EN_EJECUCION);
        assertEquals("EN_EJECUCION", ejecucion.estado());

        // EN_EJECUCION -> FINALIZADO
        ProyectoResponse finalizado = service.cambiarEstado(p.id(), Proyecto.Estado.FINALIZADO);
        assertEquals("FINALIZADO", finalizado.estado());
    }

    @Test
    void rejectsInvalidTransitions() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Salón Comunal", "Remodelación", new BigDecimal("8000.00"), 1, Proyecto.Estado.BORRADOR
        ));

        // Intento de saltar de BORRADOR a EN_EJECUCION
        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(p.id(), Proyecto.Estado.EN_EJECUCION));
        // Intento de saltar de BORRADOR a FINALIZADO
        assertThrows(IllegalStateException.class, () -> service.cambiarEstado(p.id(), Proyecto.Estado.FINALIZADO));
    }

    @Test
    void allowsRejectionAndReformulation() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Puente Peatonal", "Cruce seguro", new BigDecimal("15000.00"), 1, Proyecto.Estado.BORRADOR
        ));

        service.cambiarEstado(p.id(), Proyecto.Estado.PROPUESTO);
        ProyectoResponse rechazado = service.cambiarEstado(p.id(), Proyecto.Estado.RECHAZADO);
        assertEquals("RECHAZADO", rechazado.estado());

        // RECHAZADO -> BORRADOR para replanteamiento
        ProyectoResponse borrador = service.cambiarEstado(p.id(), Proyecto.Estado.BORRADOR);
        assertEquals("BORRADOR", borrador.estado());
    }

    @Test
    void updatesProyectoSuccessfully() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Arborización", "Fase 1", new BigDecimal("500.00"), 1, Proyecto.Estado.BORRADOR
        ));

        ProyectoResponse updated = service.update(p.id(), new ProyectoRequest(
            "Arborización Comunal", "Fase 1 y 2", new BigDecimal("950.00"), 1, Proyecto.Estado.BORRADOR
        ));

        assertEquals("Arborización Comunal", updated.nombre());
        assertEquals("Fase 1 y 2", updated.descripcion());
        assertEquals(new BigDecimal("950.00"), updated.presupuesto());
    }

    @Test
    void rejectsEditingFinalizedProject() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Feria Gastronómica", "Evento", new BigDecimal("300.00"), 1, Proyecto.Estado.BORRADOR
        ));
        service.cambiarEstado(p.id(), Proyecto.Estado.PROPUESTO);
        service.cambiarEstado(p.id(), Proyecto.Estado.APROBADO);
        service.cambiarEstado(p.id(), Proyecto.Estado.EN_EJECUCION);
        service.cambiarEstado(p.id(), Proyecto.Estado.FINALIZADO);

        assertThrows(IllegalStateException.class, () -> service.update(p.id(), new ProyectoRequest(
            "Feria Modificada", "Nueva", new BigDecimal("400.00"), 1, Proyecto.Estado.FINALIZADO
        )));
    }

    @Test
    void deletesDraftProjectSuccessfully() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Idea descartada", "Borrador temporal", BigDecimal.ZERO, 1, Proyecto.Estado.BORRADOR
        ));
        assertTrue(service.delete(p.id()));
    }

    @Test
    void rejectsDeletingApprovedOrActiveProject() {
        ProyectoResponse p = service.create(new ProyectoRequest(
            "Clínica Comunal", "Construcción", new BigDecimal("20000.00"), 1, Proyecto.Estado.BORRADOR
        ));
        service.cambiarEstado(p.id(), Proyecto.Estado.PROPUESTO);
        service.cambiarEstado(p.id(), Proyecto.Estado.APROBADO);

        assertThrows(IllegalStateException.class, () -> service.delete(p.id()));
    }

    private static final class MemoryProyectoDAO extends ProyectoDAO {
        private final List<Proyecto> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public Proyecto save(Proyecto entity) {
            entity.setIdProyecto(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Proyecto> findById(Integer id) {
            return store.stream().filter(p -> p.getIdProyecto().equals(id)).findFirst();
        }

        @Override
        public Optional<ProyectoDetail> findByIdWithCreator(Integer id) {
            return findById(id).map(p -> new ProyectoDetail(p, "admin"));
        }

        @Override
        public Optional<Proyecto> findByNombre(String nombre) {
            return store.stream().filter(p -> p.getNombre().equalsIgnoreCase(nombre)).findFirst();
        }

        @Override
        public List<Proyecto> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public List<ProyectoDetail> findFiltered(Proyecto.Estado estado, Integer creadoPor, String busqueda) {
            return store.stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> creadoPor == null || p.getCreadoPor().equals(creadoPor))
                .filter(p -> {
                    if (busqueda == null || busqueda.isBlank()) return true;
                    String b = busqueda.toLowerCase();
                    return p.getNombre().toLowerCase().contains(b) || p.getDescripcion().toLowerCase().contains(b);
                })
                .map(p -> new ProyectoDetail(p, "admin"))
                .toList();
        }

        @Override
        public Proyecto update(Proyecto entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdProyecto().equals(entity.getIdProyecto())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean updateEstado(Integer id, Proyecto.Estado estado) {
            return findById(id).map(p -> {
                p.setEstado(estado);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(p -> p.getIdProyecto().equals(id));
        }
    }

    private static final class MemoryUsuarioDAO extends UsuarioDAO {
        private final List<Usuario> store = new ArrayList<>();

        @Override
        public Usuario save(Usuario entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Usuario> findById(Integer id) {
            return store.stream().filter(u -> u.getIdUsuario().equals(id)).findFirst();
        }
    }
}
