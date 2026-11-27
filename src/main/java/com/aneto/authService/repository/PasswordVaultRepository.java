package com.aneto.authService.repository;

import com.aneto.authService.models.PasswordVaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordVaultRepository extends JpaRepository<PasswordVaultEntry, Long> {

    List<PasswordVaultEntry> findAllByOwnerUsernameIgnoreCaseOrderByFavoriteDescUpdatedAtDesc(String ownerUsername);

    Optional<PasswordVaultEntry> findByPublicIdAndOwnerUsernameIgnoreCase(UUID publicId, String ownerUsername);
}