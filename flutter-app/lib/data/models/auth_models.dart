/// Modelos para la autenticación y tokens de sesión.
class UserDto {
  final int id;
  final String username;
  final String role;
  final bool passwordChangeRequired;

  const UserDto({
    required this.id,
    required this.username,
    required this.role,
    this.passwordChangeRequired = false,
  });

  factory UserDto.fromJson(Map<String, dynamic> json) {
    return UserDto(
      id: json['id'] as int? ?? 0,
      username: json['username'] as String? ?? '',
      role: json['role'] as String? ?? 'MIEMBRO',
      passwordChangeRequired: json['passwordChangeRequired'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'username': username,
        'role': role,
        'passwordChangeRequired': passwordChangeRequired,
      };
}

class MemberDto {
  final int id;
  final String names;
  final String lastNames;
  final String status;

  const MemberDto({
    required this.id,
    required this.names,
    required this.lastNames,
    required this.status,
  });

  String get fullName => '$names $lastNames'.trim();
  String get firstName => names.split(' ').first;

  factory MemberDto.fromJson(Map<String, dynamic> json) {
    return MemberDto(
      id: json['id'] as int? ?? 0,
      names: json['names'] as String? ?? json['nombres'] as String? ?? '',
      lastNames: json['lastNames'] as String? ?? json['apellidos'] as String? ?? '',
      status: json['status'] as String? ?? json['estado'] as String? ?? 'ACTIVO',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'names': names,
        'lastNames': lastNames,
        'status': status,
      };
}

class LoginResponse {
  final String accessToken;
  final String refreshToken;
  final int expiresIn;
  final UserDto user;

  const LoginResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
    required this.user,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
      expiresIn: json['expiresIn'] as int? ?? 900,
      user: UserDto.fromJson(json['user'] as Map<String, dynamic>? ?? {}),
    );
  }
}

class TokenResponse {
  final String accessToken;
  final String refreshToken;
  final int expiresIn;

  const TokenResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
  });

  factory TokenResponse.fromJson(Map<String, dynamic> json) {
    return TokenResponse(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
      expiresIn: json['expiresIn'] as int? ?? 900,
    );
  }
}

class MeResponse {
  final UserDto user;
  final MemberDto? member;

  const MeResponse({
    required this.user,
    this.member,
  });

  factory MeResponse.fromJson(Map<String, dynamic> json) {
    return MeResponse(
      user: UserDto.fromJson(json['user'] as Map<String, dynamic>? ?? {}),
      member: json['member'] != null
          ? MemberDto.fromJson(json['member'] as Map<String, dynamic>)
          : null,
    );
  }
}
