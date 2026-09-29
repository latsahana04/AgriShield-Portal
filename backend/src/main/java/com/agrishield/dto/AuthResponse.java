package com.agrishield.dto;

import com.agrishield.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInMs;
    private String username;
    private String fullName;
    private Role role;
    private String licenseId;
    private String orgName;
}
