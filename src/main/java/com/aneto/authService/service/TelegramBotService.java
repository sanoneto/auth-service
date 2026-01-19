package com.aneto.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class TelegramBotService implements LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final AuthService authService; // O teu serviço de autenticação


    // O Spring faz o "Auto-vínculo" aqui
    public TelegramBotService(AuthService authService, @Value("${telegram.bot.token.anetoBot}") String botToken) {
        this.authService = authService;
        // Criamos o cliente usando o mesmo Token que está no properties
        // Substitui pela tua String real ou usa @Value("${telegram.botToken}")
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(update -> {
            log.info("--- NOVO UPDATE RECEBIDO: {} ---", update.getUpdateId());

            if (update.hasMessage()) {
                String text = update.getMessage().getText();
                String chatId = String.valueOf(update.getMessage().getChatId());
                log.info("Conteúdo da mensagem: [{}]", text); // Verifica se o UUID aparece aqui
                log.info("Chat ID: {}", chatId);

                // O Telegram às vezes envia o comando colado ao nome do bot: /start@meu_bot <ID>
                if (text != null && text.contains("/start")) {
                    handleVinculo(text, chatId);
                }
            } else {
                log.warn("Update recebido não contém mensagem.");
            }
        });
    }
    private void handleVinculo(String text, String chatId) {
        try {
            // Pega tudo o que vem depois do primeiro espaço (o UUID)
            String[] parts = text.split("\\s+");

            if (parts.length < 2) {
                log.error("Comando /start sem parâmetro. Texto recebido: [{}]", text);
                enviarMensagem(chatId, "❌ Link inválido. Clique no botão 'Vincular' no site.");
                return;
            }

            String uuidPart = parts[1].trim();
            log.info("UUID extraído: [{}]", uuidPart);

            UUID publicId = UUID.fromString(uuidPart);
            authService.vincularTelegram(publicId, chatId);

            enviarMensagem(chatId, "✅ Vínculo realizado com sucesso!");

        } catch (Exception e) {
            log.error("Erro ao vincular: ", e);
            enviarMensagem(chatId, "❌ Erro ao processar vínculo. Tente novamente.");
        }
    }

    private void enviarMensagem(String chatId, String texto) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .build();
        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}