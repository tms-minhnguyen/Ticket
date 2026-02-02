package com.example.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private Long id;
    private String email;
    private String fullName;
    private String role;
}
