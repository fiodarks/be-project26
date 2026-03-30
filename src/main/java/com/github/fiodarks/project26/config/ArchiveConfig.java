package com.github.fiodarks.project26.config;

import com.github.fiodarks.project26.archive.adapter.out.storage.ArchiveStorageProperties;
import com.github.fiodarks.project26.archive.adapter.out.geocoding.nominatim.NominatimGeocodingProperties;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyNodeStorePort;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyNodeQueryPort;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyRepositoryPort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialFileStoragePort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialHierarchyAggregationPort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialRepositoryPort;
import com.github.fiodarks.project26.archive.application.port.out.ReverseGeocodingPort;
import com.github.fiodarks.project26.archive.application.port.out.UserBlockStatusPort;
import com.github.fiodarks.project26.archive.application.service.ArchiveApplicationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
        ArchiveStorageProperties.class,
        ArchiveSecurityProperties.class,
        ArchiveBootstrapAdminProperties.class,
        GoogleOAuthProperties.class,
        ArchiveCorsProperties.class,
        NominatimGeocodingProperties.class
})
public class ArchiveConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ArchiveApplicationService archiveApplicationService(
            MaterialRepositoryPort materials,
            HierarchyRepositoryPort hierarchy,
            HierarchyNodeStorePort hierarchyNodeStore,
            HierarchyNodeQueryPort hierarchyNodeQuery,
            MaterialHierarchyAggregationPort materialHierarchyAggregation,
            MaterialFileStoragePort storage,
            ReverseGeocodingPort reverseGeocodingPort,
            Clock clock,
            UserBlockStatusPort userBlockStatusPort
    ) {
        return new ArchiveApplicationService(
                materials,
                hierarchy,
                hierarchyNodeStore,
                hierarchyNodeQuery,
                materialHierarchyAggregation,
                storage,
                reverseGeocodingPort,
                clock,
                userBlockStatusPort
        );
    }
}
