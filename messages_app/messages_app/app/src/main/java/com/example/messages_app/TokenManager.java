package com.example.messages_app;
//clase para gestionar el token
public class TokenManager {
    private static TokenManager instance;
    private String token;

    private TokenManager() {
        // Constructor privado para evitar instanciación directa
    }

    public static TokenManager getInstance() {
        if (instance == null) {
            instance = new TokenManager();
        }
        return instance;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
