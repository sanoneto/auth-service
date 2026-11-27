package com.aneto.authService.controller;

import com.aneto.authService.dto.response.UploadResponse;
import com.aneto.authService.enums.ProfileImageType;
import com.aneto.authService.models.UserProfileImage;
import com.aneto.authService.service.ProfileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L; // 5 MB

    private final ProfileUploadService profileUploadService;

    @PreAuthorize("hasAnyRole('ADMIN', 'ESPECIALISTA') or (hasAnyRole('ESTAGIARIO', 'USER') and #paramUsername == authentication.name)")
    @PostMapping
    public ResponseEntity<UploadResponse> uploadProfilePic(
            @RequestParam("profilePicture") MultipartFile file,
            @RequestParam("userName") String paramUsername,
            @RequestParam(value = "type", defaultValue = "avatar") String typeParam,
            HttpServletRequest request
    ) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            return new ResponseEntity<>(
                    new UploadResponse(null, "O ficheiro de imagem não pode estar vazio."),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            return new ResponseEntity<>(
                    new UploadResponse(null, "A imagem excede o limite máximo de 5 MB."),
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return new ResponseEntity<>(
                    new UploadResponse(null, "Formato de ficheiro não suportado. Use uma imagem."),
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE
            );
        }

        ProfileImageType imageType;
        try {
            imageType = ProfileImageType.fromString(typeParam);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    new UploadResponse(null, e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        }

        try {
            // Guarda caminho relativo (/api/v1/profile/image/{username}/{type}) para funcionar em qualquer ambiente/domínio
            String publicUrl = profileUploadService.saveImageToDatabase(file, paramUsername, imageType, "");
            log.info("Imagem [{}] guardada na DB para {} -> {}", imageType, paramUsername, publicUrl);
            return ResponseEntity.ok(new UploadResponse(publicUrl, "Imagem atualizada com sucesso!"));
        } catch (IOException e) {
            log.error("Erro ao ler bytes da imagem para {}: {}", paramUsername, e.getMessage());
            return new ResponseEntity<>(
                    new UploadResponse(null, "Erro interno do servidor ao guardar a imagem."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/image/{username}/{type}")
    public ResponseEntity<byte[]> getProfileImage(
            @PathVariable String username,
            @PathVariable String type
    ) {
        ProfileImageType imageType;
        try {
            imageType = ProfileImageType.fromString(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        Optional<UserProfileImage> imageOpt = profileUploadService.getImage(username, imageType);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfileImage img = imageOpt.get();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(img.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.IMAGE_JPEG;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + username + "-" + imageType.toPathSegment() + "\"")
                .body(img.getData());
    }

    @GetMapping("/appearance/{username}")
    public ResponseEntity<?> getAppearance(@PathVariable String username) {
        return ResponseEntity.ok(profileUploadService.getAppearanceUrls(username));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ESPECIALISTA') or (hasAnyRole('ESTAGIARIO', 'USER') and #username == authentication.name)")
    @DeleteMapping("/image/{username}/{type}")
    public ResponseEntity<Void> deleteProfileImage(
            @PathVariable String username,
            @PathVariable String type
    ) {
        ProfileImageType imageType = ProfileImageType.fromString(type);
        profileUploadService.removeImage(username, imageType);
        return ResponseEntity.noContent().build();
    }
}