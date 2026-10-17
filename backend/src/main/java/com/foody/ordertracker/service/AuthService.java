package com.foody.ordertracker.service;

import com.foody.ordertracker.dto.RegisterRequest;
import com.foody.ordertracker.dto.UserResponse;
import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.exception.EmailAlreadyInUseException;
import com.foody.ordertracker.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        String hash = passwordEncoder.encode(request.password());
        User user = new User(request.name().trim(), email, hash);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Duas requisições simultâneas passaram pelo existsByEmail;
            // a constraint UNIQUE do banco barrou a segunda.
            throw new EmailAlreadyInUseException(email);
        }

        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}