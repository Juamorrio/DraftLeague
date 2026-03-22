package com.DraftLeague.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotNull
    @Size(min = 3, max = 40)
    private String username;

    @NotNull
    @Email
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private String email;

    @NotNull
    @Size(min = 8, max = 100)
    private String password;

    @NotNull
    @Size(max = 40)
    private String displayName;
}
