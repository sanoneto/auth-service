package com.aneto.authService.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
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
@Table(name = "TB_USERS", schema = "AUTH", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

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
    private List<JwtToken> jwtToken = new ArrayList<>();

    @Setter
    private String verificationCode;

    @Setter
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = false;

    @Column(name = "google_token", length = 1000)
    private String googleToken;

    @Setter
    @Column(name = "facebook_id", unique = true)
    private String facebookId;

    @Column(name = "telegram_chat_id", unique = true)
    private String telegramChatId;

    @Setter
    @Getter
    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled = false;

    @Setter
    @Getter
    @Column(name = "mfa_secret")
    private String mfaSecret;

    // CONFIGURAÇÃO DE PERSISTÊNCIA DOS MÓDULOS
    @Getter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "USER_ALLOWED_MODULES",
            schema = "AUTH",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "module_name")
    private List<String> allowedModules = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Users() {
        this.jwtToken = new ArrayList<>();
        this.allowedModules = new ArrayList<>();
    }

    public Users(String username, String email, String password, String role, String profilePictureUrl, String verificationCode) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.verificationCode = verificationCode;
        this.profile_picture_url = profilePictureUrl;
        this.publicId = UUID.randomUUID();
        this.jwtToken = new ArrayList<>();
        this.allowedModules = new ArrayList<>();
    }

    @PrePersist
    @PreUpdate
    private void normalizeAndGenerateId() {
        if (this.username != null) {
            this.username = this.username.trim();
        }
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    // Método auxiliar para garantir que a gravação funcione no Service
    public void setAllowedModules(List<String> modules) {
        this.allowedModules.clear();
        if (modules != null) {
            this.allowedModules.addAll(modules);
        }
    }

    // --- Getters e Setters Manuais ---
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public List<JwtToken> getJwtToken() { return jwtToken; }
    public String getProfile_picture_url() { return profile_picture_url; }
    public String getUsername() { return username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getTelegramChatId() { return telegramChatId; }
    public String getGoogleToken() { return googleToken; }
    public String getFacebookId() { return facebookId; }
    public void setUsername(String username) { this.username = username != null ? username.trim().toLowerCase() : null; }
    public void setPublicId(UUID publicId) { this.publicId = publicId; }
    public void setJwtToken(List<JwtToken> jwtToken) { this.jwtToken = jwtToken; }
    public String getVerificationCode() { return verificationCode; }
    public boolean isEnabled() { return enabled; }
    public void setGoogleToken(String googleToken) { this.googleToken = googleToken; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}