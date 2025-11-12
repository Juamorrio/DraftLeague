package com.DraftLeague.models.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;        // access token (JWT)
    private String refreshToken; // raw refresh token (only returned to client)
}
