package com.foody.ordertracker.service;

import com.foody.ordertracker.dto.LoginRequest;
import com.foody.ordertracker.dto.RegisterRequest;
import com.foody.ordertracker.dto.TokenResponse;
import com.foody.ordertracker.dto.UserResponse;
import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.exception.EmailAlreadyInUseException;
import com.foody.ordertracker.exception.InvalidCredentialsException;
import com.foody.ordertracker.repository.UserRepository;
import com.foody.ordertracker.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new TokenResponse(jwtService.generateToken(user.getEmail()), "Bearer");
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}