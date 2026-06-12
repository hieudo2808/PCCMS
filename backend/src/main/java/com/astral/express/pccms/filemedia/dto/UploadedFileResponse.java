package com.astral.express.pccms.filemedia.dto;

import java.util.UUID;

public record UploadedFileResponse(
        UUID id,
        String url,
        String publicId,
        String mimeType,
        long sizeBytes
) {
}
