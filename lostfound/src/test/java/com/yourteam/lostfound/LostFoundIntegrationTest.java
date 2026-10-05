package com.yourteam.lostfound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourteam.lostfound.dto.ClaimRequestDTO;
import com.yourteam.lostfound.dto.ItemRequestDTO;
import com.yourteam.lostfound.dto.UserLoginDTO;
import com.yourteam.lostfound.dto.UserRegisterDTO;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LostFoundIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static Long dynamicItemId;
    private static Long reporterId;
    private static Long claimantId;

    @BeforeEach
    void setUp() {
        User reporter = userRepository.findByEmail("reporter@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Alice Reporter");
            u.setEmail("reporter@example.com");
            u.setPassword("password123");
            u.setRole("USER");
            return userRepository.save(u);
        });
        reporterId = reporter.getId();

        User claimant = userRepository.findByEmail("claimant@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Bob Claimant");
            u.setEmail("claimant@example.com");
            u.setPassword("password123");
            u.setRole("USER");
            return userRepository.save(u);
        });
        claimantId = claimant.getId();
    }

    // 1. Auth: Registration and Login
    @Test
    @Order(1)
    void testAuthEndpoints() throws Exception {
        String uniqueEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        UserRegisterDTO regDto = new UserRegisterDTO();
        regDto.setName("New Registered User");
        regDto.setEmail(uniqueEmail);
        regDto.setPassword("secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(uniqueEmail));

        UserLoginDTO loginDto = new UserLoginDTO();
        loginDto.setEmail("reporter@example.com");
        loginDto.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // 2. Items: Post and query
    @Test
    @Order(2)
    @WithMockUser(username = "reporter@example.com")
    void testItemOperations() throws Exception {
        ItemRequestDTO itemDto = new ItemRequestDTO();
        itemDto.setTitle("MacBook Pro " + UUID.randomUUID().toString().substring(0, 4));
        itemDto.setDescription("Silver laptop with stickers");
        itemDto.setCategory("Electronics");
        itemDto.setLocation("Campus Library");
        itemDto.setStatus("FOUND");

        MvcResult result = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        dynamicItemId = json.get("id").asLong();

        mockMvc.perform(get("/api/items?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/items/category/Electronics"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/items/status/FOUND"))
                .andExpect(status().isOk());
    }

    // 3. Claims: Post claim and post comment
    @Test
    @Order(3)
    @WithMockUser(username = "claimant@example.com")
    void testValidClaimSubmissionAndComments() throws Exception {
        ClaimRequestDTO claimDto = new ClaimRequestDTO();
        claimDto.setItemId(dynamicItemId);
        claimDto.setClaimantId(claimantId);
        claimDto.setProofDescription("Blue tag attached to zipper");

        MvcResult claimResult = mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode json = objectMapper.readTree(claimResult.getResponse().getContentAsString());
        long claimId = json.get("id").asLong();

        String commentPayload = "{\"message\":\"Can you describe the serial number?\"}";

        // Expect 201 Created on comment creation
        mockMvc.perform(post("/api/claims/" + claimId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/claims/" + claimId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 4. In-App Messaging: Sending a message
    @Test
    @Order(4)
    @WithMockUser(username = "claimant@example.com")
    void testMessaging() throws Exception {
        String messagePayload = String.format(
                "{\"senderId\":%d,\"receiverId\":%d,\"recipientId\":%d,\"itemId\":%d,\"content\":\"Checking item status.\"}",
                claimantId, reporterId, reporterId, dynamicItemId
        );

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messagePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    // 5. Notifications: Query notifications list
    @Test
    @Order(5)
    @WithMockUser(username = "reporter@example.com")
    void testNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 6. Admin: Dashboard Stats
    @Test
    @Order(6)
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").exists());
    }
}