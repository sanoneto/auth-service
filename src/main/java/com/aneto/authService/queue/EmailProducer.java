package com.aneto.authService.queue;


import com.aneto.authService.config.RabbitMQConfig;
import com.aneto.authService.dto.request.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailProducer {

    private static final Logger log = LoggerFactory.getLogger(EmailProducer.class);
    private final RabbitTemplate rabbitTemplate;

    public void sendRegistrationEmail(String recipientName, String recipientEmail, String subject, String message) {

        // Constrói o DTO específico que a fila espera
        EmailRequest emailRequest = new EmailRequest(
                recipientName,
                recipientEmail,
                subject,
                message
        );

        log.info("Enviando mensagem para a Exchange: {} com Routing Key: {}", RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY);

        // Envia a mensagem para a fila
        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, emailRequest);

        log.info("Mensagem de registo de e-mail enviada para a fila: {}", emailRequest.email());
    }
}