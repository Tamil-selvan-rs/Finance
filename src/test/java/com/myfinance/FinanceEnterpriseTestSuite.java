package com.myfinance;

import com.myfinance.dto.CredentialVaultRequest;
import com.myfinance.dto.FinanceItemRequest;
import com.myfinance.entity.User;
import com.myfinance.exception.BusinessValidationException;
import com.myfinance.exception.FileStorageException;
import com.myfinance.repository.UserRepository;
import com.myfinance.security.CustomUserDetailsService;
import com.myfinance.service.CredentialVaultService;
import com.myfinance.service.EmailNotificationService;
import com.myfinance.service.FinanceItemService;
import com.myfinance.validator.FileUploadValidator;
import com.myfinance.validator.FinanceItemValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinanceEnterpriseTestSuite {

    @Autowired private MockMvc mockMvc;
    @Autowired private FinanceItemService financeItemService;
    @Autowired private CredentialVaultService credentialVaultService;
    @Autowired private FinanceItemValidator financeItemValidator;
    @Autowired private FileUploadValidator fileUploadValidator;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    @MockitoBean private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setupTestUser() {
        if (userRepository.findByEmail("engineer@myfinance.com").isEmpty()) {
            User u = new User();
            u.setFullName("Enterprise Engineer");
            u.setEmail("engineer@myfinance.com");
            u.setPassword("$2a$12$dummyBcryptHashForTestingOnly12345678901234567890");
            userRepository.save(u);
        }
    }

    @Test
    @DisplayName("TC-S01: Create Entries Across All 8 Finance Tabs (LEND, SPEND, SALARY, SAVINGS, FINE, BORROW, EMI, LOAN)")
    void testAllSevenFinanceTabs() {
        List<String> tabs = List.of("LEND", "SPEND", "SALARY", "SAVINGS", "FINE", "BORROW", "EMI", "LOAN");
        for (String tab : tabs) {
            FinanceItemRequest req = new FinanceItemRequest();
            req.setTitle("Test Entry for " + tab);
            req.setCategory("Counterparty " + tab);
            req.setItemType(tab);
            req.setAmount(new BigDecimal("2500.00"));
            req.setDueDate(LocalDate.now().plusDays(7));

            var saved = financeItemService.createItem("engineer@myfinance.com", req, null);
            assertEquals(tab, saved.getItemType());
            assertNull(saved.getAttachmentUrl());
        }
        assertEquals(8, financeItemService.getUserItems("engineer@myfinance.com").size());
    }

    @Test
    @DisplayName("TC-S02: Create Finance Item WITH Valid PNG Image & Due Today Triggers Email")
    void testCreateWithValidImageAndImmediateDueEmail() {
        byte[] validPngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile pngFile = new MockMultipartFile("attachment", "receipt.png", "image/png", validPngHeader);

        FinanceItemRequest req = new FinanceItemRequest();
        req.setTitle("Home Loan Monthly EMI");
        req.setCategory("HDFC Bank");
        req.setItemType("EMI");
        req.setAmount(new BigDecimal("28500.00"));
        req.setDueDate(LocalDate.now());

        var res = financeItemService.createItem("engineer@myfinance.com", req, pngFile);
        assertNotNull(res.getAttachmentUrl());
        assertTrue(res.isReminderSent());
        verify(emailNotificationService, times(1)).sendDueDateReminder(any());
    }

    @Test
    @DisplayName("TC-S03: Credential Vault Encrypts & Decrypts Application Username & Password")
    void testCredentialVaultRoundTrip() {
        CredentialVaultRequest req = new CredentialVaultRequest();
        req.setAppTitle("HDFC NetBanking");
        req.setAppUrl("https://netbanking.hdfcbank.com");
        req.setLoginUsername("tamil_cust_9981");
        req.setPassword("S3cur3Vault#2026");
        req.setCategory("BANKING");

        var saved = credentialVaultService.saveCredential("engineer@myfinance.com", req);
        assertNotNull(saved.getId());
        assertEquals("HDFC NetBanking", saved.getAppTitle());
        assertEquals("S3cur3Vault#2026", saved.getPlainPassword());

        var list = credentialVaultService.getUserCredentials("engineer@myfinance.com");
        assertEquals(1, list.size());
        assertEquals("S3cur3Vault#2026", list.get(0).getPlainPassword());
    }


    @Test
    @DisplayName("TC-S04: Create Finance Item WITHOUT Optional Due Date & Without Image")
    void testCreateWithoutOptionalDueDate() {
        FinanceItemRequest req = new FinanceItemRequest();
        req.setTitle("Cash Grocery Spend");
        req.setCategory("Supermarket");
        req.setItemType("SPEND");
        req.setAmount(new BigDecimal("1250.00"));
        req.setDueDate(null);

        var res = financeItemService.createItem("engineer@myfinance.com", req, null);
        assertNotNull(res.getId());
        assertNull(res.getDueDate());
        assertFalse(res.isReminderSent());
    }


    @Test
    @DisplayName("TC-S05: Edit Existing Money Record Updates Amount, Category & Status")
    void testEditFinanceItem() {
        FinanceItemRequest createReq = new FinanceItemRequest();
        createReq.setTitle("Bike Loan EMI");
        createReq.setCategory("ICICI Bank");
        createReq.setItemType("EMI");
        createReq.setAmount(new BigDecimal("4500.00"));
        createReq.setDueDate(LocalDate.now().plusDays(10));

        var created = financeItemService.createItem("engineer@myfinance.com", createReq, null);

        FinanceItemRequest editReq = new FinanceItemRequest();
        editReq.setTitle("Bike Loan EMI Revised");
        editReq.setCategory("ICICI Auto Finance");
        editReq.setItemType("EMI");
        editReq.setAmount(new BigDecimal("4850.00"));
        editReq.setStatus("PAID");

        var updated = financeItemService.updateItem("engineer@myfinance.com", created.getId(), editReq, null);
        assertEquals("Bike Loan EMI Revised", updated.getTitle());
        assertEquals(new BigDecimal("4850.00"), updated.getAmount());
        assertEquals("PAID", updated.getStatus());
    }

    @Test
    @DisplayName("TC-S06: Login Authentication Resolves User via Email, Username, or Mobile Number")
    void testLoginByEmailUsernameOrMobile() {
        User u = userRepository.findByEmail("engineer@myfinance.com").orElseThrow();
        u.setUsername("tamilselvan_dev");
        u.setPhone("+919876543210");
        userRepository.save(u);

        assertEquals("engineer@myfinance.com", customUserDetailsService.loadUserByUsername("engineer@myfinance.com").getUsername());
        assertEquals("engineer@myfinance.com", customUserDetailsService.loadUserByUsername("tamilselvan_dev").getUsername());
        assertEquals("engineer@myfinance.com", customUserDetailsService.loadUserByUsername("+919876543210").getUsername());
    }

    @Test
    @DisplayName("TC-E01: Validator Rejects Invalid Finance Tab & Negative Amount")
    void testValidatorRejectsInvalidPayload() {
        FinanceItemRequest badReq = new FinanceItemRequest();
        badReq.setTitle("AB");
        badReq.setCategory("");
        badReq.setItemType("UNKNOWN_TAB");
        badReq.setAmount(new BigDecimal("-50.00"));
        badReq.setDueDate(LocalDate.now());

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> financeItemValidator.validateCreateOrUpdate(badReq, true));
        assertTrue(ex.getFieldErrors().containsKey("title"));
        assertTrue(ex.getFieldErrors().containsKey("amount"));
        assertTrue(ex.getFieldErrors().containsKey("itemType"));
    }

    @Test
    @DisplayName("TC-E02: FileUploadValidator Rejects Spoofed Executable Disguised as PNG")
    void testRejectsSpoofedMimeFile() {
        byte[] exeBytes = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00};
        MockMultipartFile spoofed = new MockMultipartFile("attachment", "malware.png", "image/png", exeBytes);
        assertThrows(FileStorageException.class, () -> fileUploadValidator.validateOptionalImage(spoofed));
    }

    @Test
    @DisplayName("TC-E03: Vault API Returns Structured 400 JSON from GlobalExceptionHandler on Blank Title")
    @WithMockUser(username = "engineer@myfinance.com")
    void testVaultApiValidationError() throws Exception {
        String badVaultJson = "{\"appTitle\":\"\",\"loginUsername\":\"\",\"password\":\"\"}";
        mockMvc.perform(post("/api/vault")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(badVaultJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-4001"))
                .andExpect(jsonPath("$.fieldErrors.appTitle").exists())
                .andExpect(jsonPath("$.fieldErrors.loginUsername").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
