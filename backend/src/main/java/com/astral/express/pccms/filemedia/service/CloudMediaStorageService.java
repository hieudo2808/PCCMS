package com.astral.express.pccms.filemedia.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudMediaStorageService {
    StoredMedia uploadImage(MultipartFile file, String folder);
    record StoredMedia(
            String publicId,
            String secureUrl,
            String mimeType,
            long sizeBytes
    ) {
    }
}
