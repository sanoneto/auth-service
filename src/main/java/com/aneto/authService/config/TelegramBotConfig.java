package com.aneto.authService.config;

import com.aneto.authService.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
@Slf4j
public class TelegramBotConfig {

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(
            TelegramBotService telegramBotService,
            @Value("${telegram.bot.token.anetoBot}") String botToken) {

        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        try {
            String cleanToken = botToken.replaceAll("[\\p{Cntrl}\\s]", "").trim();

            // Log de segurança para debugar em produção
            if (cleanToken.length() > 10) {
                log.info("🤖 A tentar registar o bot com o token iniciado por: {}...", cleanToken.substring(0, 10));
            } else {
                log.error("❌ O token do Telegram parece ser demasiado curto ou inválido!");
            }

            botsApplication.registerBot(cleanToken, telegramBotService);
            log.info("✅ Bot do Telegram registado com sucesso!");
        } catch (Exception e) {
            log.error("❌ ERRO FATAL AO REGISTRAR BOT: {}", e.getMessage());
            // Não lançar exceção aqui permite que o resto do auth-service suba mesmo sem bot
        }
        return botsApplication;
    }
}