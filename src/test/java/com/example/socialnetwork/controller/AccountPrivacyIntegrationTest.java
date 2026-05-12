package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.account.AccountPrivacyUpdateRequestDto;
import com.example.socialnetwork.domain.dto.account.AccountProfileUpdateRequestDto;
import com.example.socialnetwork.domain.dto.account.PrivacyRuleDto;
import com.example.socialnetwork.domain.dto.auth.RegisterRequestDto;
import com.example.socialnetwork.domain.model.PrivacyPolicyMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountPrivacyIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void privacySettingsShouldHideInfoAndCloseMessages() throws Exception {
        RegisterResponse owner = register("owner@mail.local", "Owner", "One");
        RegisterResponse viewer = register("viewer@mail.local", "Viewer", "Two");

        AccountProfileUpdateRequestDto profileUpdate = new AccountProfileUpdateRequestDto();
        profileUpdate.setAbout("Owner private bio");
        profileUpdate.setBirthDate(LocalDate.of(1990, 1, 1));

        mockMvc.perform(patch("/api/account/profile")
                        .header("Authorization", owner.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.about").value("Owner private bio"));

        mockMvc.perform(get("/api/users/" + owner.userId)
                        .header("Authorization", viewer.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.about").value("Owner private bio"))
                .andExpect(jsonPath("$.is_closed").value(false));

        AccountPrivacyUpdateRequestDto privacyUpdate = new AccountPrivacyUpdateRequestDto();
        privacyUpdate.setCanSeeInfo(new PrivacyRuleDto(PrivacyPolicyMode.NOBODY, List.of()));
        privacyUpdate.setCanMessageMe(new PrivacyRuleDto(PrivacyPolicyMode.ONLY_SELECTED, List.of(owner.userId)));

        mockMvc.perform(patch("/api/account/privacy")
                        .header("Authorization", owner.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privacyUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canSeeInfo.mode").value("NOBODY"))
                .andExpect(jsonPath("$.canMessageMe.mode").value("ONLY_SELECTED"));

        mockMvc.perform(get("/api/users/" + owner.userId)
                        .header("Authorization", viewer.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.about").doesNotExist())
                .andExpect(jsonPath("$.bdate").doesNotExist())
                .andExpect(jsonPath("$.is_closed").value(true));

        mockMvc.perform(get("/api/users/" + owner.userId)
                        .header("Authorization", owner.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.about").value("Owner private bio"))
                .andExpect(jsonPath("$.is_closed").value(false));
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

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = json.get("accessToken").asText();

        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode meJson = objectMapper.readTree(meResult.getResponse().getContentAsString());
        return new RegisterResponse(meJson.get("id").asLong(), accessToken);
    }

    private record RegisterResponse(Long userId, String accessToken) {
        private String authorizationHeader() {
            return "Bearer " + accessToken;
        }
    }
}
