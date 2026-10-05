package com.aneto.authService.exception;

import com.aneto.authService.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        // Exceções do Spring com estado próprio (ex: ResponseStatusException) mantêm o seu código HTTP
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            log.warn("Erro HTTP {}: {}", springError.getStatusCode().value(), e.getMessage());
            return build(springError.getStatusCode(), e.getMessage());
        }
        log.error("Erro de runtime: {}", e.getMessage(), e);
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Argumento inválido: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error("Erro de validação: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Acesso negado: {}", e.getMessage());
        return build(HttpStatus.FORBIDDEN, "Acesso negado: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        // Exceções do Spring MVC (método não suportado, rota inexistente, media type inválido, ...)
        // trazem o código HTTP correto (405, 404, 415, ...) em vez de 500
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            log.warn("Erro HTTP {}: {}", springError.getStatusCode().value(), e.getMessage());
            return build(springError.getStatusCode(), e.getMessage());
        }
        // Não expor detalhes internos ao cliente; o stack trace fica no log
        log.error("Erro inesperado: {}", e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        log.warn("Falha de autenticação: {}", e.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Falha de autenticação: " + e.getMessage());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException e) {
        log.warn("Tentativa de login com conta não ativada: {}", e.getMessage());
        return build(HttpStatus.FORBIDDEN, "Esta conta ainda não foi ativada. Verifique o seu e-mail.");
    }

    // Restantes falhas de autenticação (ex: UsernameNotFoundException) → 401 em vez de 400
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        log.warn("Falha de autenticação: {}", e.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Falha de autenticação: " + e.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccess(ResourceAccessException e) {
        log.warn("Falha ao aceder a serviço externo: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Serviço externo indisponível. Tente novamente mais tarde.");
    }

    @ExceptionHandler(java.net.ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(java.net.ConnectException e) {
        log.warn("Falha de conexão: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Serviço remoto indisponível. Tente novamente mais tarde.");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        log.warn("Utilizador não encontrado: {}", e.getMessage());
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e) {
        log.warn("Token inválido ou expirado: {}", e.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado. " + e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados: {}", e.getMessage());
        return build(HttpStatus.CONFLICT, "Os dados já existem ou violam uma restrição.");
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        log.error("Violação de segurança (Código de convite): {}", e.getMessage());
        return build(HttpStatus.FORBIDDEN, e.getMessage()); // Mensagem: "Código de autorização inválido..."
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Recurso não encontrado: {}", e.getMessage());
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        log.warn("Conflito: {}", e.getMessage());
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro em falta: " + ex.getParameterName());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, status.value()));
    }
}
