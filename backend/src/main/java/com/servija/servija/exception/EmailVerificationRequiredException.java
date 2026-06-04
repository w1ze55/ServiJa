package com.servija.servija.exception;

public class EmailVerificationRequiredException extends RuntimeException {

    private final String email;

    public EmailVerificationRequiredException(String email) {
        super("E-mail nao verificado. Confirme seu e-mail para concluir o acesso.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
