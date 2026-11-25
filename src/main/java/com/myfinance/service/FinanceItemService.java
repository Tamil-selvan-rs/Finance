package com.myfinance.service;

import com.myfinance.dto.FinanceItemRequest;
import com.myfinance.dto.FinanceItemResponse;
import com.myfinance.entity.FinanceItem;
import com.myfinance.entity.User;
import com.myfinance.exception.ResourceNotFoundException;
import com.myfinance.repository.FinanceItemRepository;
import com.myfinance.repository.UserRepository;
import com.myfinance.validator.FileUploadValidator;
import com.myfinance.validator.FinanceItemValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class FinanceItemService {

    private final FinanceItemRepository financeItemRepository;
    private final UserRepository userRepository;
    private final FinanceItemValidator financeItemValidator;
    private final FileUploadValidator fileUploadValidator;
    private final FileStorageService fileStorageService;
    private final EmailNotificationService emailNotificationService;

    public FinanceItemService(FinanceItemRepository financeItemRepository,
                              UserRepository userRepository,
                              FinanceItemValidator financeItemValidator,
                              FileUploadValidator fileUploadValidator,
                              FileStorageService fileStorageService,
                              EmailNotificationService emailNotificationService) {
        this.financeItemRepository = financeItemRepository;
        this.userRepository = userRepository;
        this.financeItemValidator = financeItemValidator;
        this.fileUploadValidator = fileUploadValidator;
        this.fileStorageService = fileStorageService;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public FinanceItemResponse createItem(String userEmail, FinanceItemRequest req, MultipartFile optionalImage) {
        financeItemValidator.validateCreateOrUpdate(req, true);
        fileUploadValidator.validateOptionalImage(optionalImage);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        FinanceItem item = new FinanceItem();
        item.setUser(user);
        applyFields(item, req, optionalImage);

        FinanceItem saved = financeItemRepository.save(item);

        if (saved.getDueDate() != null && !saved.getDueDate().isAfter(LocalDate.now()) && "PENDING".equals(saved.getStatus())) {
            emailNotificationService.sendDueDateReminder(saved);
            saved.setReminderSent(true);
            saved.setLastRemindedAt(Instant.now());
            saved = financeItemRepository.save(saved);
        }

        return FinanceItemResponse.fromEntity(saved);
    }

    @Transactional
    public FinanceItemResponse updateItem(String userEmail, Long itemId, FinanceItemRequest req, MultipartFile optionalImage) {
        financeItemValidator.validateCreateOrUpdate(req, false);
        fileUploadValidator.validateOptionalImage(optionalImage);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
        FinanceItem item = financeItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Finance item not found or access denied."));

        LocalDate previousDueDate = item.getDueDate();
        applyFields(item, req, optionalImage);

        // Reset reminder flag if due date was rescheduled into the future
        if (item.getDueDate() != null && !item.getDueDate().equals(previousDueDate) && item.getDueDate().isAfter(LocalDate.now())) {
            item.setReminderSent(false);
        }

        FinanceItem updated = financeItemRepository.save(item);
        return FinanceItemResponse.fromEntity(updated);
    }

    private void applyFields(FinanceItem item, FinanceItemRequest req, MultipartFile optionalImage) {
        item.setTitle(req.getTitle().trim());
        item.setCategory(req.getCategory().trim());
        item.setItemType(req.getItemType().trim().toUpperCase());
        item.setAmount(req.getAmount());
        item.setDueDate(req.getDueDate());
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            item.setStatus(req.getStatus().trim().toUpperCase());
        } else if (item.getStatus() == null) {
            item.setStatus("PENDING");
        }
        item.setNotes(req.getNotes() != null && !req.getNotes().isBlank() ? req.getNotes().trim() : null);

        if (optionalImage != null && !optionalImage.isEmpty()) {
            String storedPath = fileStorageService.storeOptionalFile(optionalImage);
            item.setAttachmentPath(storedPath);
            item.setAttachmentOriginalName(optionalImage.getOriginalFilename());
        }
    }

    @Transactional(readOnly = true)
    public List<FinanceItemResponse> getUserItems(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return financeItemRepository.findByUserIdOrderByDueDateAsc(user.getId())
                .stream()
                .map(FinanceItemResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FinanceItemResponse markAsPaid(String userEmail, Long itemId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        FinanceItem item = financeItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Finance item not found or access denied."));
        item.setStatus("PAID");
        return FinanceItemResponse.fromEntity(financeItemRepository.save(item));
    }

    @Transactional
    public void triggerManualReminder(String userEmail, Long itemId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        FinanceItem item = financeItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Finance item not found or access denied."));
        emailNotificationService.sendDueDateReminder(item);
        item.setReminderSent(true);
        item.setLastRemindedAt(Instant.now());
        financeItemRepository.save(item);
    }
}
