package com.aneto.authService.models;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectId implements Serializable {
    private String username;
    private String projectName;
}