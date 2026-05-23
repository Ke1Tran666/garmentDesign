package com.garmentDesign.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(String email, String password);
}