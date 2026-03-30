package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.application.port.in.MaterialUpload;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.UserId;

public interface MaterialFileStoragePort {
    StoredMaterialFile store(UserId createdBy, MaterialId materialId, MaterialUpload upload);

    void delete(MaterialId materialId);
}
