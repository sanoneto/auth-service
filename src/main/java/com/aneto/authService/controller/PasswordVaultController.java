package com.aneto.authService.controller;

import com.aneto.authService.dto.request.VaultEntryRequest;
import com.aneto.authService.dto.response.VaultEntryResponse;
import com.aneto.authService.service.PasswordVaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
public class PasswordVaultController {

    private final PasswordVaultService vaultService;

    @GetMapping
    public ResponseEntity<List<VaultEntryResponse>> listMyVault(Authentication authentication) {
        return ResponseEntity.ok(vaultService.listMyEntries(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<VaultEntryResponse> createEntry(
            Authentication authentication,
            @Valid @RequestBody VaultEntryRequest request
    ) {
        VaultEntryResponse created = vaultService.createEntry(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<VaultEntryResponse> updateEntry(
            Authentication authentication,
            @PathVariable UUID publicId,
            @Valid @RequestBody VaultEntryRequest request
    ) {
        return ResponseEntity.ok(vaultService.updateEntry(authentication.getName(), publicId, request));
    }

    @PatchMapping("/{publicId}/favorite")
    public ResponseEntity<VaultEntryResponse> toggleFavorite(
            Authentication authentication,
            @PathVariable UUID publicId
    ) {
        return ResponseEntity.ok(vaultService.toggleFavorite(authentication.getName(), publicId));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteEntry(
            Authentication authentication,
            @PathVariable UUID publicId
    ) {
        vaultService.deleteEntry(authentication.getName(), publicId);
        return ResponseEntity.noContent().build();
    }
}