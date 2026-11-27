package com.aneto.authService.repository;

import com.aneto.authService.enums.ProfileImageType;
import com.aneto.authService.models.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {

    Optional<UserProfileImage> findByUsernameIgnoreCaseAndImageType(String username, ProfileImageType imageType);

    void deleteByUsernameIgnoreCaseAndImageType(String username, ProfileImageType imageType);

    void deleteByUsernameIgnoreCase(String username);
}