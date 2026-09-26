package sv.asociacion.dao;

import sv.asociacion.config.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ConfiguracionDAO {

    public Optional<String> getValor(String clave) {
        if (clave == null || clave.isBlank()) return Optional.empty();
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("valor"));
                }
            }
        } catch (SQLException e) {
            // Silencioso si la tabla no está creada aún en modo memoria/test
        }
        return Optional.empty();
    }

    public void setValor(String clave, String valor, String descripcion) {
        if (clave == null || clave.isBlank()) return;
        String sql = "INSERT INTO configuracion (clave, valor, descripcion) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE valor = VALUES(valor), descripcion = COALESCE(VALUES(descripcion), descripcion)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave.trim());
            ps.setString(2, valor != null ? valor.trim() : "");
            ps.setString(3, descripcion != null ? descripcion.trim() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public BigDecimal getDecimal(String clave, BigDecimal defaultValue) {
        return getValor(clave)
            .map(val -> {
                try {
                    return new BigDecimal(val.trim());
                } catch (Exception e) {
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }
}
