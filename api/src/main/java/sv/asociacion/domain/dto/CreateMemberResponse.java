package sv.asociacion.domain.dto;

public record CreateMemberResponse(MiembroResponse member, String username, String temporaryPassword) { }
