package com.aneto.authService.controller;

import com.aneto.authService.service.TelegramBotManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramBotManager botManager; // Este é o único que precisas

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("online", botManager.isBotActive());
        status.put("currentToken", botManager.getCurrentToken());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/renew")
    public ResponseEntity<?> renew(@RequestBody Map<String, String> body) {
        try {
            botManager.startBot(body.get("newToken"));
            return ResponseEntity.ok("Token renovado!");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Erro: " + e.getMessage());
        }
    }

    @PostMapping("/test")
    public ResponseEntity<?> testMessage() {
        try {
            // CORRIGIDO: Usar botManager em vez de telegramBotService
            botManager.sendTestMessage();
            return ResponseEntity.ok("Mensagem de teste enviada!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Falha no teste: " + e.getMessage());
        }
    }
}