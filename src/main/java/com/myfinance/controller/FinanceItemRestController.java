package com.myfinance.controller;

import com.myfinance.dto.FinanceItemRequest;
import com.myfinance.dto.FinanceItemResponse;
import com.myfinance.service.FinanceItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
public class FinanceItemRestController {

    private final FinanceItemService financeItemService;

    public FinanceItemRestController(FinanceItemService financeItemService) {
        this.financeItemService = financeItemService;
    }

    @GetMapping
    public ResponseEntity<List<FinanceItemResponse>> listItems(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(financeItemService.getUserItems(user.getUsername()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FinanceItemResponse> createItemWithOptionalImage(
            @AuthenticationPrincipal UserDetails user,
            @ModelAttribute FinanceItemRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        FinanceItemResponse created = financeItemService.createItem(user.getUsername(), request, attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FinanceItemResponse> createItemJson(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody FinanceItemRequest request) {
        FinanceItemResponse created = financeItemService.createItem(user.getUsername(), request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FinanceItemResponse> updateItem(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @ModelAttribute FinanceItemRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        return ResponseEntity.ok(financeItemService.updateItem(user.getUsername(), id, request, attachment));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<FinanceItemResponse> markPaid(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(financeItemService.markAsPaid(user.getUsername(), id));
    }

    @PostMapping("/{id}/remind")
    public ResponseEntity<Map<String, String>> sendReminderNow(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        financeItemService.triggerManualReminder(user.getUsername(), id);
        return ResponseEntity.ok(Map.of("message", "Due-date email notification queued successfully."));
    }
}
