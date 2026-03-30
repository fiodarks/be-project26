package com.github.fiodarks.project26.archive.adapter.in.web;

import com.github.fiodarks.project26.archive.adapter.out.storage.ArchiveStorageProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MaterialFilesController {

    private final ArchiveStorageProperties storageProperties;

    @GetMapping("/files/materials/{id}")
    public ResponseEntity<Resource> getMaterialFile(@PathVariable UUID id) throws IOException {
        var dir = storageProperties.baseDir().resolve(id.toString());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return ResponseEntity.notFound().build();
        }

        Path file;
        try (var stream = Files.list(dir)) {
            file = stream
                    .filter(p -> p.getFileName().toString().startsWith("original"))
                    .findFirst()
                    .orElse(null);
        }
        if (file == null || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }

        var mediaType = MediaType.APPLICATION_OCTET_STREAM;
        var detected = Files.probeContentType(file);
        if (detected != null) {
            mediaType = MediaType.parseMediaType(detected);
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new FileSystemResource(file));
    }
}
