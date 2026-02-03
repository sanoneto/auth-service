package com.aneto.authService.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SocketIOConfig {

    @Value("${socketio.host:0.0.0.0}")
    private String socketHost;

    @Value("${socketio.port:9095}")
    private int socketPort;

    @Value("${socketio.origin:http://localhost:5173}")
    private String allowedOrigin;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketHost);
        config.setPort(socketPort);

        // No Socket.IO v4, o CORS é extremamente restrito.
        // Se houver uma barra no fim (ex: ...5173/), o browser bloqueia.
        if (allowedOrigin.endsWith("/")) {
            allowedOrigin = allowedOrigin.substring(0, allowedOrigin.length() - 1);
        }
        config.setOrigin(allowedOrigin);

        // Habilita explicitamente os transportes
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        config.setAllowCustomRequests(true);

        // Melhora a compatibilidade com o handshake v4
        config.setUpgradeTimeout(10000);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);

        return new SocketIOServer(config);
    }
}