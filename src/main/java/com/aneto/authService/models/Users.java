package com.aneto.authService.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "TB_USERS", schema = "AUTH", uniqueConstraints = { // <--- Esquema configurado
        @UniqueConstraint(columnNames = "username")
})

public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId; // Não final, pois é gerado no @PrePersist

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username não pode ser vazio")
    private String username;

    @Setter
    @Column(unique = true, nullable = false)
    @Email(message = "email inválido")
    private String email;

    @Setter
    @Column(nullable = false)
    @JsonIgnore
    @NotBlank(message = "Password não pode ser vazia")
    private String password;

    @Setter
    @Column(nullable = false)
    @NotBlank(message = "Role não pode ser vazia")
    private String role;

    @Setter
    private String profile_picture_url;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    // Não usamos @ToString.Exclude, a implementação nativa de toString() é mais segura
    private List<JwtToken> jwtToken = new ArrayList<>();

    @Setter
    private String verificationCode;
    @Setter
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = false;

    @Column(name = "google_token", length = 1000) // Tokens do Google podem ser longos
    private String googleToken;

    @Setter
    @Column(name = "facebook_id", unique = true)
    private String facebookId;

    @Setter
    @Column(name = "telegram_chat_id", unique = true)
    private String telegramChatId;

    // Construtor sem argumentos para JPA
    public Users() {
        // Inicializa a lista aqui, se não for feito na declaração
        this.jwtToken = new ArrayList<>();
    }

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Construtor para criação de um NOVO usuário (Substitui o @Builder/@AllArgsConstructor)
    public Users(String username, String email, String password, String role, String profilePictureUrl, String verificationCode) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.verificationCode = verificationCode;
        this.profile_picture_url = profilePictureUrl;
        this.publicId = UUID.randomUUID(); // Inicialização explícita
        this.jwtToken = new ArrayList<>();
    }


    // --- Métodos de Normalização e Geração de ID (Limpos) ---

    @PrePersist
    @PreUpdate
    private void normalizeAndGenerateId() {
        // Normalização do Username (agora mais limpa)
        if (this.username != null) {
            this.username = this.username.trim();
        }
        // Geração do ID Público (Não precisa checar se é null se for feito no construtor)
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }


    // --- Getters e Setters Manuais (Apenas onde a lógica é necessária) ---

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public List<JwtToken> getJwtToken() {
        return jwtToken;
    }

    public String getProfile_picture_url() {
        return profile_picture_url;
    }

    // Getter com lógica
    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public String getGoogleToken() {
        return googleToken;
    }

    public String getFacebookId() {
        return facebookId;
    }

    // Setter com lógica (Mantido devido à regra de normalização)
    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : null;
    }

    // Setters restantes (Se forem necessários para o fluxo de atualização)
    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public void setJwtToken(List<JwtToken> jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setGoogleToken(String googleToken) {
        this.googleToken = googleToken;
    }

}
