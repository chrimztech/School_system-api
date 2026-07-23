package com.srms.api.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    /** Either an email address or a phone number — some accounts (e.g. parents) have no email. */
    @NotBlank
    private String identifier;
    @NotBlank
    private String password;
}
