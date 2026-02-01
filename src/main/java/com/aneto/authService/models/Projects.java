package com.aneto.authService.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TB_Projetos", schema = "AUTH",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "project_name"}, name = "UK_project_by_user")
)
public class Projects {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username não pode ser vazio")
    private String username;

    @NotBlank(message = "Nome do projeto não pode ser vazio")
    @Column(name = "project_name")
    private String projectName;

    @Column(nullable = false)
    private Double requiredHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private Users users;
}