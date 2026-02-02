package com.aneto.authService.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(ProjectId.class)
@EntityListeners(AuditingEntityListener.class) // Habilita a auditoria automática
@Table(name = "TB_Projetos", schema = "AUTH")
public class Projects {

    @Id
    @NotBlank(message = "Username não pode ser vazio")
    private String username;

    @Id
    @NotBlank(message = "Nome do projeto não pode ser vazio")
    @Column(name = "project_name")
    private String projectName;

    private Double requiredHours;

    @Column(nullable = false, columnDefinition = "float8 default 1.1")
    private Double hourlyRate = 1.1;

    // Campos de Auditoria
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private Users users;
}