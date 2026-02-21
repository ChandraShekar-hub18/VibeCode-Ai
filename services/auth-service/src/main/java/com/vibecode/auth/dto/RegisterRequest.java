package com.vibecode.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @Email(message = "Email should be valid")
    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
