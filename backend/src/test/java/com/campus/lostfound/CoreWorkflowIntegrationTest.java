package com.campus.lostfound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreWorkflowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void completePublishReviewAndClaimWorkflow() throws Exception {
        register("publisher01", "password123");
        register("applicant01", "password123");
        String publisherToken = login("publisher01", "password123");
        String applicantToken = login("applicant01", "password123");
        String adminToken = login("admin", "admin123");

        JsonNode categories = read(mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long categoryId = categories.path("data").get(0).path("id").asLong();

        Map<String, Object> itemPayload = Map.of(
                "name", "黑色校园卡",
                "type", "FOUND",
                "categoryId", categoryId,
                "location", "图书馆二楼",
                "eventTime", "2026-09-10T09:30:00",
                "description", "卡套背面有蓝色贴纸",
                "contact", "publisher@example.com"
        );
        JsonNode created = read(mockMvc.perform(post("/api/items")
                        .header("Authorization", bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
        long itemId = created.path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/items/{id}/review", itemId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"APPROVED\",\"comment\":\"信息完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/items").param("keyword", "校园卡"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("黑色校园卡"));

        JsonNode claimed = read(mockMvc.perform(post("/api/claims/items/{id}", itemId)
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"这是我的校园卡\",\"proof\":\"学号尾号 4392\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());
        long claimId = claimed.path("data").path("id").asLong();

        mockMvc.perform(patch("/api/claims/{id}/decision", claimId)
                        .header("Authorization", bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/claims/mine").header("Authorization", bearer(applicantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(applicantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isRead").value(false));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password, "contact", username + "@example.com"))))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return read(content).path("data").path("token").asText();
    }

    private JsonNode read(String content) throws Exception {
        return objectMapper.readTree(content);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
