package com.aneto.authService.service.impl;

import com.aneto.authService.service.AuthService;
import com.aneto.authService.service.ProfileUploadService;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileUploadServiceImpl implements ProfileUploadService {

    // O S3Template substitui o AmazonS3 (SDK v1)
    private final S3Template s3Template;
    private final AuthService authService;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.s3.folder-name}")
    private String s3Folder;

    @Override
    public String uploadImageAndSaveUrl(MultipartFile file, String userName) {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = s3Folder + userName + "/profile/" + userName + "." + fileExtension;

        try (InputStream inputStream = file.getInputStream()) {
            // A sintaxe correta para passar o Content-Type no S3Template:
            var s3Resource = s3Template.upload(bucketName, key, inputStream,
                    ObjectMetadata.builder()
                            .contentType(file.getContentType())
                            .build()
            );
            // Obter a URL (o SDK v2 gera a URL baseada na região configurada)
            String publicUrl = s3Resource.getURL().toString();

            authService.UpdateProfile(userName, publicUrl);

            return publicUrl;

        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler o arquivo de imagem.", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "jpg";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex + 1) : "jpg";
    }

    @Override
    public void deleteOldImage(String s3Key) {
        s3Template.deleteObject(bucketName, s3Key);
    }
}