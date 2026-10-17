package com.foody.ordertracker.service;

import com.foody.ordertracker.dto.RegisterRequest;
import com.foody.ordertracker.dto.UserResponse;
import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.exception.EmailAlreadyInUseException;
import com.foody.ordertracker.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}