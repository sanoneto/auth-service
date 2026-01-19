package com.aneto.authService.service;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Service
public class TelegramBotManager {

    private TelegramBotsLongPollingApplication botApp;

    @Getter // Isto cria o método getCurrentToken() automaticamente
    private String currentToken;

    private boolean active = false;

    // Resolve o erro: Cannot resolve method 'isBotActive'
    public boolean isBotActive() {
        return this.active;
    }

    public void startBot(String token) throws Exception {
        if (botApp != null && active) {
            botApp.close(); // Fecha a sessão anterior se existir
        }

        this.currentToken = token;
        this.botApp = new TelegramBotsLongPollingApplication();

        // Aqui deves registar o teu bot real (ex: MyBotHandler)
        // botApp.registerBot(token, new MyBotHandler(token));

        this.active = true;
    }

    public void sendTestMessage() {
        // Lógica para enviar mensagem usando o bot ativo
        if (!active) throw new RuntimeException("Bot não está iniciado");
        System.out.println("Enviando mensagem de teste...");
    }
}