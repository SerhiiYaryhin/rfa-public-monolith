package media.toloka.rfa.radio.client.service;

import media.toloka.rfa.radio.repository.UserRepository;
import media.toloka.rfa.security.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClientService clientService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void GetUserById_ShouldReturnUser_WhenUserExists() {
        // Arrange
        Users mockUser = new Users();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        when(userRepository.getReferenceById(1L)).thenReturn(mockUser);

        // Act
        Users result = clientService.GetUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).getReferenceById(1L);
    }

    @Test
    void GetUserById_ShouldReturnNull_WhenUserNotFound() {
        // Arrange
        when(userRepository.getReferenceById(99L)).thenReturn(null);

        // Act
        Users result = clientService.GetUserById(99L);

        // Assert
        assertNull(result);
        verify(userRepository, times(1)).getReferenceById(99L);
    }
}
