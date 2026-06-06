package com.astral.express.pccms.filemedia.service;

import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileMediaService {
    UploadedFileResponse uploadOwnerVisibleImage(MultipartFile file, UUID uploadedByUserId);
}
