package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordLinkValidationResponse {

    private String emailHint;
    private User.UserType userType;
}
