package com.aneto.authService.config;

import com.aneto.authService.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = false)
public class TelegramBotConfig {

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(
            TelegramBotService telegramBotService,
            @Value("${telegram.bot.token:}") String botToken) {

        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        try {
            if (botToken == null || botToken.isBlank()) {
                log.warn("⚠️ O token do Telegram não foi configurado. O Bot não será registado.");
                return botsApplication;
            }

            String cleanToken = botToken.replaceAll("[\\p{Cntrl}\\s]", "").trim();

            if (cleanToken.length() > 10) {
                log.info("🤖 A tentar registar o bot com o token iniciado por: {}...", cleanToken.substring(0, 10));
            } else {
                log.error("❌ O token do Telegram parece ser demasiado curto ou inválido!");
                return botsApplication;
            }

            botsApplication.registerBot(cleanToken, telegramBotService);
            log.info("✅ Bot do Telegram registado com sucesso!");
        } catch (Exception e) {
            log.error("❌ ERRO AO REGISTAR BOT: {}", e.getMessage());
        }
        return botsApplication;
    }
}