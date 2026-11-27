package com.aneto.authService.service.impl;

import com.aneto.authService.dto.request.VaultEntryRequest;
import com.aneto.authService.dto.response.VaultEntryResponse;
import com.aneto.authService.enums.UserRole;
import com.aneto.authService.exception.ResourceNotFoundException;
import com.aneto.authService.models.PasswordVaultEntry;

import com.aneto.authService.models.Users;
import com.aneto.authService.repository.PasswordVaultRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.security.VaultCryptoUtil;
import com.aneto.authService.service.PasswordVaultService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordVaultServiceImpl implements PasswordVaultService {

    private static final String GROUP_NAME = "Dashboards";
    private static final String SUBMODULE_KEY = "Dashboards:Cofre de Senhas";

    private final PasswordVaultRepository vaultRepository;
    private final UsersRepository usersRepository;
    private final VaultCryptoUtil cryptoUtil;

    private void assertVaultPermission(String username) {
        Users user = usersRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        List<String> modules = user.getAllowedModules();
        if (modules == null) {
            throw new AccessDeniedException("Acesso ao Cofre de Senhas não atribuído.");
        }

        boolean hasExplicitSubmodule = modules.contains(SUBMODULE_KEY);
        boolean hasAnyGranularDashboard = modules.stream().anyMatch(m -> m.startsWith(GROUP_NAME + ":"));
        boolean hasLegacyGroup = !hasAnyGranularDashboard && modules.contains(GROUP_NAME);

        if (!hasExplicitSubmodule && !hasLegacyGroup) {
            throw new AccessDeniedException("Acesso ao Cofre de Senhas não atribuído pelo administrador.");
        }
    }

    private VaultEntryResponse toResponse(PasswordVaultEntry entity) {
        return new VaultEntryResponse(
                entity.getPublicId(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getAccountUsername(),
                cryptoUtil.decrypt(entity.getEncryptedPassword()),
                entity.getUrl(),
                entity.getNotes(),
                entity.isFavorite(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Override
    public List<VaultEntryResponse> listMyEntries(String username) {
        assertVaultPermission(username);
        return vaultRepository
                .findAllByOwnerUsernameIgnoreCaseOrderByFavoriteDescUpdatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VaultEntryResponse createEntry(String username, VaultEntryRequest request) {
        assertVaultPermission(username);

        PasswordVaultEntry entry = new PasswordVaultEntry();
        entry.setOwnerUsername(username);
        entry.setTitle(request.title().trim());
        entry.setCategory(request.category() != null && !request.category().isBlank()
                ? request.category().trim()
                : "Geral");
        entry.setAccountUsername(request.accountUsername() != null ? request.accountUsername().trim() : null);
        entry.setEncryptedPassword(cryptoUtil.encrypt(request.password()));
        entry.setUrl(request.url() != null ? request.url().trim() : null);
        entry.setNotes(request.notes() != null ? request.notes().trim() : null);
        entry.setFavorite(Boolean.TRUE.equals(request.favorite()));

        PasswordVaultEntry saved = vaultRepository.saveAndFlush(entry);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VaultEntryResponse updateEntry(String username, UUID publicId, VaultEntryRequest request) {
        assertVaultPermission(username);

        PasswordVaultEntry entry = vaultRepository
                .findByPublicIdAndOwnerUsernameIgnoreCase(publicId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada do cofre não encontrada."));

        entry.setTitle(request.title().trim());
        entry.setCategory(request.category() != null && !request.category().isBlank()
                ? request.category().trim()
                : "Geral");
        entry.setAccountUsername(request.accountUsername() != null ? request.accountUsername().trim() : null);
        entry.setEncryptedPassword(cryptoUtil.encrypt(request.password()));
        entry.setUrl(request.url() != null ? request.url().trim() : null);
        entry.setNotes(request.notes() != null ? request.notes().trim() : null);
        if (request.favorite() != null) {
            entry.setFavorite(request.favorite());
        }
        entry.setUpdatedAt(LocalDateTime.now());

        PasswordVaultEntry saved = vaultRepository.saveAndFlush(entry);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VaultEntryResponse toggleFavorite(String username, UUID publicId) {
        assertVaultPermission(username);

        PasswordVaultEntry entry = vaultRepository
                .findByPublicIdAndOwnerUsernameIgnoreCase(publicId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada do cofre não encontrada."));

        entry.setFavorite(!entry.isFavorite());
        entry.setUpdatedAt(LocalDateTime.now());
        return toResponse(vaultRepository.saveAndFlush(entry));
    }

    @Override
    @Transactional
    public void deleteEntry(String username, UUID publicId) {
        assertVaultPermission(username);

        PasswordVaultEntry entry = vaultRepository
                .findByPublicIdAndOwnerUsernameIgnoreCase(publicId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada do cofre não encontrada."));

        vaultRepository.delete(entry);
    }
}