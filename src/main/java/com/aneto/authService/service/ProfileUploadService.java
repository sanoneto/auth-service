package com.aneto.authService.service;


import com.aneto.authService.enums.ProfileImageType;
import com.aneto.authService.models.UserProfileImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface ProfileUploadService {

    String uploadImageAndSaveUrl(MultipartFile file, String userName) throws IOException;

    String saveImageToDatabase(MultipartFile file, String userName, ProfileImageType type, String baseApiUrl) throws IOException;

    Optional<UserProfileImage> getImage(String userName, ProfileImageType type);

    void removeImage(String userName, ProfileImageType type);

    Map<String, String> getAppearanceUrls(String userName);

    void deleteOldImage(String s3Key);
}