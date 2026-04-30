package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.Data;

@Data
public class AdminUserEditRequest {
    private String name;
    private String email;
    private String phone;
    private String bio;
    private User.UserType userType;
}
