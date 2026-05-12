package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.attachment.AttachmentDeleteRequestDto;
import com.example.socialnetwork.domain.dto.auth.RegisterRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttachmentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadListAndDeleteAttachmentShouldWork() throws Exception {
        RegisterResponse owner = register("attach-owner@mail.local");

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );

        MvcResult uploadResult = mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/attachments")
                        .file(file)
                        .param("type", "photo")
                        .header("Authorization", owner.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("photo"))
                .andExpect(jsonPath("$[0].attachment_key").value(org.hamcrest.Matchers.startsWith("photo_")))
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String attachmentKey = uploadJson.get(0).get("attachment_key").asText();

        mockMvc.perform(get("/api/attachments")
                        .header("Authorization", owner.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attachment_key").value(attachmentKey))
                .andExpect(jsonPath("$[0].width").value(1))
                .andExpect(jsonPath("$[0].height").value(1));

        AttachmentDeleteRequestDto deleteRequest = new AttachmentDeleteRequestDto();
        deleteRequest.setAttachmentKey(attachmentKey);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/attachments")
                        .header("Authorization", owner.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/attachments")
                        .header("Authorization", owner.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deletingForeignAttachmentShouldBeForbidden() throws Exception {
        RegisterResponse owner = register("attach-owner-2@mail.local");
        RegisterResponse stranger = register("attach-stranger@mail.local");

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );

        MvcResult uploadResult = mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/attachments")
                        .file(file)
                        .param("type", "photo")
                        .header("Authorization", owner.authorizationHeader()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String attachmentKey = uploadJson.get(0).get("attachment_key").asText();

        AttachmentDeleteRequestDto deleteRequest = new AttachmentDeleteRequestDto();
        deleteRequest.setAttachmentKey(attachmentKey);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/attachments")
                        .header("Authorization", stranger.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isForbidden());
    }

    private RegisterResponse register(String email) throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail(email);
        request.setPassword("StrongPassword123!");
        request.setFirstName("Attach");
        request.setLastName("User");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new RegisterResponse(json.get("accessToken").asText());
    }

    private byte[] pngBytes() {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Failed to build test png", ex);
        }
    }

    private record RegisterResponse(String accessToken) {
        private String authorizationHeader() {
            return "Bearer " + accessToken;
        }
    }
}
