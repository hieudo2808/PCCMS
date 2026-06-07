package com.astral.express.pccms.filemedia.service.impl;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.service.CloudMediaStorageService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryMediaStorageService implements CloudMediaStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryMediaStorageService(
            @Value("${pccms.cloudinary.cloud-name:}") String cloudName,
            @Value("${pccms.cloudinary.api-key:}") String apiKey,
            @Value("${pccms.cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Override
    public StoredMedia uploadImage(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"));
            String publicId = String.valueOf(result.get("public_id"));
            String secureUrl = String.valueOf(result.get("secure_url"));
            String mimeType = file.getContentType() == null ? "image/jpeg" : file.getContentType();
            return new StoredMedia(publicId, secureUrl, mimeType, file.getSize());
        } catch (IOException exception) {
            log.warn("Cloudinary upload failed: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
    }
}
