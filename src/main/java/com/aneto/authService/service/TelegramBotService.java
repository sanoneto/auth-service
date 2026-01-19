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
        // A biblioteca agora envia uma lista de atualizações de uma vez
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();

                if (messageText.startsWith("/start")) {
                    log.info("🤖 Comando /start recebido no Auth-Service. ChatID: {}", chatId);
                    handleVinculo(messageText, chatId); // Chamar a tua lógica de extração
                }
            }
        }
    }
    private void handleVinculo(String text, String chatId) {
        try {
            // O comando chega como "/start <UUID>"
            String[] parts = text.split("\\s+");

            if (parts.length < 2) {
                log.warn("⚠️ Comando /start sem parâmetro recebido do ChatID: {}", chatId);
                enviarMensagem(chatId, "👋 Olá! Para vincular a tua conta, utiliza o botão 'Verificar' no nosso site.");
                return;
            }

            String uuidPart = parts[1].trim();
            UUID publicId = UUID.fromString(uuidPart);

            // Grava no banco de dados através do serviço
            authService.vincularTelegram(publicId, chatId);

            enviarMensagem(chatId, "✅ *Conta vinculada com sucesso!*\n\nJá podes fechar o Telegram e voltar ao sistema.");
            log.info("✅ Vínculo realizado: PublicID {} -> ChatID {}", publicId, chatId);

        } catch (IllegalArgumentException e) {
            log.error("❌ UUID inválido recebido: {}", e.getMessage());
            enviarMensagem(chatId, "⚠️ O link de ativação parece estar incorreto. Tenta clicar novamente no site.");
        } catch (Exception e) {
            log.error("❌ Erro ao vincular: ", e);
            enviarMensagem(chatId, "❌ Ocorreu um erro interno. Tenta novamente mais tarde.");
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