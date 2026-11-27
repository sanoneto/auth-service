package com.aneto.authService.service.impl;

import com.aneto.authService.enums.ProfileImageType;
import com.aneto.authService.exception.ResourceNotFoundException;
import com.aneto.authService.models.UserProfileImage;
import com.aneto.authService.models.Users;
import com.aneto.authService.repository.UserProfileImageRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.ProfileUploadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileUploadServiceImpl implements ProfileUploadService {

    private final UserProfileImageRepository imageRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public String uploadImageAndSaveUrl(MultipartFile file, String userName) throws IOException {
        return saveImageToDatabase(file, userName, ProfileImageType.AVATAR, "");
    }

    @Override
    @Transactional
    public String saveImageToDatabase(MultipartFile file, String userName, ProfileImageType type, String baseApiUrl) throws IOException {
        String normalizedUser = userName.trim().toLowerCase(Locale.ROOT);

        Users user = usersRepository.findByUsername(normalizedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado: " + userName));

        UserProfileImage entity = imageRepository
                .findByUsernameIgnoreCaseAndImageType(normalizedUser, type)
                .orElseGet(UserProfileImage::new);

        entity.setUsername(normalizedUser);
        entity.setImageType(type);
        entity.setContentType(file.getContentType() != null ? file.getContentType() : "image/jpeg");
        entity.setData(file.getBytes());
        entity.setUpdatedAt(LocalDateTime.now());
        imageRepository.saveAndFlush(entity);

        String cleanBase = (baseApiUrl != null && !baseApiUrl.isBlank())
                ? baseApiUrl.replaceAll("/+$", "")
                : "";
        String imageUrl = cleanBase + "/api/v1/profile/image/" + normalizedUser + "/" + type.toPathSegment()
                + "?v=" + Instant.now().toEpochMilli();

        switch (type) {
            case AVATAR -> user.setProfile_picture_url(imageUrl);
            case COVER -> user.setCoverPictureUrl(imageUrl);
            case BACKGROUND -> user.setBackgroundPictureUrl(imageUrl);
        }
        usersRepository.saveAndFlush(user);

        log.info("Imagem [{}] guardada na DB para {} ({} bytes)", type, normalizedUser, entity.getData().length);
        return imageUrl;
    }

    @Override
    public Optional<UserProfileImage> getImage(String userName, ProfileImageType type) {
        if (userName == null || userName.isBlank()) {
            return Optional.empty();
        }
        return imageRepository.findByUsernameIgnoreCaseAndImageType(userName.trim().toLowerCase(Locale.ROOT), type);
    }

    @Override
    @Transactional
    public void removeImage(String userName, ProfileImageType type) {
        String normalizedUser = userName.trim().toLowerCase(Locale.ROOT);
        imageRepository.deleteByUsernameIgnoreCaseAndImageType(normalizedUser, type);

        usersRepository.findByUsername(normalizedUser).ifPresent(user -> {
            switch (type) {
                case AVATAR -> user.setProfile_picture_url(null);
                case COVER -> user.setCoverPictureUrl(null);
                case BACKGROUND -> user.setBackgroundPictureUrl(null);
            }
            usersRepository.saveAndFlush(user);
        });
    }

    @Override
    public Map<String, String> getAppearanceUrls(String userName) {
        String normalizedUser = userName.trim().toLowerCase(Locale.ROOT);
        Map<String, String> result = new HashMap<>();
        usersRepository.findByUsername(normalizedUser).ifPresent(user -> {
            result.put("avatarUrl", user.getProfile_picture_url());
            result.put("coverUrl", user.getCoverPictureUrl());
            result.put("backgroundUrl", user.getBackgroundPictureUrl());
        });
        return result;
    }

    @Override
    public void deleteOldImage(String s3Key) {
        // Sem operação externa (armazenamento migrado para PostgreSQL)
    }
}