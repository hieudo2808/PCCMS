package com.astral.express.pccms.filemedia.service;

public interface CloudMediaStorageService {
    StoredMedia store(StoreMediaCommand command);

    record StoredMedia(
            String publicId,
            String secureUrl,
            String mimeType,
            long sizeBytes
    ) {
    }
}
