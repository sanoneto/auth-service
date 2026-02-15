package com.aneto.authService.socket;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketHandler {

    private final SocketIOServer server;

    // Armazena as sessões ativas. Key: publicId
    private static final Map<String, SocketUserSession> onlineUsers = new ConcurrentHashMap<>();

    // DTO interno para envio de dados
    @Getter
    public static class SocketUserSession {
        private final String publicId;
        private final String device;
        private final UUID sessionId;

        public SocketUserSession(String publicId, String device, UUID sessionId) {
            this.publicId = publicId;
            this.device = device;
            this.sessionId = sessionId;
        }
    }

    /**
     * Configuramos apenas os listeners aqui.
     * O arranque do servidor (server.start()) é gerido pela classe SocketServerRunner.
     */
    @PostConstruct
    private void init() {
        log.info("Configurando Listeners do Socket.IO...");
        setupListeners();
    }

    private void setupListeners() {
        // --- REGISTO DE UTILIZADOR ---
        server.addEventListener("register_user", String.class, (client, publicId, ackRequest) -> {
            String userAgent = client.getHandshakeData().getHttpHeaders().get("User-Agent");
            String deviceDetail = parseUserAgent(userAgent);

            SocketUserSession session = new SocketUserSession(publicId, deviceDetail, client.getSessionId());
            onlineUsers.put(publicId, session);

            // Guarda o publicId na sessão do socket para facilitar a desconexão
            client.set("publicId", publicId);

            log.info("Utilizador conectado: {} [Device: {}] [Session: {}]", publicId, deviceDetail, client.getSessionId());
            server.getBroadcastOperations().sendEvent("user_connected", publicId);
        });

        // --- PEDIDO DE LISTA ONLINE ---
        server.addEventListener("request_online_users", String.class, (client, data, ackRequest) -> {
            Collection<SocketUserSession> sessions = onlineUsers.values();
            client.sendEvent("get_online_users_details", sessions);
        });

        // --- DESCONEXÃO ---
        server.addDisconnectListener(client -> {
            String publicId = client.get("publicId");
            if (publicId != null) {
                SocketUserSession session = onlineUsers.get(publicId);
                // Remove apenas se a sessionId for a mesma para evitar inconsistência em multiplas abas
                if (session != null && session.getSessionId().equals(client.getSessionId())) {
                    onlineUsers.remove(publicId);
                    log.info("Utilizador desconectado: {} [Session: {}]", publicId, client.getSessionId());
                    server.getBroadcastOperations().sendEvent("user_disconnected", publicId);
                }
            }
        });
    }

    private String parseUserAgent(String ua) {
        if (ua == null) return "Desconhecido";
        String userAgent = ua.toLowerCase();
        if (userAgent.contains("mobi")) return "Telemóvel";
        if (userAgent.contains("edge")) return "Edge";
        if (userAgent.contains("firefox")) return "Firefox";
        if (userAgent.contains("chrome")) return "Chrome";
        if (userAgent.contains("safari")) return "Safari";
        return "Desktop";
    }
}