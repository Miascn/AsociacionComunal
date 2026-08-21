package sv.asociacion.service;

import java.util.List;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.domain.dto.RolResponse;

public class RolService {
    private final RolDAO rolDAO;

    public RolService(RolDAO rolDAO) { this.rolDAO = rolDAO; }

    public List<RolResponse> findAll() {
        return rolDAO.findAll().stream().map(RolResponse::from).toList();
    }
}
