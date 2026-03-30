package com.github.fiodarks.project26.archive.adapter.out.storage;

import com.github.fiodarks.project26.archive.application.port.in.MaterialUpload;
import com.github.fiodarks.project26.archive.application.port.out.MaterialFileStoragePort;
import com.github.fiodarks.project26.archive.application.port.out.StoredMaterialFile;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class LocalFilesystemMaterialFileStorageAdapter implements MaterialFileStoragePort {

    private final ArchiveStorageProperties properties;

    @Override
    public StoredMaterialFile store(UserId createdBy, MaterialId materialId, MaterialUpload upload) {
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(materialId, "materialId");
        Objects.requireNonNull(upload, "upload");

        var materialDir = properties.baseDir().resolve(materialId.value().toString());
        try {
            Files.createDirectories(materialDir);
            var target = materialDir.resolve("original" + safeExtension(upload.originalFilename()));
            Files.copy(upload.inputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            var fileUrl = URI.create(buildPublicUrl(materialId));
            return new StoredMaterialFile(fileUrl, fileUrl);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file for material: " + materialId.value(), e);
        }
    }

    @Override
    public void delete(MaterialId materialId) {
        Objects.requireNonNull(materialId, "materialId");
        var dir = properties.baseDir().resolve(materialId.value().toString());
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to delete file: " + path, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete material directory: " + dir, e);
        }
    }

    private String buildPublicUrl(MaterialId materialId) {
        var base = properties.publicBaseUrl();
        var relative = "/files/materials/" + materialId.value();
        if (base == null || base.isBlank()) {
            return relative;
        }
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1) + relative;
        }
        return base + relative;
    }

    private static String safeExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        var lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "";
        }
        var ext = originalFilename.substring(lastDot).toLowerCase();
        if (!ext.matches("^\\.[a-z0-9]{1,10}$")) {
            return "";
        }
        return ext;
    }
}
