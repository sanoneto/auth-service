package com.aneto.authService.service;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocketService {

    private final SocketIOServer server;
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    // Usamos @PostConstruct apenas para registar os LISTENERS (os ouvintes)
    // O comando server.start() NÃO deve estar aqui.
    @PostConstruct
    public void setupListeners() {
        log.info("Configurando Listeners do Socket.IO...");

        // Evento de registo (quando o user faz login no front e emite 'register')
        server.addEventListener("register", String.class, (client, publicId, ackSender) -> {
            sessionMap.put(client.getSessionId().toString(), publicId);
            log.info("Utilizador {} registado na sessão {}", publicId, client.getSessionId());
            server.getBroadcastOperations().sendEvent("user_connected", publicId);
        });

        // Evento de desconexão
        server.addDisconnectListener(client -> {
            String sessionId = client.getSessionId().toString();
            String publicId = sessionMap.remove(sessionId);
            if (publicId != null) {
                log.info("Utilizador {} desconectado", publicId);
                server.getBroadcastOperations().sendEvent("user_disconnected", publicId);
            }
        });
    }

    // Método útil para enviares mensagens de outros lugares do sistema
    public void sendToAll(String event, Object data) {
        server.getBroadcastOperations().sendEvent(event, data);
    }
}