package com.footballstats.backend.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GuestLoginRequest {

    @Size(max = 80, message = "Имя гостя должно быть не длиннее 80 символов.")
    @Pattern(regexp = "^$|^[^\\p{Cntrl}]+$", message = "Имя гостя не должно содержать управляющих символов.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}