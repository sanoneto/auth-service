package com.aneto.authService.queue;


import com.aneto.authService.config.RabbitMQConfig;
import com.aneto.authService.dto.request.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishEmailRequest(String name, String email, String subject, String message, String resetLink) {

        EmailRequest emailRequest = new EmailRequest(
                name,
                email,
                subject,
                message,
                resetLink
        );

        log.info("Publicando evento de email para: {}", email);

        // Se isto falhar, o GlobalExceptionHandler assume o controlo!
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                emailRequest
        );

        log.info("Mensagem enviada para a fila.");
    }
}