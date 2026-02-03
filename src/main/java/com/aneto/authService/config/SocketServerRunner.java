package com.aneto.authService.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketServerRunner {

    private final SocketIOServer server;

    @PostConstruct
    public void startServer() {
        try {
            server.start();
            log.info("✅ Servidor Socket.IO iniciado com sucesso na porta {}", server.getConfiguration().getPort());
        } catch (Exception e) {
            log.error("❌ Erro ao iniciar o servidor Socket.IO: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
        log.info("🛑 Servidor Socket.IO desligado.");
    }
}