package com.lilamaris.lilserve.reservation.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("웹 API 버전")
@WebMvcTest(controllers = ApiVersionTest.VersionProbeController.class)
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@Import({ApiVersionConfiguration.class, ApiVersionTest.VersionProbeController.class})
class ApiVersionTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("경로가 담고 버전 1로 요청하면 요청 mappings가 답한다")
    void answersRequestCarryingVersionOne() throws Exception {
        mockMvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("요청 mappings가 선언하지 않은 버전은 400으로 거부한다")
    void rejectsVersionNoMappingDeclares() throws Exception {
        mockMvc.perform(get("/api/v2/ping")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("버전 자리에 버전이 없는 요청은 400으로 거부한다")
    void rejectsRequestWithoutVersion() throws Exception {
        mockMvc.perform(get("/api/vabc/ping")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("버전 자리를 담지 못한 경로는 매칭이 없어 404로 답한다")
    void rejectsPathWithoutVersionPosition() throws Exception {
        mockMvc.perform(get("/api/ping")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("버전 자리까지 오지 못한 경로는 매칭이 없어 404로 답한다")
    void rejectsPathShorterThanTheVersionPosition() throws Exception {
        mockMvc.perform(get("/api")).andExpect(status().isNotFound());
    }

    /** The smallest mapping that declares the current version, so versioning is read on its own. */
    @RestController
    @RequestMapping(path = "/api/v{version}/ping", version = ApiVersionConfiguration.CURRENT)
    static class VersionProbeController {
        @GetMapping
        ResponseEntity<Void> ping() {
            return ResponseEntity.ok().build();
        }
    }
}
