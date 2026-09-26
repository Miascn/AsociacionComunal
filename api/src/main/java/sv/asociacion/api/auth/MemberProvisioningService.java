package sv.asociacion.api.auth;

import sv.asociacion.config.DBConnection;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;

public final class MemberProvisioningService {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
    private final PasswordService passwords = new PasswordService();
    private final SecureRandom random = new SecureRandom();

    public Result create(String document, String documentType, String country, Integer houseId, String names, String lastNames, String phone, String email) {
        String temporary = temporaryPassword();
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                int memberId;
                String memberSql = "INSERT INTO miembro (dui,tipo_documento,pais_origen,id_vivienda,nombres,apellidos,telefono,correo,direccion,fecha_ingreso,estado) VALUES (?,?,?,?,?,?,?,?,NULL,?, 'ACTIVO')";
                try (PreparedStatement statement = connection.prepareStatement(memberSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, document); statement.setString(2, documentType); statement.setString(3, country);
                    if (houseId == null) statement.setNull(4, Types.INTEGER); else statement.setInt(4, houseId);
                    statement.setString(5, names); statement.setString(6, lastNames);
                    statement.setString(7, phone); statement.setString(8, email);
                    statement.setDate(9, Date.valueOf(LocalDate.now())); statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Sin identificador de miembro."); memberId = keys.getInt(1); }
                }
                String userSql = "INSERT INTO usuario (id_rol,id_miembro,nombre_usuario,clave_hash,estado,requiere_cambio_clave,clave_temporal) SELECT id_rol,?,?,?,'ACTIVO',TRUE,? FROM rol WHERE nombre='MIEMBRO'";
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setInt(1, memberId); statement.setString(2, document); statement.setString(3, passwords.hash(temporary)); statement.setString(4, temporary);
                    if (statement.executeUpdate() != 1) throw new SQLException("No existe el rol MIEMBRO.");
                } catch (SQLException sqle) {
                    String fallbackSql = "INSERT INTO usuario (id_rol,id_miembro,nombre_usuario,clave_hash,estado,requiere_cambio_clave) SELECT id_rol,?,?,?,'ACTIVO',TRUE FROM rol WHERE nombre='MIEMBRO'";
                    try (PreparedStatement statement = connection.prepareStatement(fallbackSql)) {
                        statement.setInt(1, memberId); statement.setString(2, document); statement.setString(3, passwords.hash(temporary));
                        if (statement.executeUpdate() != 1) throw new SQLException("No existe el rol MIEMBRO.");
                    }
                }
                connection.commit(); return new Result(memberId, temporary);
            } catch (Exception exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        } catch (SQLException exception) { throw new IllegalStateException("No fue posible crear el miembro y su cuenta.", exception); }
    }

    private String temporaryPassword() {
        StringBuilder value = new StringBuilder("Tmp!");
        for (int i = 0; i < 13; i++) value.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        return value.toString();
    }
    public record Result(int memberId, String temporaryPassword) { }
}
