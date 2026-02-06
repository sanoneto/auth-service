package com.aneto.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class TelegramBotService implements LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final AuthService authService;

    public TelegramBotService(AuthService authService, @Value("${telegram.bot.token}") String botToken) {
        this.authService = authService;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();

                if (messageText.startsWith("/start")) {
                    handleVinculo(messageText, chatId);
                }
            }
        }
    }

    private void handleVinculo(String text, String chatId) {
        try {
            String[] parts = text.split("\\s+");
            if (parts.length < 2) {
                enviarMensagem(chatId, "👋 Olá! Utilize o link de verificação no site para vincular a sua conta.");
                return;
            }

            UUID publicId = UUID.fromString(parts[1].trim());
            authService.vincularTelegram(publicId, chatId);

            enviarMensagem(chatId, "✅ *Vínculo realizado!*\nAgora receberá aqui as suas notificações.");
            log.info("Vínculo OK: {} -> {}", publicId, chatId);

        } catch (IllegalArgumentException e) {
            enviarMensagem(chatId, "⚠️ Link inválido.");
        } catch (Exception e) {
            log.error("Erro no vínculo: ", e);
            enviarMensagem(chatId, "❌ Erro interno no sistema.");
        }
    }

    private void enviarMensagem(String chatId, String texto) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .parseMode("Markdown")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar para {}: {}", chatId, e.getMessage());
        }
    }
}