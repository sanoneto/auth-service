package com.aneto.authService.service;

import com.aneto.authService.exception.ConflictException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

/**
 * Único ponto de gestão do bot Telegram: arranque (automático ou via /renew) e paragem.
 * Garante que existe no máximo uma sessão de long polling ativa.
 */
@Service
@Slf4j
public class TelegramBotManager implements AutoCloseable {

    private TelegramBotsLongPollingApplication botApp;

    @Getter
    private String currentToken;

    private volatile boolean active = false;
    private final AuthService authService;

    @Value("${telegram.bot.enabled:false}")
    private boolean enabledOnStartup;

    @Value("${telegram.bot.token:}")
    private String configuredToken;

    public TelegramBotManager(AuthService authService) {
        this.authService = authService;
    }

    public boolean isBotActive() {
        return this.active;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startOnApplicationReady() {
        if (!enabledOnStartup) {
            log.info("ℹ️ Bot Telegram desativado no arranque (telegram.bot.enabled=false).");
            return;
        }
        if (configuredToken == null || configuredToken.isBlank()) {
            log.warn("⚠️ O token do Telegram não foi configurado. O Bot não será registado.");
            return;
        }
        try {
            startBot(configuredToken);
        } catch (Exception e) {
            log.error("❌ ERRO AO REGISTAR BOT: {}", e.getMessage());
        }
    }

    public synchronized void startBot(String token) throws Exception {
        String cleanToken = token == null ? "" : token.replaceAll("[\\p{Cntrl}\\s]", "").trim();
        if (cleanToken.length() <= 10 || !cleanToken.contains(":")) {
            throw new Exception("O token do Telegram tem formato inválido (esperado: <bot_id>:<secret>).");
        }

        try {
            if (botApp != null) {
                stopBot();
            }

            log.info("🤖 A iniciar bot com o token iniciado por: {}...", cleanToken.substring(0, 10));
            this.currentToken = cleanToken;
            this.botApp = new TelegramBotsLongPollingApplication();

            TelegramBotService botService = new TelegramBotService(authService, cleanToken);
            botApp.registerBot(cleanToken, botService);

            this.active = true;
            log.info("✅ Bot Telegram registado e a ouvir atualizações!");
        } catch (Exception e) {
            this.active = false;
            if (this.botApp != null) {
                try {
                    this.botApp.close();
                } catch (Exception ignored) {
                }
                this.botApp = null;
            }
            String detail = (e.getMessage() != null && !e.getMessage().isBlank())
                    ? e.getMessage()
                    : (e.getCause() != null ? e.getCause().toString() : e.getClass().getName());
            log.error("❌ Falha ao iniciar bot ({}):", detail, e);
            throw new Exception("Erro ao validar token com o Telegram: " + detail);
        }
    }

    public synchronized void stopBot() {
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
        if (!active) throw new ConflictException("Bot não está iniciado");
        log.info("A disparar teste de conectividade...");
    }

    @Override
    public void close() throws Exception {
        stopBot();
    }
}