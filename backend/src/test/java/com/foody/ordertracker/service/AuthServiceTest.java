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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService.register")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("normaliza o e-mail, criptografa a senha e salva o usuário")
    void shouldNormalizeEmailAndHashPassword() {
        // Given
        var request = new RegisterRequest("  Ana  ", "  Ana@Foody.COM ", "senha1234");
        when(userRepository.existsByEmail("ana@foody.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserResponse response = authService.register(request);

        // Then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Ana");
        assertThat(saved.getEmail()).isEqualTo("ana@foody.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(response.email()).isEqualTo("ana@foody.com");
    }

    @Test
    @DisplayName("lança EmailAlreadyInUseException quando o e-mail já existe, sem salvar")
    void shouldRejectDuplicatedEmail() {
        // Given
        var request = new RegisterRequest("Ana", "ANA@foody.com", "senha1234");
        when(userRepository.existsByEmail("ana@foody.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("converte a violação da constraint UNIQUE em EmailAlreadyInUseException")
    void shouldTranslateConstraintViolationOnRace() {
        // Given: o exists não viu o e-mail, mas o banco barrou o INSERT
        var request = new RegisterRequest("Ana", "ana@foody.com", "senha1234");
        when(userRepository.existsByEmail("ana@foody.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-bcrypt");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed"));

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("com credenciais corretas devolve token Bearer")
        void shouldReturnTokenWhenCredentialsAreValid() {
            var user = new User("Ana", "ana@foody.com", "hash-bcrypt");
            when(userRepository.findByEmail("ana@foody.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senha1234", "hash-bcrypt")).thenReturn(true);
            when(jwtService.generateToken("ana@foody.com")).thenReturn("token-jwt");

            TokenResponse response = authService.login(new LoginRequest("  ANA@foody.com ", "senha1234"));

            assertThat(response.token()).isEqualTo("token-jwt");
            assertThat(response.type()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("com e-mail inexistente lança InvalidCredentialsException")
        void shouldRejectUnknownEmail() {
            when(userRepository.findByEmail("ana@foody.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("ana@foody.com", "senha1234")))
                    .isInstanceOf(InvalidCredentialsException.class);
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("com senha errada lança InvalidCredentialsException")
        void shouldRejectWrongPassword() {
            var user = new User("Ana", "ana@foody.com", "hash-bcrypt");
            when(userRepository.findByEmail("ana@foody.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("errada123", "hash-bcrypt")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("ana@foody.com", "errada123")))
                    .isInstanceOf(InvalidCredentialsException.class);
            verify(jwtService, never()).generateToken(any());
        }
    }
}