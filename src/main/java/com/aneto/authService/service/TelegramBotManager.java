package com.aneto.authService.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Service
@Slf4j
public class TelegramBotManager implements AutoCloseable {

    private TelegramBotsLongPollingApplication botApp;

    @Getter
    private String currentToken;

    private boolean active = false;
    private final AuthService authService;

    // Injetamos o AuthService para passá-lo ao bot quando ele iniciar
    public TelegramBotManager(AuthService authService) {
        this.authService = authService;
    }

    public boolean isBotActive() {
        return this.active;
    }

    public void startBot(String token) throws Exception {
        try {
            // 1. Limpeza: Se já houver um bot a correr, paramos primeiro
            if (botApp != null) {
                stopBot();
            }

            log.info("🤖 A iniciar bot com o novo token...");
            this.currentToken = token;
            this.botApp = new TelegramBotsLongPollingApplication();

            // 2. REGISTO REAL: Aqui ligamos o Manager ao seu TelegramBotService
            // Passamos o token e o authService para o consumidor de mensagens
            TelegramBotService botService = new TelegramBotService(authService, token);

            botApp.registerBot(token, botService);

            this.active = true;
            log.info("✅ Bot Telegram registado e a ouvir atualizações!");
        } catch (Exception e) {
            this.active = false;
            log.error("❌ Falha ao iniciar bot: {}", e.getMessage());
            throw new Exception("Erro ao validar token com o Telegram: " + e.getMessage());
        }
    }

    public void stopBot() {
        try {
            if (botApp != null) {
                botApp.close();
                botApp = null;
                this.active = false;
                log.info("ℹ️ Sessão do Bot encerrada.");
            }
        } catch (Exception e) {
            log.error("Erro ao parar bot: {}", e.getMessage());
        }
    }

    public void sendTestMessage() {
        if (!active) throw new RuntimeException("Bot não está iniciado");
        log.info("A disparar teste de conectividade...");
        // Como o botService é interno ao registro, o teste ideal é verificar o status no dashboard
    }

    @Override
    public void close() throws Exception {
        stopBot();
    }
}