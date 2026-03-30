package com.github.fiodarks.project26.archive.adapter.in.web;

import com.github.fiodarks.project26.adapter.in.web.api.HierarchyApi;
import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyNode;
import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyViewportResponse;
import com.github.fiodarks.project26.archive.adapter.in.web.mapper.HierarchyViewportWebMapper;
import com.github.fiodarks.project26.archive.adapter.in.web.mapper.HierarchyWebMapper;
import com.github.fiodarks.project26.archive.adapter.in.web.security.SpringSecurityActorResolver;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportInclude;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportRequest;
import com.github.fiodarks.project26.archive.application.service.ArchiveApplicationService;
import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class HierarchyController implements HierarchyApi {

    private final ArchiveApplicationService archive;
    private final SpringSecurityActorResolver actorResolver;

    @Override
    public ResponseEntity<HierarchyViewportResponse> hierarchyGet(
            List<Double> bbox,
            String level,
            UUID parentId,
            String search,
            Integer limit,
            List<String> include
    ) {
        actorResolver.requireActor();
        if (bbox == null || bbox.isEmpty()) {
            throw new com.github.fiodarks.project26.archive.application.exception.ValidationException("bbox is required");
        }

        EnumSet<HierarchyViewportInclude> includeSet = EnumSet.noneOf(HierarchyViewportInclude.class);
        if (include != null) {
            for (var entry : include) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                for (var rawToken : entry.split(",")) {
                    if (rawToken == null) {
                        continue;
                    }
                    var normalized = rawToken.trim().toLowerCase(Locale.ROOT);
                    if (normalized.isBlank()) {
                        continue;
                    }
                    includeSet.add(switch (normalized) {
                        case "ancestors" -> HierarchyViewportInclude.ANCESTORS;
                        case "counts" -> HierarchyViewportInclude.COUNTS;
                        case "bbox" -> HierarchyViewportInclude.BBOX;
                        default -> throw new com.github.fiodarks.project26.archive.application.exception.ValidationException(
                                "Unknown include value: " + rawToken
                        );
                    });
                }
            }
        }

        var request = HierarchyViewportRequest.builder()
                .bbox(GeoBoundingBox.fromBboxDoubles(bbox))
                .level(level)
                .parentId(parentId == null ? null : new HierarchyNodeId(parentId))
                .search(search)
                .limit(limit)
                .include(includeSet)
                .build();

        var result = archive.getHierarchyViewport(request);
        return ResponseEntity.ok(HierarchyViewportWebMapper.toDto(result));
    }

    @Override
    public ResponseEntity<HierarchyNode> hierarchyTreeGet() {
        actorResolver.requireActor();
        return ResponseEntity.ok(HierarchyWebMapper.toDto(archive.getHierarchy()));
    }
}
