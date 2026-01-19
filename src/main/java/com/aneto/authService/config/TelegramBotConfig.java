package com.aneto.authService.config;

import com.aneto.authService.service.TelegramBotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(
            TelegramBotService telegramBotService,
            @Value("${telegram.bot.token.anetoBot}") String botToken) {

        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        try {
            // Isto é o que faz o bot começar a ouvir as mensagens do Telegram
            botsApplication.registerBot(botToken, telegramBotService);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return botsApplication;
    }
}