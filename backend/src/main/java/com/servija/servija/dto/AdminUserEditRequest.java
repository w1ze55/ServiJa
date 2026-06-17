package com.servija.servija.dto;

import com.servija.servija.model.User;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserEditRequest {
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String name;
    private String email;
    private String phone;
    private String bio;
    private User.UserType userType;
}
