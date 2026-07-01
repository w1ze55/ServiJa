package com.servija.servija.dto;

import com.servija.servija.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRequest {

    @NotNull(message = "Perfil obrigatório")
    private User.UserType role;
}
