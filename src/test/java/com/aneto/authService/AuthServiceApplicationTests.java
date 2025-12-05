package com.aneto.authService;

// language: java

import com.aneto.authService.service.AuthService;

import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

@SpringBootTest
public class AuthServiceApplicationTests {

    // O Spring irá injetar o mock configurado abaixo.
    // ...

    @TestConfiguration // Indica que esta classe fornece beans específicos para o teste
    static class TestConfig {
        @Bean // O bean injetado no contexto de teste será este Mock
        public AuthService usersService() {
            // Cria e retorna o mock do Mockito
            return Mockito.mock(AuthService.class);
        }
    }
}