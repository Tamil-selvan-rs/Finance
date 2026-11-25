package com.myfinance.dto;

import com.myfinance.entity.FinanceItem;
import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceItemResponse {
    private Long id;
    private String title;
    private String category;
    private String itemType;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String status;
    private String notes;
    private String attachmentUrl;
    private String attachmentOriginalName;
    private boolean reminderSent;

    public static FinanceItemResponse fromEntity(FinanceItem item) {
        FinanceItemResponse dto = new FinanceItemResponse();
        dto.id = item.getId();
        dto.title = item.getTitle();
        dto.category = item.getCategory();
        dto.itemType = item.getItemType();
        dto.amount = item.getAmount();
        dto.dueDate = item.getDueDate();
        dto.status = item.getStatus();
        dto.notes = item.getNotes();
        dto.attachmentUrl = item.getAttachmentPath() != null ? "/uploads/" + item.getAttachmentPath() : null;
        dto.attachmentOriginalName = item.getAttachmentOriginalName();
        dto.reminderSent = item.isReminderSent();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getItemType() { return itemType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public boolean isReminderSent() { return reminderSent; }
}
