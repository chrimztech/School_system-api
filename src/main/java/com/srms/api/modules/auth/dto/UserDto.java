package com.srms.api.modules.auth.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String name;
    private String email;
    private String role;
    private String initials;
    private String schoolId;
    private String phone;
    private boolean active;
    private boolean notifyEmail;
    private boolean notifySms;
}