package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.account.AccountPrivacyUpdateRequestDto;
import com.example.socialnetwork.domain.dto.account.PrivacyRuleDto;
import com.example.socialnetwork.domain.dto.auth.RegisterRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatOpenRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupApplyRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupCreateRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupInviteRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageEditRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageSendRequestDto;
import com.example.socialnetwork.domain.model.PrivacyPolicyMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommunicationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void directChatMessagingAndSearchShouldWork() throws Exception {
        RegisterResponse alice = register("alice@mail.local", "Alice", "Blue");
        RegisterResponse bob = register("bob@mail.local", "Bob", "Green");

        String attachmentKey = uploadPhoto(alice);

        ChatOpenRequestDto openRequest = new ChatOpenRequestDto();
        openRequest.setPeerId(bob.userId());

        MvcResult openResult = mockMvc.perform(post("/api/chats/open")
                        .header("Authorization", alice.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peer_id").value(bob.userId()))
                .andReturn();

        Long chatId = objectMapper.readTree(openResult.getResponse().getContentAsString()).get("id").asLong();

        MessageSendRequestDto sendRequest = new MessageSendRequestDto();
        sendRequest.setChatId(chatId);
        sendRequest.setMessage("hello bob");
        sendRequest.setAttachments(List.of(attachmentKey));

        MvcResult sendResult = mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", alice.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("hello bob"))
                .andExpect(jsonPath("$.attachments", hasSize(1)))
                .andReturn();

        Long messageId = objectMapper.readTree(sendResult.getResponse().getContentAsString()).get("id").asLong();

        MessageEditRequestDto editRequest = new MessageEditRequestDto();
        editRequest.setChatId(chatId);
        editRequest.setMessageId(messageId);
        editRequest.setNewMessage("hello bob edited");
        editRequest.setNewAttachments(List.of(attachmentKey));

        mockMvc.perform(patch("/api/messages/edit")
                        .header("Authorization", alice.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("hello bob edited"));

        mockMvc.perform(get("/api/messages/history")
                        .header("Authorization", bob.authorizationHeader())
                        .param("chat_id", chatId.toString())
                        .param("limit", "10")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].from_id").value(alice.userId()))
                .andExpect(jsonPath("$.items[0].attachments[0].attachment_key").value(attachmentKey))
                .andExpect(jsonPath("$.limit").value(10));

        mockMvc.perform(get("/api/chats")
                        .header("Authorization", bob.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chatId))
                .andExpect(jsonPath("$[0].last_message").value("hello bob edited"));

        mockMvc.perform(get("/api/chats/search")
                        .header("Authorization", bob.authorizationHeader())
                        .param("query", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].peer_name").value("Alice Blue"));
    }

    @Test
    void groupsAndPrivacyFlowShouldWork() throws Exception {
        RegisterResponse owner = register("group-owner@mail.local", "Group", "Owner");
        RegisterResponse blocked = register("blocked@mail.local", "Blocked", "User");
        RegisterResponse applicant = register("applicant@mail.local", "Apply", "User");

        AccountPrivacyUpdateRequestDto privacyUpdate = new AccountPrivacyUpdateRequestDto();
        privacyUpdate.setCanInviteMe(new PrivacyRuleDto(PrivacyPolicyMode.ONLY_SELECTED, List.of(applicant.userId())));

        mockMvc.perform(patch("/api/account/privacy")
                        .header("Authorization", blocked.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privacyUpdate)))
                .andExpect(status().isOk());

        GroupCreateRequestDto createRequest = new GroupCreateRequestDto();
        createRequest.setTitle("Backend Guild");

        MvcResult groupResult = mockMvc.perform(post("/api/groups/create")
                        .header("Authorization", owner.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Backend Guild"))
                .andReturn();

        Long groupId = objectMapper.readTree(groupResult.getResponse().getContentAsString()).get("id").asLong();

        GroupApplyRequestDto applyRequest = new GroupApplyRequestDto();
        applyRequest.setGroupId(groupId);

        mockMvc.perform(post("/api/groups/apply")
                        .header("Authorization", applicant.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/groups/invites")
                        .header("Authorization", owner.authorizationHeader())
                        .param("group_id", groupId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(applicant.userId()));

        GroupInviteRequestDto inviteRequest = new GroupInviteRequestDto();
        inviteRequest.setGroupId(groupId);
        inviteRequest.setUserIds(List.of(blocked.userId(), applicant.userId()));

        mockMvc.perform(post("/api/groups/invite")
                        .header("Authorization", owner.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(blocked.userId()));

        mockMvc.perform(get("/api/groups")
                        .header("Authorization", applicant.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(groupId));

        mockMvc.perform(get("/api/groups/members")
                        .header("Authorization", owner.authorizationHeader())
                        .param("group_id", groupId.toString())
                        .param("limit", "10")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(get("/api/search/groups")
                        .header("Authorization", owner.authorizationHeader())
                        .param("query", "Backend")
                        .param("limit", "10")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Backend Guild"));
    }

    private String uploadPhoto(RegisterResponse user) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );

        MvcResult uploadResult = mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/attachments")
                        .file(file)
                        .param("type", "photo")
                        .header("Authorization", user.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attachment_key").value(startsWith("photo_")))
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        return uploadJson.get(0).get("attachment_key").asText();
    }

    private RegisterResponse register(String email, String firstName, String lastName) throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail(email);
        request.setPassword("StrongPassword123!");
        request.setFirstName(firstName);
        request.setLastName(lastName);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode authJson = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = authJson.get("accessToken").asText();

        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode meJson = objectMapper.readTree(meResult.getResponse().getContentAsString());
        return new RegisterResponse(meJson.get("id").asLong(), accessToken);
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

    private record RegisterResponse(Long userId, String accessToken) {
        private String authorizationHeader() {
            return "Bearer " + accessToken;
        }
    }
}
