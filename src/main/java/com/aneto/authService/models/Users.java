package com.aneto.authService.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_USERS", schema = "AUTH", uniqueConstraints = { // <--- Esquema configurado
        @UniqueConstraint(columnNames = "username")
})
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId; // Não final, pois é gerado no @PrePersist

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username não pode ser vazio")
    private String username;

    @Column(unique = true, nullable = false)
    @Email(message = "email inválido")
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    @NotBlank(message = "Password não pode ser vazia")
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Role não pode ser vazia")
    private String role;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    // Não usamos @ToString.Exclude, a implementação nativa de toString() é mais segura
    private List<JwtToken> jwtToken = new ArrayList<>();


    // Construtor sem argumentos para JPA
    public Users() {
        // Inicializa a lista aqui, se não for feito na declaração
        this.jwtToken = new ArrayList<>();
    }

    // Construtor para criação de um NOVO usuário (Substitui o @Builder/@AllArgsConstructor)
    public Users(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.publicId = UUID.randomUUID(); // Inicialização explícita
        this.jwtToken = new ArrayList<>();
    }


    // --- Métodos de Normalização e Geração de ID (Limpos) ---

    @PrePersist
    @PreUpdate
    private void normalizeAndGenerateId() {
        // Normalização do Username (agora mais limpa)
        if (this.username != null) {
            this.username = this.username.trim().toLowerCase();
        }
        // Geração do ID Público (Não precisa checar se é null se for feito no construtor)
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }


    // --- Getters e Setters Manuais (Apenas onde a lógica é necessária) ---

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public List<JwtToken> getJwtToken() { return jwtToken; }

    // Getter com lógica
    public String getUsername() { return username; }

    // Setter com lógica (Mantido devido à regra de normalização)
    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : null;
    }

    // Setters restantes (Se forem necessários para o fluxo de atualização)
    public void setPublicId(UUID publicId) { this.publicId = publicId; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setJwtToken(List<JwtToken> jwtToken) { this.jwtToken = jwtToken; }
}