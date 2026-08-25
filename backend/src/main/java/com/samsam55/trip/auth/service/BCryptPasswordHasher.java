package com.samsam55.trip.auth.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher {

    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return BCrypt.checkpw(rawPassword, passwordHash);
    }
}
