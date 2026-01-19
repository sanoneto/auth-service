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
            // Remove espaços, aspas e caracteres de controle invisíveis
            String cleanToken = botToken.replaceAll("[\\p{Cntrl}\\s]", "").trim();

            botsApplication.registerBot(cleanToken, telegramBotService);
        } catch (Exception e) {
            log.error("ERRO FATAL AO REGISTRAR BOT: ", e);
        }
        return botsApplication;
    }
}