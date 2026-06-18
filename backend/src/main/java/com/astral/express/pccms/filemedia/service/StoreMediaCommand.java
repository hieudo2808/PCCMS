package com.astral.express.pccms.filemedia.service;

public record StoreMediaCommand(
        String folder,
        String originalFilename,
        String contentType,
        byte[] bytes
) {
    public long sizeBytes() {
        return bytes == null ? 0L : bytes.length;
    }
}
