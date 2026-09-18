package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import sv.asociacion.domain.entity.Voto;
import sv.asociacion.util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VotoDAO implements DAO<Voto, Long> {
    @Override
    public Optional<Voto> findById(Long id) {
        String sql = "SELECT * FROM voto WHERE id_voto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Voto> findAll() {
        List<Voto> votos = new ArrayList<>();
        String sql = "SELECT * FROM voto";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                votos.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return votos;
    }

    @Override
    public Voto save(Voto entity) {
        String sql = "INSERT INTO voto (id_votacion, id_opcion, id_miembro, fecha_hora) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getIdVotacion());
            ps.setInt(2, entity.getIdOpcion());
            ps.setInt(3, entity.getIdMiembro());
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setIdVoto(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    /**
     * Registra el voto dejando que la restriccion {@code uk_voto_votacion_miembro} sea
     * la autoridad frente a duplicados y concurrencia: se inserta directamente y la
     * violacion de unicidad se traduce en {@link VotoDuplicadoException}.
     *
     * <p>No hay comprobacion previa con {@code SELECT ... FOR UPDATE}: la restriccion ya
     * garantiza la unicidad sin el coste de los gap locks.
     *
     * <p>A diferencia de {@link #save(Voto)}, <b>nunca devuelve una entidad sin
     * identificador</b>. Si la insercion falla, lanza. Devolver un voto sin id
     * equivaldria a informar del exito de algo que no se guardo.
     */
    public Voto registrarUnico(Voto entity) {
        String sql = "INSERT INTO voto (id_votacion, id_opcion, id_miembro, fecha_hora) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, entity.getIdVotacion());
                ps.setInt(2, entity.getIdOpcion());
                ps.setInt(3, entity.getIdMiembro());
                ps.setString(4, DateUtils.formatDateTime(entity.getFechaHora()));
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        throw new PersistenciaException(
                            "El registro del voto no devolvio identificador generado.", null);
                    }
                    entity.setIdVoto(keys.getLong(1));
                }

                conn.commit();
                return entity;
            } catch (SQLIntegrityConstraintViolationException e) {
                conn.rollback();
                // 1062 = ER_DUP_ENTRY. Cualquier otra violacion de integridad en esta
                // tabla apunta a una clave foranea invalida, que es un fallo distinto.
                if (e.getErrorCode() == 1062) {
                    throw new VotoDuplicadoException(
                        "El miembro ya ha emitido su voto en esta votación. Solo se permite un voto por persona.", e);
                }
                throw new PersistenciaException("Violación de integridad al registrar el voto.", e);
            } catch (SQLException e) {
                conn.rollback();
                throw new PersistenciaException("No fue posible registrar el voto.", e);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No fue posible abrir la conexión para registrar el voto.", e);
        }
    }

    @Override
    public Voto update(Voto entity) {
        String sql = "UPDATE voto SET id_votacion = ?, id_opcion = ?, id_miembro = ?, fecha_hora = ? WHERE id_voto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getIdVotacion());
            ps.setInt(2, entity.getIdOpcion());
            ps.setInt(3, entity.getIdMiembro());
            ps.setString(4, DateUtils.formatDateTime(entity.getFechaHora()));
            ps.setLong(5, entity.getIdVoto());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM voto WHERE id_voto = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Voto> findByVotacion(Integer idVotacion) {
        List<Voto> list = new ArrayList<>();
        String sql = "SELECT * FROM voto WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Voto> findByOpcion(Integer idOpcion) {
        List<Voto> list = new ArrayList<>();
        String sql = "SELECT * FROM voto WHERE id_opcion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOpcion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean existsByVotacionAndMiembro(Integer idVotacion, Integer idMiembro) {
        String sql = "SELECT 1 FROM voto WHERE id_votacion = ? AND id_miembro = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            ps.setInt(2, idMiembro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Voto> findByVotacionAndMiembro(Integer idVotacion, Integer idMiembro) {
        String sql = "SELECT * FROM voto WHERE id_votacion = ? AND id_miembro = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            ps.setInt(2, idMiembro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public int countByVotacion(Integer idVotacion) {
        String sql = "SELECT COUNT(*) FROM voto WHERE id_votacion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVotacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countByOpcion(Integer idOpcion) {
        String sql = "SELECT COUNT(*) FROM voto WHERE id_opcion = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOpcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Voto mapResultSet(ResultSet rs) throws SQLException {
        return new Voto(
            rs.getLong("id_voto"),
            rs.getInt("id_votacion"),
            rs.getInt("id_opcion"),
            rs.getInt("id_miembro"),
            DateUtils.parseDateTime(rs.getString("fecha_hora"))
        );
    }
}
