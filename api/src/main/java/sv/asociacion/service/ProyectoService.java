package sv.asociacion.service;

import sv.asociacion.domain.dto.ProyectoResponse;
import sv.asociacion.dao.ProyectoDAO;
import java.util.List;

public class ProyectoService {
    private final ProyectoDAO proyectoDAO;

    public ProyectoService(ProyectoDAO proyectoDAO) {
        this.proyectoDAO = proyectoDAO;
    }

    public List<ProyectoResponse> findAll() {
        return proyectoDAO.findAll().stream().map(ProyectoResponse::from).toList();
    }
}