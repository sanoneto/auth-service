package com.aneto.authService.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "PASSWORD_VAULT",
        schema = "AUTH",
        indexes = {
                @Index(name = "idx_vault_owner", columnList = "owner_username"),
                @Index(name = "idx_vault_public_id", columnList = "public_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PasswordVaultEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "owner_username", nullable = false, length = 120)
    private String ownerUsername;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "account_username", length = 200)
    private String accountUsername;

    @Column(name = "encrypted_password", nullable = false, length = 2000)
    private String encryptedPassword;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "favorite", nullable = false)
    private boolean favorite = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        if (this.ownerUsername != null) {
            this.ownerUsername = this.ownerUsername.trim().toLowerCase(Locale.ROOT);
        }
        if (this.category == null || this.category.isBlank()) {
            this.category = "Geral";
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
}