package com.astral.express.pccms.filemedia.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class R2MediaStorageService implements CloudMediaStorageService {
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final String bucket;
    private final String publicBaseUrl;
    private final String cacheControl;
    private final S3Client s3Client;
    private final Clock clock;

    @Autowired
    public R2MediaStorageService(
            @Value("${pccms.r2.account-id:}") String accountId,
            @Value("${pccms.r2.access-key-id:}") String accessKeyId,
            @Value("${pccms.r2.secret-access-key:}") String secretAccessKey,
            @Value("${pccms.r2.bucket:}") String bucket,
            @Value("${pccms.r2.public-base-url:}") String publicBaseUrl,
            @Value("${pccms.r2.cache-control:public,max-age=31536000,immutable}") String cacheControl) {
        this(
                require(accountId, "pccms.r2.account-id"),
                require(accessKeyId, "pccms.r2.access-key-id"),
                require(secretAccessKey, "pccms.r2.secret-access-key"),
                require(bucket, "pccms.r2.bucket"),
                require(publicBaseUrl, "pccms.r2.public-base-url"),
                defaultCacheControl(cacheControl),
                null,
                Clock.systemUTC()
        );
    }

    R2MediaStorageService(
            String accountId,
            String accessKeyId,
            String secretAccessKey,
            String bucket,
            String publicBaseUrl,
            String cacheControl,
            S3Client s3Client,
            Clock clock) {
        this.bucket = require(bucket, "pccms.r2.bucket");
        this.publicBaseUrl = trimTrailingSlash(require(publicBaseUrl, "pccms.r2.public-base-url"));
        this.cacheControl = defaultCacheControl(cacheControl);
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.s3Client = s3Client == null
                ? buildClient(
                require(accountId, "pccms.r2.account-id"),
                require(accessKeyId, "pccms.r2.access-key-id"),
                require(secretAccessKey, "pccms.r2.secret-access-key"))
                : s3Client;
    }
    public StoredMedia store(StoreMediaCommand command) {
        String mimeType = command.contentType() == null ? "image/jpeg" : command.contentType();
        String objectKey = buildObjectKey(command);
        try {
            byte[] bytes = command.bytes();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(mimeType)
                    .contentLength((long) bytes.length)
                    .cacheControl(cacheControl)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            return new StoredMedia(objectKey, publicUrl(objectKey), mimeType, bytes.length);
        } catch (RuntimeException exception) {
            log.warn("Cloudflare R2 upload failed: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
    }

    private String buildObjectKey(StoreMediaCommand command) {
        String normalizedFolder = normalizeFolder(command.folder());
        String prefix = normalizedFolder.isBlank() ? "pccms" : "pccms/" + normalizedFolder;
        String datePath = DATE_PATH_FORMATTER.format(LocalDate.now(clock));
        return prefix + "/" + datePath + "/" + UUID.randomUUID() + "." + extensionFor(command);
    }

    private String publicUrl(String objectKey) {
        return publicBaseUrl + "/" + objectKey;
    }

    private static S3Client buildClient(String accountId, String accessKeyId, String secretAccessKey) {
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private static String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "";
        }
        String cleaned = folder.trim()
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        if (cleaned.equals("pccms")) {
            return "";
        }
        if (cleaned.startsWith("pccms/")) {
            cleaned = cleaned.substring("pccms/".length());
        }
        return cleaned.replaceAll("[^A-Za-z0-9/_-]", "-");
    }

    private static String extensionFor(StoreMediaCommand command) {
        String originalName = command.originalFilename();
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                String extension = originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
                if (extension.matches("[a-z0-9]{1,10}")) {
                    return extension;
                }
            }
        }
        return switch (command.contentType() == null ? "" : command.contentType()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg" -> "jpg";
            default -> "bin";
        };
    }

    private static String require(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required R2 configuration: " + propertyName);
        }
        return value.trim();
    }

    private static String defaultCacheControl(String cacheControl) {
        return cacheControl == null || cacheControl.isBlank()
                ? "public,max-age=31536000,immutable"
                : cacheControl.trim();
    }

    private static String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}

