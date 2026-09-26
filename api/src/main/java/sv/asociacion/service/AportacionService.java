package sv.asociacion.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import sv.asociacion.dao.AportacionDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.domain.dto.AportacionPageResponse;
import sv.asociacion.domain.dto.AportacionRequest;
import sv.asociacion.domain.dto.AportacionResponse;
import sv.asociacion.domain.entity.Aportacion;
import sv.asociacion.domain.entity.Miembro;

public class AportacionService {
    private final AportacionDAO aportacionDAO;
    private final MiembroDAO miembroDAO;
    private final ProyectoDAO proyectoDAO;
    private final sv.asociacion.dao.ConfiguracionDAO configuracionDAO;

    public AportacionService(AportacionDAO aportacionDAO, MiembroDAO miembroDAO, ProyectoDAO proyectoDAO) {
        this(aportacionDAO, miembroDAO, proyectoDAO, new sv.asociacion.dao.ConfiguracionDAO());
    }

    public AportacionService(AportacionDAO aportacionDAO, MiembroDAO miembroDAO, ProyectoDAO proyectoDAO, sv.asociacion.dao.ConfiguracionDAO configuracionDAO) {
        this.aportacionDAO = aportacionDAO;
        this.miembroDAO = miembroDAO;
        this.proyectoDAO = proyectoDAO;
        this.configuracionDAO = configuracionDAO != null ? configuracionDAO : new sv.asociacion.dao.ConfiguracionDAO();
    }

    public AportacionResponse create(AportacionRequest request) {
        validar(request, null);

        Aportacion entity = new Aportacion();
        entity.setIdMiembro(request.idMiembro());
        entity.setIdProyecto(request.idProyecto());
        entity.setPeriodoMes(request.periodoMes().trim());
        entity.setMonto(request.monto());
        entity.setFechaPago(request.fechaPago());
        entity.setMetodoPago(request.metodoPago());
        entity.setReferencia(request.referencia() != null ? request.referencia().trim() : null);
        entity.setEstado(Aportacion.Estado.REGISTRADA);

        Aportacion saved = aportacionDAO.save(entity);
        return findById(saved.getIdAportacion());
    }

    public AportacionResponse update(Long id, AportacionRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("ID de aportación no especificado.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la aportación son obligatorios.");
        }
        Aportacion existing = aportacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aportación no encontrada."));

        if (existing.getEstado() == Aportacion.Estado.ANULADA) {
            throw new IllegalStateException("No se puede editar una aportación que ha sido anulada.");
        }

        Integer idMiembro = request.idMiembro() != null ? request.idMiembro() : existing.getIdMiembro();
        Integer idProyecto = request.idProyecto() != null ? request.idProyecto() : existing.getIdProyecto();
        String periodoMes = request.periodoMes() != null && !request.periodoMes().isBlank()
            ? request.periodoMes().trim() : existing.getPeriodoMes();
        BigDecimal monto = request.monto() != null ? request.monto() : existing.getMonto();
        LocalDate fechaPago = request.fechaPago() != null ? request.fechaPago() : existing.getFechaPago();
        Aportacion.MetodoPago metodoPago = request.metodoPago() != null ? request.metodoPago() : existing.getMetodoPago();
        String referencia = request.referencia() != null ? request.referencia().trim() : existing.getReferencia();

        AportacionRequest fullRequest = new AportacionRequest(
            idMiembro, idProyecto, periodoMes, monto, fechaPago, metodoPago, referencia
        );
        validar(fullRequest, id);

        existing.setIdMiembro(idMiembro);
        existing.setIdProyecto(idProyecto);
        existing.setPeriodoMes(periodoMes);
        existing.setMonto(monto);
        existing.setFechaPago(fechaPago);
        existing.setMetodoPago(metodoPago);
        existing.setReferencia(referencia);

        aportacionDAO.update(existing);
        return findById(id);
    }

    public AportacionResponse ajustarMonto(Long id, BigDecimal nuevoMonto) {
        if (nuevoMonto == null || nuevoMonto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser un valor monetario positivo mayor a cero.");
        }
        return update(id, new AportacionRequest(null, null, null, nuevoMonto, null, null, null));
    }

    public boolean anular(Long id) {
        if (id == null) return false;
        Aportacion existing = aportacionDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aportación no encontrada."));
        if (existing.getEstado() == Aportacion.Estado.ANULADA) {
            return true;
        }
        return aportacionDAO.anular(id);
    }

    public AportacionResponse findById(Long id) {
        if (id == null) return null;
        return aportacionDAO.findByIdWithDetails(id)
            .map(d -> AportacionResponse.from(d.aportacion(), d.nombreMiembro(), d.duiMiembro(), d.nombreProyecto()))
            .orElse(null);
    }

    public AportacionPageResponse findPage(Integer idMiembro, Integer idProyecto, String periodo,
                                          String desde, String hasta, String metodo, String estado,
                                          String busqueda, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        int total = aportacionDAO.countFiltered(idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);
        BigDecimal totalRecaudado = aportacionDAO.sumFiltered(idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda);
        List<AportacionDAO.AportacionDetail> details = aportacionDAO.findFiltered(
            idMiembro, idProyecto, periodo, desde, hasta, metodo, estado, busqueda, offset, safeSize
        );

        List<AportacionResponse> items = details.stream()
            .map(d -> AportacionResponse.from(d.aportacion(), d.nombreMiembro(), d.duiMiembro(), d.nombreProyecto()))
            .toList();

        return AportacionPageResponse.of(items, total, totalRecaudado, safePage, safeSize);
    }

    public BigDecimal getCuotaMantenimiento() {
        return configuracionDAO.getDecimal("cuota_mantenimiento_mensual", new BigDecimal("10.00"));
    }

    public void setCuotaMantenimiento(BigDecimal cuota) {
        setCuotaMantenimiento(cuota, null);
    }

    public void setCuotaMantenimiento(BigDecimal cuota, String descripcion) {
        if (cuota == null || cuota.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cuota de mantenimiento debe ser un valor mayor a cero.");
        }
        configuracionDAO.setValor(
            "cuota_mantenimiento_mensual",
            cuota.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
            descripcion != null && !descripcion.isBlank() ? descripcion.trim() : "Monto de la cuota mensual de mantenimiento de la colonia"
        );
    }

    public sv.asociacion.domain.dto.MantenimientoPeriodoResponse getMantenimientoPeriodo(String periodoMes, String busqueda) {
        String periodo = (periodoMes != null && periodoMes.trim().matches("^\\d{4}-(0[1-9]|1[0-2])$"))
            ? periodoMes.trim()
            : LocalDate.now().toString().substring(0, 7);

        BigDecimal cuota = getCuotaMantenimiento();

        List<Miembro> miembros = miembroDAO != null ? miembroDAO.findAll() : List.of();
        List<Miembro> miembrosActivos = miembros.stream()
            .filter(m -> m.getEstado() == null || m.getEstado() == Miembro.Estado.ACTIVO)
            .toList();

        List<AportacionDAO.AportacionDetail> aportes = aportacionDAO.findFiltered(
            null, null, periodo, null, null, null, "REGISTRADA", null, 0, 5000
        );

        java.util.Map<Integer, AportacionDAO.AportacionDetail> aportePorMiembro = new java.util.HashMap<>();
        for (AportacionDAO.AportacionDetail d : aportes) {
            if (d.aportacion() != null && d.aportacion().getIdProyecto() == null && d.aportacion().getIdMiembro() != null) {
                aportePorMiembro.put(d.aportacion().getIdMiembro(), d);
            }
        }

        String search = busqueda != null ? busqueda.trim().toLowerCase() : "";
        List<sv.asociacion.domain.dto.MantenimientoPeriodoResponse.Item> items = new java.util.ArrayList<>();
        int pagados = 0;
        BigDecimal recaudado = BigDecimal.ZERO;

        for (Miembro m : miembrosActivos) {
            String nombre = ((m.getNombres() != null ? m.getNombres() : "") + " " + (m.getApellidos() != null ? m.getApellidos() : "")).trim();
            String dui = m.getDui() != null ? m.getDui() : "";

            if (!search.isEmpty() && !nombre.toLowerCase().contains(search) && !dui.toLowerCase().contains(search)) {
                continue;
            }

            AportacionDAO.AportacionDetail detalle = aportePorMiembro.get(m.getIdMiembro());
            boolean pagado = detalle != null && detalle.aportacion() != null;

            Long idAportacion = null;
            BigDecimal monto = cuota;
            String fechaPago = null;
            String metodoPago = null;
            String referencia = null;
            String estadoAportacion = "PENDIENTE";

            if (pagado) {
                pagados++;
                Aportacion a = detalle.aportacion();
                idAportacion = a.getIdAportacion();
                monto = a.getMonto() != null ? a.getMonto() : cuota;
                recaudado = recaudado.add(monto);
                fechaPago = a.getFechaPago() != null ? a.getFechaPago().toString() : null;
                metodoPago = a.getMetodoPago() != null ? a.getMetodoPago().name() : null;
                referencia = a.getReferencia();
                estadoAportacion = a.getEstado() != null ? a.getEstado().name() : "REGISTRADA";
            }

            String viviendaInfo = m.getIdVivienda() != null ? "Vivienda #" + m.getIdVivienda() : "Sin asignar";

            items.add(new sv.asociacion.domain.dto.MantenimientoPeriodoResponse.Item(
                m.getIdMiembro(),
                dui,
                nombre,
                viviendaInfo,
                pagado,
                idAportacion,
                monto,
                fechaPago,
                metodoPago,
                referencia,
                estadoAportacion
            ));
        }

        int totalMiembros = items.size();
        int pendientes = totalMiembros - pagados;
        BigDecimal esperado = cuota.multiply(BigDecimal.valueOf(totalMiembros));
        double porcentaje = totalMiembros > 0 ? (pagados * 100.0) / totalMiembros : 0.0;

        return new sv.asociacion.domain.dto.MantenimientoPeriodoResponse(
            periodo,
            cuota,
            totalMiembros,
            pagados,
            pendientes,
            recaudado,
            esperado,
            porcentaje,
            items
        );
    }

    private void validar(AportacionRequest req, Long excludeId) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos de la aportación son obligatorios.");
        }
        if (req.idMiembro() == null) {
            throw new IllegalArgumentException("El miembro asociado es obligatorio.");
        }
        if (miembroDAO != null && miembroDAO.findById(req.idMiembro()).isEmpty()) {
            throw new IllegalArgumentException("El miembro seleccionado no existe en el sistema.");
        }
        if (req.idProyecto() != null && proyectoDAO != null) {
            if (proyectoDAO.findById(req.idProyecto()).isEmpty()) {
                throw new IllegalArgumentException("El proyecto seleccionado no existe.");
            }
        }
        if (req.monto() == null || req.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser un valor monetario positivo mayor a cero.");
        }
        if (req.fechaPago() == null) {
            throw new IllegalArgumentException("La fecha de pago es obligatoria.");
        }
        if (req.fechaPago().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser posterior a la fecha actual.");
        }
        if (req.periodoMes() == null || !req.periodoMes().trim().matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("El período debe tener formato de mes válido: YYYY-MM (ej. 2026-03).");
        }
        if (req.metodoPago() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio.");
        }
        // La restricción de una sola aportación por período aplica exclusivamente a la cuota mensual ordinaria (sin proyecto).
        // Para las aportaciones a proyectos comunitarios se permiten múltiples aportaciones voluntarias del mismo miembro.
        if (req.idProyecto() == null && aportacionDAO.existsByMiembroAndPeriodo(req.idMiembro(), null, req.periodoMes().trim(), excludeId)) {
            throw new IllegalStateException("Ya existe una aportación registrada para este miembro en el período " + req.periodoMes().trim() + ".");
        }
    }
}
