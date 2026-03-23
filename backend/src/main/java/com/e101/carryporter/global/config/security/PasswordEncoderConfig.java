package com.e101.carryporter.global.config.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public static class BCryptPasswordEncoder {

        public String encode(String password) {
            return BCrypt.hashpw(password, BCrypt.gensalt());
        }

        public boolean matches(String rawPassword, String encodedPassword) {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        }
    }
}
