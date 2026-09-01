package sv.asociacion.service;

import java.util.List;
import sv.asociacion.dao.CargoDAO;
import sv.asociacion.dao.MiembroCargoDAO;
import sv.asociacion.domain.dto.CargoRequest;
import sv.asociacion.domain.dto.CargoResponse;
import sv.asociacion.domain.entity.Cargo;

public class CargoService {
    private final CargoDAO cargoDAO;
    private final MiembroCargoDAO miembroCargoDAO;

    public CargoService(CargoDAO cargoDAO) {
        this(cargoDAO, null);
    }

    public CargoService(CargoDAO cargoDAO, MiembroCargoDAO miembroCargoDAO) {
        this.cargoDAO = cargoDAO;
        this.miembroCargoDAO = miembroCargoDAO;
    }

    public List<CargoResponse> findAll() {
        return cargoDAO.findFiltered(null, null).stream()
            .map(d -> CargoResponse.from(d.cargo(), d.totalAsignaciones()))
            .toList();
    }

    public List<CargoResponse> findFiltered(Boolean activo, String busqueda) {
        return cargoDAO.findFiltered(activo, busqueda).stream()
            .map(d -> CargoResponse.from(d.cargo(), d.totalAsignaciones()))
            .toList();
    }

    public CargoResponse findById(Integer id) {
        if (id == null) return null;
        return cargoDAO.findByIdWithDetail(id)
            .map(d -> CargoResponse.from(d.cargo(), d.totalAsignaciones()))
            .orElse(null);
    }

    public CargoResponse create(CargoRequest request) {
        validar(request, null);

        Cargo cargo = new Cargo();
        cargo.setNombre(request.nombre().trim());
        cargo.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : "");
        cargo.setNivelJerarquico(request.nivelJerarquico() != null ? request.nivelJerarquico() : 1);
        cargo.setActivo(request.activo() != null ? request.activo() : true);

        Cargo saved = cargoDAO.save(cargo);
        return findById(saved.getIdCargo());
    }

    public CargoResponse update(Integer id, CargoRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del cargo es requerido.");
        }
        Cargo existing = cargoDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado."));

        validar(request, id);

        existing.setNombre(request.nombre().trim());
        existing.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : "");
        existing.setNivelJerarquico(request.nivelJerarquico() != null ? request.nivelJerarquico() : 1);
        if (request.activo() != null) {
            existing.setActivo(request.activo());
        }

        cargoDAO.update(existing);
        return findById(id);
    }

    public CargoResponse toggleActivo(Integer id, boolean activo) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del cargo es requerido.");
        }
        cargoDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado."));
        cargoDAO.toggleActivo(id, activo);
        return findById(id);
    }

    public boolean delete(Integer id) {
        if (id == null) return false;
        cargoDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado."));

        if (miembroCargoDAO != null && miembroCargoDAO.countByCargo(id) > 0) {
            throw new IllegalStateException(
                "No es posible eliminar el cargo porque cuenta con asignaciones históricas o vigentes asociadas. Se recomienda desactivarlo."
            );
        }

        return cargoDAO.delete(id);
    }

    private void validar(CargoRequest req, Integer excludeId) {
        if (req == null) {
            throw new IllegalArgumentException("Los datos del cargo son obligatorios.");
        }
        if (req.nombre() == null || req.nombre().trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del cargo es obligatorio.");
        }
        if (req.nivelJerarquico() == null || req.nivelJerarquico() <= 0) {
            throw new IllegalArgumentException("El nivel jerárquico debe ser un número entero mayor que cero.");
        }

        cargoDAO.findByNombre(req.nombre().trim()).ifPresent(c -> {
            if (excludeId == null || !c.getIdCargo().equals(excludeId)) {
                throw new IllegalStateException("Ya existe un cargo directivo con el nombre '" + req.nombre().trim() + "'.");
            }
        });
    }
}
