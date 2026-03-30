package com.github.fiodarks.project26.archive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaterialsFlowIT {

    private static final Path uploadsDir = createUploadsDir();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("archive.storage.base-dir", () -> uploadsDir.toString());
        registry.add("archive.storage.public-base-url", () -> "");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void creator_can_create_update_search_and_delete_material() throws Exception {
        var creatorId = UUID.randomUUID();
        var hierarchyId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        var createdJson = mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "My photo")
                        .param("location", "Warsaw")
                        .param("creationDate", "1984-05")
                        .param("description", "Test description")
                        .param("hierarchyId", hierarchyId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId").value(creatorId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var materialId = objectMapper.readTree(createdJson).get("id").asText();

        mvc.perform(get("/api/v1/materials/{id}", materialId)
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(materialId));

        mvc.perform(get("/api/v1/materials")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533")
                        .param("search", "photo")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].photos[0].id").value(materialId));

        mvc.perform(put("/api/v1/materials/{id}", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated",
                                  "location": "Warsaw",
                                  "creationDate": "1984",
                                  "description": "Updated description",
                                  "hierarchyId": "%s",
                                  "metadata": {"camera":"Zenit"},
                                  "tags": ["tag1"]
                                }
                                """.formatted(hierarchyId))
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        mvc.perform(delete("/api/v1/materials/{id}", materialId)
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isNoContent());
    }

    @Test
    void viewer_can_filter_by_bbox_tags_metadata_and_hierarchy_affiliation() throws Exception {
        var creatorId = UUID.randomUUID();
        var cityId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var districtId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        var createdJson = mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Geo photo")
                        .param("location", "Warsaw")
                        .param("creationDate", "1984-05")
                        .param("description", "Has geo + metadata + tags")
                        .param("hierarchyId", districtId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var materialId = objectMapper.readTree(createdJson).get("id").asText();

        mvc.perform(put("/api/v1/materials/{id}", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Geo photo",
                                  "location": "Warsaw",
                                  "creationDate": "1984-05",
                                  "description": "Has geo + metadata + tags",
                                  "hierarchyId": "%s",
                                  "metadata": {"camera":"Zenit"},
                                  "tags": ["tag1"]
                                }
                                """.formatted(districtId))
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/materials")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533")
                        .param("filter", "camera=Zenit")
                        .param("tags", "tag1")
                        .param("hierarchyLevelId", cityId.toString())
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(1))
                .andExpect(jsonPath("$.points[0].photos.length()").value(1))
                .andExpect(jsonPath("$.points[0].photos[0].id").value(materialId));
    }

    @Test
    void viewer_can_fetch_hierarchy_viewport_nodes_by_bbox_and_level() throws Exception {
        var creatorId = UUID.randomUUID();
        var districtId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Viewport test")
                        .param("location", "Warsaw")
                        .param("creationDate", "1984-05")
                        .param("description", "Test description")
                        .param("hierarchyId", districtId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/hierarchy")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533")
                        .param("level", "country")
                        .param("include", "ancestors,counts,bbox")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("country"))
                .andExpect(jsonPath("$.root.name").value("Archive"))
                .andExpect(jsonPath("$.root.level").value("root"))
                .andExpect(jsonPath("$.root.children[0].name").value("Poland"))
                .andExpect(jsonPath("$.root.children[0].level").value("country"))
                .andExpect(jsonPath("$.root.children[0].hasChildren").value(true))
                .andExpect(jsonPath("$.root.children[0].stats.points").value(1))
                .andExpect(jsonPath("$.root.children[0].path[0].name").value("Archive"))
                .andExpect(jsonPath("$.root.children[0].extent[0]").value(21.01))
                .andExpect(jsonPath("$.root.children[0].extent[1]").value(52.25))
                .andExpect(jsonPath("$.pagination.truncated").value(false));
    }

    @Test
    void viewer_fetching_city_level_hierarchy_also_gets_upper_levels_in_path() throws Exception {
        var creatorId = UUID.randomUUID();
        var districtId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Viewport city test")
                        .param("location", "Warsaw")
                        .param("creationDate", "1984-05")
                        .param("description", "Test description")
                        .param("hierarchyId", districtId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/hierarchy")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533")
                        .param("level", "city")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("city"))
                .andExpect(jsonPath("$.root.name").value("Archive"))
                .andExpect(jsonPath("$.root.level").value("root"))
                .andExpect(jsonPath("$.root.children[0].name").value("Poland"))
                .andExpect(jsonPath("$.root.children[0].level").value("country"))
                .andExpect(jsonPath("$.root.children[0].children[0].name").value("Mazowieckie"))
                .andExpect(jsonPath("$.root.children[0].children[0].level").value("region"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].name").value("Warsaw"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].level").value("city"));
    }

    @Test
    void viewer_can_fetch_district_level_hierarchy_with_full_parent_path() throws Exception {
        var creatorId = UUID.randomUUID();
        var districtId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Viewport district test")
                        .param("location", "Warsaw")
                        .param("creationDate", "1984-05")
                        .param("description", "Test description")
                        .param("hierarchyId", districtId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/hierarchy")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533")
                        .param("level", "district")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("district"))
                .andExpect(jsonPath("$.root.name").value("Archive"))
                .andExpect(jsonPath("$.root.level").value("root"))
                .andExpect(jsonPath("$.root.children[0].name").value("Poland"))
                .andExpect(jsonPath("$.root.children[0].level").value("country"))
                .andExpect(jsonPath("$.root.children[0].children[0].name").value("Mazowieckie"))
                .andExpect(jsonPath("$.root.children[0].children[0].level").value("region"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].name").value("Warsaw"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].level").value("city"))
                .andExpect(jsonPath("$.root.children[0].children[0].children[0].children[0].level").value("district"));
    }

    @Test
    void viewer_cannot_request_hierarchy_level_below_district() throws Exception {
        mvc.perform(get("/api/v1/hierarchy")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533")
                        .param("level", "neighborhood")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewer_cannot_upload_material() throws Exception {
        var viewerId = UUID.randomUUID();
        var hierarchyId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Nope")
                        .param("location", "Warsaw")
                        .param("creationDate", "1984")
                        .param("description", "Test description")
                        .param("hierarchyId", hierarchyId.toString())
                        .with(jwt().jwt(j -> j.subject(viewerId.toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_can_fetch_materials_for_map_viewport() throws Exception {
        var creatorId = UUID.randomUUID();
        var hierarchyId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Public marker photo")
                        .param("location", "Warsaw")
                        .param("creationDate", "1999")
                        .param("description", "Public map marker")
                        .param("hierarchyId", hierarchyId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/materials")
                        .param("bbox", "20.8257837,52.1779306,21.1703081,52.3058533"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray());
    }

    @Test
    void anonymous_can_fetch_material_details() throws Exception {
        var creatorId = UUID.randomUUID();
        var hierarchyId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        var createdJson = mvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .param("title", "Public details photo")
                        .param("location", "Warsaw")
                        .param("creationDate", "1999")
                        .param("description", "Public details")
                        .param("hierarchyId", hierarchyId.toString())
                        .param("lat", "52.25")
                        .param("lon", "21.01")
                        .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var materialId = objectMapper.readTree(createdJson).get("id").asText();

        mvc.perform(get("/api/v1/materials/{id}", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(materialId));
    }

    @Test
    void anonymous_can_batch_fetch_material_previews() throws Exception {
        var creatorId = UUID.randomUUID();
        var hierarchyId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        var createdIds = new ArrayList<String>();
        for (int i = 0; i < 2; i++) {
            var createdJson = mvc.perform(multipart("/api/v1/materials")
                            .file(file)
                            .param("title", "Preview " + i)
                            .param("location", "Warsaw")
                            .param("creationDate", "1999")
                            .param("description", "Preview " + i)
                            .param("hierarchyId", hierarchyId.toString())
                            .param("lat", "52.25")
                            .param("lon", "21.01")
                            .with(jwt().jwt(j -> j.subject(creatorId.toString()).claim("roles", List.of("CREATOR")))))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            createdIds.add(objectMapper.readTree(createdJson).get("id").asText());
        }

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/materials/previews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "ids",
                                List.of(
                                        UUID.fromString(createdIds.get(0)),
                                        UUID.fromString(createdIds.get(1)),
                                        UUID.fromString("11111111-1111-1111-1111-111111111111")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").exists())
                .andExpect(jsonPath("$.notFoundIds.length()").value(1));
    }

    private static Path createUploadsDir() {
        try {
            return Files.createTempDirectory("project26-uploads-");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp uploads dir", e);
        }
    }
}
