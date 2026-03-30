package com.github.fiodarks.project26.archive.application.port.out;

import java.net.URI;
import java.util.Objects;

public record StoredMaterialFile(URI fileUrl, URI thumbnailUrl) {
    public StoredMaterialFile {
        Objects.requireNonNull(fileUrl, "fileUrl");
    }
}

