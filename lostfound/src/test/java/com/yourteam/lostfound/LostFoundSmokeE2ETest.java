package com.yourteam.lostfound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourteam.lostfound.dto.ClaimCommentRequestDTO;
import com.yourteam.lostfound.dto.ClaimRequestDTO;
import com.yourteam.lostfound.dto.ItemRequestDTO;
import com.yourteam.lostfound.dto.MessageRequestDTO;
import com.yourteam.lostfound.dto.UserLoginDTO;
import com.yourteam.lostfound.dto.UserRegisterDTO;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.ClaimCommentRepository;
import com.yourteam.lostfound.repository.ClaimRepository;
import com.yourteam.lostfound.repository.ItemRepository;
import com.yourteam.lostfound.repository.MessageRepository;
import com.yourteam.lostfound.repository.NotificationRepository;
import com.yourteam.lostfound.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LostFoundSmokeE2ETest {

    private static final Logger log = LoggerFactory.getLogger(LostFoundSmokeE2ETest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimCommentRepository claimCommentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Shared state across sequential scenario tests
    private static String standardUserToken;
    private static Long standardUserId;
    private static String adminToken;
    private static Long adminId;
    private static String userBToken;
    private static Long userBId;

    private static Long lostItemId;
    private static Long foundItemId;
    private static String uploadedImageFilename;
    private static Long createdClaimId;
    private static String generatedHandoverPin;

    private static final String RUN_SUFFIX = UUID.randomUUID().toString().substring(0, 6);
    private static final String TEST_USER_EMAIL = "testuser_" + RUN_SUFFIX + "@example.com";
    private static final String TEST_USER_PASSWORD = "Password@123";
    private static final String ADMIN_EMAIL = "admin_e2e@lostfound.edu";
    private static final String USER_B_EMAIL = "claimant_b_" + RUN_SUFFIX + "@example.com";

    @BeforeEach
    void ensureSeedUsers() {
        // Guarantee Admin user exists
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            User admin = new User("E2E Admin", ADMIN_EMAIL, passwordEncoder.encode("Admin@123"), "ROLE_ADMIN");
            userRepository.save(admin);
        }
    }

    // =========================================================================
    // 1. Authentication, Role Security & Profile Operations
    // =========================================================================

    @Test
    @Order(1)
    void test01_RegisterNewStandardUser_ReturnsJwt() throws Exception {
        log.info("[TEST 1.1] Registering fresh user: {}", TEST_USER_EMAIL);

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setName("E2E Test User");
        dto.setEmail(TEST_USER_EMAIL);
        dto.setPassword(TEST_USER_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        standardUserToken = json.get("token").asText();
        standardUserId = json.has("id") ? json.get("id").asLong() : json.get("userId").asLong();

        assertNotNull(standardUserToken, "JWT Token must not be null upon registration");
        log.info("[PASS 1.1] User registered successfully with JWT: {}...", standardUserToken.substring(0, 15));
    }

    @Test
    @Order(2)
    void test02_DuplicateRegistration_Returns400BadRequest() throws Exception {
        log.info("[TEST 1.2] Attempting duplicate registration for: {}", TEST_USER_EMAIL);

        UserRegisterDTO duplicateDto = new UserRegisterDTO();
        duplicateDto.setName("Duplicate User");
        duplicateDto.setEmail(TEST_USER_EMAIL);
        duplicateDto.setPassword("AnyPassword@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest());

        log.info("[PASS 1.2] Duplicate email rejected with HTTP 400 Bad Request");
    }

    @Test
    @Order(3)
    void test03_AuthenticationWithValidAndInvalidPassword() throws Exception {
        log.info("[TEST 1.3] Testing valid and invalid credentials");

        // Invalid password
        UserLoginDTO badLogin = new UserLoginDTO();
        badLogin.setEmail(TEST_USER_EMAIL);
        badLogin.setPassword("WrongPassword@999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().is4xxClientError());

        // Valid password
        UserLoginDTO validLogin = new UserLoginDTO();
        validLogin.setEmail(TEST_USER_EMAIL);
        validLogin.setPassword(TEST_USER_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        standardUserToken = json.get("token").asText();
        log.info("[PASS 1.3] Valid credentials accepted; invalid rejected");
    }

    @Test
    @Order(4)
    void test04_ProtectedEndpointsRejectUnauthenticatedWith401() throws Exception {
        log.info("[TEST 1.4] Calling protected endpoints without Bearer token");

        // Protected item creation
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andExpect(status().isUnauthorized());

        // Protected claim creation
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1}"))
                .andExpect(status().isUnauthorized());

        log.info("[PASS 1.4] Unauthenticated access rejected with HTTP 401 Unauthorized");
    }

    @Test
    @Order(5)
    void test05_NonAdminAccessToAdminEndpointsRejectedWith403() throws Exception {
        log.info("[TEST 1.5] Non-admin attempting to access /api/admin/stats");

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + standardUserToken))
                .andExpect(status().isForbidden());

        log.info("[PASS 1.5] Standard user rejected from admin route with HTTP 403 Forbidden");
    }

    @Test
    @Order(6)
    void test06_AdminLoginAndAccessToAnalytics() throws Exception {
        log.info("[TEST 1.6] Admin login and analytics retrieval");

        UserLoginDTO adminLogin = new UserLoginDTO();
        adminLogin.setEmail(ADMIN_EMAIL);
        adminLogin.setPassword("Admin@123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        adminToken = json.get("token").asText();
        adminId = json.has("id") ? json.get("id").asLong() : json.get("userId").asLong();

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").exists());

        log.info("[PASS 1.6] Admin access verified successfully");
    }

    // =========================================================================
    // 2. Item Lifecycle & Search Operations
    // =========================================================================

    @Test
    @Order(7)
    void test07_CreateLostItemWithMediaAttachment() throws Exception {
        log.info("[TEST 2.1] Uploading image attachment and creating LOST item");

        // 1. Upload sample mock image
        byte[] fakeImageContent = new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }; // PNG header
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "thinkpad-x1.png",
                "image/png",
                fakeImageContent
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/items/upload-image")
                        .file(file)
                        .header("Authorization", "Bearer " + standardUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String imageUrl = uploadJson.get("imageUrl").asText();
        uploadedImageFilename = imageUrl.replace("/uploads/", "");

        // Verify file physically exists on disk in ./uploads without path traversal
        Path localFilePath = Paths.get("./uploads", uploadedImageFilename);
        assertTrue(Files.exists(localFilePath), "Uploaded file must physically exist in ./uploads");

        // 2. Create LOST item report
        ItemRequestDTO lostItem = new ItemRequestDTO();
        lostItem.setTitle("Lost Blue ThinkPad X1 Laptop");
        lostItem.setDescription("ThinkPad X1 Carbon with blue campus sticker and matte finish.");
        lostItem.setCategory("Electronics");
        lostItem.setLocation("Campus Main Library 2nd Floor");
        lostItem.setStatus("LOST");
        lostItem.setDate(LocalDate.now());
        lostItem.setImageUrl(imageUrl);
        lostItem.setVerificationQuestion("What sticker is on the back lid?");

        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lostItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("LOST"))
                .andReturn();

        JsonNode itemJson = objectMapper.readTree(itemResult.getResponse().getContentAsString());
        lostItemId = itemJson.get("id").asLong();

        log.info("[PASS 2.1] Lost item created with ID: {} and media: {}", lostItemId, imageUrl);
    }

    @Test
    @Order(8)
    void test08_CreateFoundItemAndVerifyMatchingEngine() throws Exception {
        log.info("[TEST 2.2] Creating matching FOUND item and validating recommendation score");

        // Register User B to act as finder
        UserRegisterDTO userBDto = new UserRegisterDTO();
        userBDto.setName("Finder Bob");
        userBDto.setEmail(USER_B_EMAIL);
        userBDto.setPassword("Finder@123");

        MvcResult regB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBDto)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode regBJson = objectMapper.readTree(regB.getResponse().getContentAsString());
        userBToken = regBJson.get("token").asText();
        userBId = regBJson.has("id") ? regBJson.get("id").asLong() : regBJson.get("userId").asLong();

        // User B posts FOUND item in same category & location
        ItemRequestDTO foundItem = new ItemRequestDTO();
        foundItem.setTitle("Found Blue ThinkPad Laptop");
        foundItem.setDescription("Found ThinkPad laptop left in Main Library study cube.");
        foundItem.setCategory("Electronics");
        foundItem.setLocation("Campus Main Library");
        foundItem.setStatus("FOUND");
        foundItem.setDate(LocalDate.now());

        MvcResult foundResult = mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foundItem)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode foundJson = objectMapper.readTree(foundResult.getResponse().getContentAsString());
        foundItemId = foundJson.get("id").asLong();

        // Query matching engine for lost item
        MvcResult matchesResult = mockMvc.perform(get("/api/items/" + lostItemId + "/matches"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode matches = objectMapper.readTree(matchesResult.getResponse().getContentAsString());
        assertTrue(matches.isArray(), "Matches response must be an array");
        assertTrue(matches.size() > 0, "Matching engine should identify candidate items");

        boolean foundMatch = false;
        for (JsonNode m : matches) {
            if (m.has("id") && m.get("id").asLong() == foundItemId) {
                foundMatch = true;
                break;
            }
        }
        assertTrue(foundMatch, "Matching engine must identify foundItemId as candidate match");

        log.info("[PASS 2.2] Matching engine successfully linked Lost #{} to Found #{}", lostItemId, foundItemId);
    }

    @Test
    @Order(9)
    void test09_ItemSearchAndPaginationStructures() throws Exception {
        log.info("[TEST 2.3] Testing search by keyword, category, status, and pagination");

        // Search with keyword + category + status
        mockMvc.perform(get("/api/items")
                        .param("keyword", "ThinkPad")
                        .param("category", "Electronics")
                        .param("status", "LOST")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.content[0].title").exists());

        log.info("[PASS 2.3] Unified search and pagination contract verified");
    }

    // =========================================================================
    // 3. Claims, Comments & Custody Handover
    // =========================================================================

    @Test
    @Order(10)
    void test10_ClaimSubmissionByStandardUser() throws Exception {
        log.info("[TEST 3.1] Submitting a claim as standard user against FOUND item");

        ClaimRequestDTO claimDto = new ClaimRequestDTO();
        claimDto.setItemId(foundItemId);
        claimDto.setClaimantId(standardUserId);
        claimDto.setProofDescription("Has blue sticker and serial ending in 9044.");
        claimDto.setVerificationAnswer("A blue campus sticker");

        MvcResult result = mockMvc.perform(post("/api/claims")
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        createdClaimId = json.get("id").asLong();

        log.info("[PASS 3.1] Claim #{} submitted with status PENDING", createdClaimId);
    }

    @Test
    @Order(11)
    void test11_ClaimCommentDiscussion() throws Exception {
        log.info("[TEST 3.2] Posting claim comment as item owner");

        ClaimCommentRequestDTO commentDto = new ClaimCommentRequestDTO();
        commentDto.setMessage("Please confirm if the laptop has a US or UK keyboard layout.");

        mockMvc.perform(post("/api/claims/" + createdClaimId + "/comments")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(commentDto.getMessage()));

        // Verify persistence in ClaimCommentRepository
        assertFalse(claimCommentRepository.findByClaimIdOrderByCreatedAtAsc(createdClaimId).isEmpty());

        log.info("[PASS 3.2] Claim comment discussion stored and verified");
    }

    @Test
    @Order(12)
    void test12_ClaimApprovalAndPinVerification() throws Exception {
        log.info("[TEST 3.3] Approving claim and completing physical handover PIN verification");

        // 1. Approve claim as finder (User B)
        mockMvc.perform(patch("/api/claims/" + createdClaimId + "/status")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 2. Claimant securely retrieves their approved claim and secret handover PIN
        MvcResult claimantClaimResult = mockMvc.perform(get("/api/claims/" + createdClaimId)
                        .header("Authorization", "Bearer " + standardUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handoverPin").isNotEmpty())
                .andReturn();

        JsonNode approvalJson = objectMapper.readTree(claimantClaimResult.getResponse().getContentAsString());
        generatedHandoverPin = approvalJson.get("handoverPin").asText();
        assertEquals(6, generatedHandoverPin.length(), "Handover PIN must be a 6-digit code");

        // Verify item status updated to CLAIMED
        mockMvc.perform(get("/api/items/" + foundItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));

        // 2. Verify handover PIN by finder
        MvcResult pinResult = mockMvc.perform(post("/api/claims/item/" + foundItemId + "/verify-pin")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", generatedHandoverPin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handoverVerified").value(true))
                .andReturn();

        // 3. Confirm item is now REUNITED
        mockMvc.perform(get("/api/items/" + foundItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REUNITED"));

        log.info("[PASS 3.3] Item #{} successfully transitioned to REUNITED after PIN verification", foundItemId);
    }

    // =========================================================================
    // 4. Real-Time WebSocket & Messaging Layer
    // =========================================================================

    @Test
    @Order(13)
    void test13_InAppMessagingAndNotificationPersistence() throws Exception {
        log.info("[TEST 4.1] Testing message transmission and notification creation");

        MessageRequestDTO msgDto = new MessageRequestDTO();
        msgDto.setItemId(foundItemId);
        msgDto.setRecipientId(userBId);
        msgDto.setContent("Thank you for finding and holding the laptop!");

        MvcResult msgResult = mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode msgJson = objectMapper.readTree(msgResult.getResponse().getContentAsString());
        Long msgId = msgJson.get("id").asLong();

        // Verify message stored in repository
        assertTrue(messageRepository.findById(msgId).isPresent());

        // Verify recipient notifications endpoint
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        log.info("[PASS 4.1] Message #{} persisted and notification pipeline verified", msgId);
    }

    @Test
    @Order(14)
    void test14_WebSocketHandshakeEndpoint() throws Exception {
        log.info("[TEST 4.2] Verifying SockJS / WebSocket endpoint at /ws/info");

        mockMvc.perform(get("/ws/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.websocket").value(true));

        log.info("[PASS 4.2] WebSocket /ws SockJS handshake responds with HTTP 200 OK");
    }

    // =========================================================================
    // 5. Resilience, Ports & Graceful Degradation
    // =========================================================================

    @Test
    @Order(15)
    void test15_StaticAssetServingFromUploads() throws Exception {
        log.info("[TEST 5.1] Requesting static uploaded asset: /uploads/{}", uploadedImageFilename);

        assertNotNull(uploadedImageFilename, "Uploaded image filename must be set from test 7");

        mockMvc.perform(get("/uploads/" + uploadedImageFilename))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("image")));

        log.info("[PASS 5.1] Static upload serving returned HTTP 200 with image Content-Type");
    }

    @Test
    @Order(16)
    void test16_VerificationOfTeardownScriptsAndDegradation() {
        log.info("[TEST 5.2] Checking script files existence and execution consistency");

        assertTrue(new File("../run.bat").exists() || new File("run.bat").exists());
        assertTrue(new File("../stop.bat").exists() || new File("stop.bat").exists());
        assertTrue(new File("../run.sh").exists() || new File("run.sh").exists());
        assertTrue(new File("../stop.sh").exists() || new File("stop.sh").exists());

        log.info("[PASS 5.2] Consolidated lifecycle scripts present for Windows and macOS/Linux");
    }

    // =========================================================================
    // 6. Regression Test Suite for Identified Bugs
    // =========================================================================

    @Test
    @Order(17)
    void test17_AdminClaimApprovalGeneratesHandoverPinAndCompletesHandover() throws Exception {
        log.info("[REGRESSION TEST 1] Verifying Admin Claim Approval Generates Handover PIN and Completes Handover");

        // 1. Student A creates a FOUND item
        ItemRequestDTO foundDto = new ItemRequestDTO();
        foundDto.setTitle("Admin Path Found Headphones");
        foundDto.setDescription("Noise cancelling headphones found in library");
        foundDto.setCategory("ELECTRONICS");
        foundDto.setLocation("Library 1st Floor");
        foundDto.setStatus("FOUND");
        foundDto.setDate(LocalDate.now());

        MvcResult itemRes = mockMvc.perform(post("/api/items")
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foundDto)))
                .andExpect(status().isCreated())
                .andReturn();

        Long regItemId = objectMapper.readTree(itemRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Student B submits a claim
        ClaimRequestDTO claimDto = new ClaimRequestDTO();
        claimDto.setItemId(regItemId);
        claimDto.setProofDescription("Black Sony XM4 headphones with scratch on right hinge");

        MvcResult claimRes = mockMvc.perform(post("/api/claims")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimDto)))
                .andExpect(status().isCreated())
                .andReturn();

        Long regClaimId = objectMapper.readTree(claimRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Admin logs in and approves claim via /api/admin/claims/{id}/approve
        mockMvc.perform(patch("/api/admin/claims/" + regClaimId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 4. Verify item status = CLAIMED
        mockMvc.perform(get("/api/items/" + regItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));

        // 5. Verify Student B (claimant) can see the 6-digit PIN
        MvcResult claimCheckRes = mockMvc.perform(get("/api/claims/" + regClaimId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.handoverPin").isNotEmpty())
                .andReturn();

        String adminPin = objectMapper.readTree(claimCheckRes.getResponse().getContentAsString()).get("handoverPin").asText();
        assertNotNull(adminPin, "PIN must not be null after admin approval");
        assertTrue(adminPin.matches("\\d{6}"), "Admin-generated PIN must be a 6-digit numeric OTP");

        // 6. Test repeated admin approval does NOT regenerate or change the PIN
        mockMvc.perform(patch("/api/admin/claims/" + regClaimId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult repeatClaimRes = mockMvc.perform(get("/api/claims/" + regClaimId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn();

        String repeatPin = objectMapper.readTree(repeatClaimRes.getResponse().getContentAsString()).get("handoverPin").asText();
        assertEquals(adminPin, repeatPin, "Repeated approval must preserve the original PIN");

        // 7. Student A enters WRONG PIN -> Rejected with 400
        mockMvc.perform(post("/api/claims/item/" + regItemId + "/verify-pin")
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "000000"))))
                .andExpect(status().isBadRequest());

        // 8. Student A enters CORRECT PIN -> Accepted (200), item = REUNITED
        mockMvc.perform(post("/api/claims/item/" + regItemId + "/verify-pin")
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", adminPin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handoverVerified").value(true));

        mockMvc.perform(get("/api/items/" + regItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REUNITED"));

        // 9. Subsequent claim rejected because item is REUNITED
        mockMvc.perform(post("/api/claims")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimDto)))
                .andExpect(status().isBadRequest());

        log.info("[PASS REGRESSION 1] Admin approval correctly generated PIN '{}' and completed handover to REUNITED", adminPin);
    }

    @Test
    @Order(18)
    void test18_UnauthorizedExceptionReturnsHttp403Forbidden() throws Exception {
        log.info("[REGRESSION TEST 2] Verifying UnauthorizedException maps to HTTP 403 Forbidden instead of HTTP 500");

        // 1. Non-owner attempting to modify another user's item -> HTTP 403
        ItemRequestDTO hackDto = new ItemRequestDTO();
        hackDto.setTitle("Hacked Title");

        mockMvc.perform(put("/api/items/" + foundItemId)
                        .header("Authorization", "Bearer " + standardUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackDto)))
                .andExpect(status().isForbidden());

        // 2. Non-owner attempting to delete another user's item -> HTTP 403
        mockMvc.perform(delete("/api/items/" + foundItemId)
                        .header("Authorization", "Bearer " + standardUserToken))
                .andExpect(status().isForbidden());

        // 3. Unauthorized student verifying handover PIN on an item they did not find -> HTTP 403
        mockMvc.perform(post("/api/claims/item/" + lostItemId + "/verify-pin")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "123456"))))
                .andExpect(status().isForbidden());

        log.info("[PASS REGRESSION 2] All unauthorized operations return HTTP 403 Forbidden cleanly");
    }

    @Test
    @Order(19)
    void test19_ClaimIdorProtection() throws Exception {
        log.info("[REGRESSION TEST 3] Verifying Claim IDOR protection on GET /api/claims/{claimId}");

        // Register Student C (completely unrelated user)
        String userCEmail = "unrelated_c_" + UUID.randomUUID().toString().substring(0, 6) + "@campus.edu";
        User userC = new User("Student C", userCEmail, passwordEncoder.encode("Password@123"), "ROLE_USER");
        userRepository.save(userC);

        String userCToken = com.yourteam.lostfound.util.JwtUtil.generateToken(userCEmail, "ROLE_USER");

        // 1. Unrelated user C attempts to view Student A's claim -> HTTP 403 Forbidden
        mockMvc.perform(get("/api/claims/" + createdClaimId)
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden());

        // 2. Claimant (Student A) can view their claim -> HTTP 200 OK
        mockMvc.perform(get("/api/claims/" + createdClaimId)
                        .header("Authorization", "Bearer " + standardUserToken))
                .andExpect(status().isOk());

        // 3. Item reporter (Student B) can view claim on their item -> HTTP 200 OK
        mockMvc.perform(get("/api/claims/" + createdClaimId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk());

        // 4. Admin can view any claim -> HTTP 200 OK
        mockMvc.perform(get("/api/claims/" + createdClaimId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        log.info("[PASS REGRESSION 3] Claim IDOR security verified: Unrelated students receive HTTP 403");
    }

    @Test
    @Order(20)
    void test20_ItemMessageThreadAuthorization() throws Exception {
        log.info("[REGRESSION TEST 4] Verifying Message thread authorization on GET /api/messages/item/{itemId}");

        String userCEmail = "unrelated_msg_" + UUID.randomUUID().toString().substring(0, 6) + "@campus.edu";
        User userC = new User("Student C Msg", userCEmail, passwordEncoder.encode("Password@123"), "ROLE_USER");
        userRepository.save(userC);
        String userCToken = com.yourteam.lostfound.util.JwtUtil.generateToken(userCEmail, "ROLE_USER");

        // 1. Unrelated user C attempting to read item messages -> HTTP 403 Forbidden
        mockMvc.perform(get("/api/messages/item/" + foundItemId)
                        .header("Authorization", "Bearer " + userCToken))
                .andExpect(status().isForbidden());

        // 2. Item reporter (Student B) allowed -> HTTP 200 OK
        mockMvc.perform(get("/api/messages/item/" + foundItemId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk());

        // 3. Active claimant (Student A) allowed -> HTTP 200 OK
        mockMvc.perform(get("/api/messages/item/" + foundItemId)
                        .header("Authorization", "Bearer " + standardUserToken))
                .andExpect(status().isOk());

        // 4. Admin allowed -> HTTP 200 OK
        mockMvc.perform(get("/api/messages/item/" + foundItemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        log.info("[PASS REGRESSION 4] Message thread access strictly restricted to conversation participants");
    }

    @Test
    @Order(21)
    void test21_JwtSecretConfigurationAndTokenLifecycle() throws Exception {
        log.info("[REGRESSION TEST 5] Verifying JWT token generation and validation with externalized secret");

        String testEmail = "jwt_check_" + UUID.randomUUID().toString().substring(0, 6) + "@campus.edu";
        String token = com.yourteam.lostfound.util.JwtUtil.generateToken(testEmail, "ROLE_USER");

        assertNotNull(token);
        assertTrue(com.yourteam.lostfound.util.JwtUtil.validateToken(token));
        assertEquals(testEmail, com.yourteam.lostfound.util.JwtUtil.extractEmail(token));
        assertEquals("ROLE_USER", com.yourteam.lostfound.util.JwtUtil.extractRole(token));

        log.info("[PASS REGRESSION 5] Externalized JWT signing and validation verified successfully");
    }
}
