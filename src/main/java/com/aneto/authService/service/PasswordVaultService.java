package com.aneto.authService.service;

import com.aneto.authService.dto.request.VaultEntryRequest;
import com.aneto.authService.dto.response.VaultEntryResponse;

import java.util.List;
import java.util.UUID;

public interface PasswordVaultService {

    List<VaultEntryResponse> listMyEntries(String username);

    VaultEntryResponse createEntry(String username, VaultEntryRequest request);

    VaultEntryResponse updateEntry(String username, UUID publicId, VaultEntryRequest request);

    VaultEntryResponse toggleFavorite(String username, UUID publicId);

    void deleteEntry(String username, UUID publicId);
}