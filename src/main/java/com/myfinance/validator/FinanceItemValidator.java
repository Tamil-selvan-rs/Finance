package com.myfinance.validator;

import com.myfinance.dto.FinanceItemRequest;
import com.myfinance.exception.BusinessValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class FinanceItemValidator {

    public static final Set<String> ALLOWED_TYPES = Set.of(
            "LEND", "SPEND", "SALARY", "SAVINGS", "FINE", "BORROW", "EMI", "LOAN",
            "BILL", "SUBSCRIPTION", "TAX", "INVESTMENT"
    );
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PAID", "OVERDUE", "RECEIVED");
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,&()/#]+$");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000.00");

    public void validateCreateOrUpdate(FinanceItemRequest req, boolean isCreate) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (req == null) {
            throw new BusinessValidationException("Request payload cannot be null.", errors);
        }

        if (req.getTitle() == null || req.getTitle().trim().length() < 3 || req.getTitle().trim().length() > 120) {
            errors.put("title", "Title must be between 3 and 120 characters.");
        } else if (!SAFE_TEXT_PATTERN.matcher(req.getTitle().trim()).matches()) {
            errors.put("title", "Title contains invalid special characters.");
        }

        if (req.getCategory() == null || req.getCategory().trim().isEmpty() || req.getCategory().trim().length() > 50) {
            errors.put("category", "Counterparty / Category is required (max 50 characters).");
        } else if (!SAFE_TEXT_PATTERN.matcher(req.getCategory().trim()).matches()) {
            errors.put("category", "Category contains invalid characters.");
        }

        if (req.getItemType() == null || !ALLOWED_TYPES.contains(req.getItemType().trim().toUpperCase())) {
            errors.put("itemType", "Tab type must be one of: LEND, SPEND, SALARY, SAVINGS, FINE, BORROW, EMI, LOAN.");
        }

        if (req.getAmount() == null) {
            errors.put("amount", "Amount is required.");
        } else if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("amount", "Amount must be strictly greater than 0.00.");
        } else if (req.getAmount().compareTo(MAX_AMOUNT) > 0) {
            errors.put("amount", "Amount cannot exceed 100,000,000.00.");
        } else if (req.getAmount().scale() > 2) {
            errors.put("amount", "Amount cannot have more than 2 decimal places.");
        }

        // Due Date is OPTIONAL: validate range only if provided
        if (req.getDueDate() != null) {
            if (isCreate && req.getDueDate().isBefore(LocalDate.now().minusYears(5))) {
                errors.put("dueDate", "Date cannot be more than 5 years in the past.");
            } else if (req.getDueDate().isAfter(LocalDate.now().plusYears(15))) {
                errors.put("dueDate", "Date cannot be more than 15 years in the future.");
            }
        }

        if (req.getStatus() != null && !req.getStatus().isBlank() &&
                !ALLOWED_STATUSES.contains(req.getStatus().trim().toUpperCase())) {
            errors.put("status", "Status must be PENDING, PAID, OVERDUE, or RECEIVED.");
        }

        if (req.getNotes() != null && req.getNotes().length() > 500) {
            errors.put("notes", "Notes cannot exceed 500 characters.");
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Finance item validation failed.", errors);
        }
    }
}
