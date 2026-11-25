package com.myfinance.controller;

import com.myfinance.dto.CredentialVaultRequest;
import com.myfinance.dto.CredentialVaultResponse;
import com.myfinance.dto.FinanceItemRequest;
import com.myfinance.dto.FinanceItemResponse;
import com.myfinance.entity.User;
import com.myfinance.repository.UserRepository;
import com.myfinance.service.CredentialVaultService;
import com.myfinance.service.FinanceItemService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final FinanceItemService financeItemService;
    private final CredentialVaultService credentialVaultService;
    private final UserRepository userRepository;

    public DashboardController(FinanceItemService financeItemService,
                               CredentialVaultService credentialVaultService,
                               UserRepository userRepository) {
        this.financeItemService = financeItemService;
        this.credentialVaultService = credentialVaultService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails principal,
                            @RequestParam(value = "tab", defaultValue = "ALL") String activeTab,
                            Model model) {
        String email = principal.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);
        String fullName = user != null ? user.getFullName() : email;
        String initials = fullName.length() >= 2 ? fullName.substring(0, 2).toUpperCase() : "MF";

        List<FinanceItemResponse> allItems = financeItemService.getUserItems(email);
        List<CredentialVaultResponse> vaultItems = credentialVaultService.getUserCredentials(email);

        List<String> tabKeys = List.of("LEND", "SPEND", "SALARY", "SAVINGS", "FINE", "BORROW", "EMI", "LOAN");
        Map<String, BigDecimal> tabTotals = new LinkedHashMap<>();
        for (String key : tabKeys) {
            BigDecimal sum = allItems.stream()
                    .filter(i -> key.equalsIgnoreCase(i.getItemType()))
                    .map(FinanceItemResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            tabTotals.put(key, sum);
        }

        BigDecimal totalPending = allItems.stream()
                .filter(i -> !"PAID".equals(i.getStatus()))
                .map(FinanceItemResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = allItems.stream()
                .filter(i -> "PAID".equals(i.getStatus()))
                .map(FinanceItemResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("user", user != null ? user : new User());
        model.addAttribute("userEmail", email);
        model.addAttribute("fullName", fullName);
        model.addAttribute("initials", initials);
        model.addAttribute("activeTab", activeTab.toUpperCase());
        model.addAttribute("items", allItems);
        model.addAttribute("tabTotals", tabTotals);
        model.addAttribute("vaultItems", vaultItems);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("newItem", new FinanceItemRequest());
        model.addAttribute("newVault", new CredentialVaultRequest());
        return "dashboard";
    }

    @PostMapping("/items")
    public String createItemFromForm(@AuthenticationPrincipal UserDetails user,
                                     @ModelAttribute FinanceItemRequest request,
                                     @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                     RedirectAttributes redirectAttributes) {
        financeItemService.createItem(user.getUsername(), request, attachment);
        String tab = request.getItemType() != null ? request.getItemType().toUpperCase() : "ALL";
        redirectAttributes.addFlashAttribute("successMessage",
                tab + " entry saved! Automated due-date email alert is active.");
        return "redirect:/dashboard?tab=" + tab;
    }


    @PostMapping("/items/{id}/edit")
    public String updateItemFromForm(@AuthenticationPrincipal UserDetails user,
                                     @PathVariable Long id,
                                     @ModelAttribute FinanceItemRequest request,
                                     @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                     RedirectAttributes redirectAttributes) {
        financeItemService.updateItem(user.getUsername(), id, request, attachment);
        String tab = request.getItemType() != null ? request.getItemType().toUpperCase() : "ALL";
        redirectAttributes.addFlashAttribute("successMessage",
                tab + " entry updated successfully!");
        return "redirect:/dashboard?tab=" + tab;
    }

    @PostMapping("/items/{id}/pay")
    public String markPaidFromForm(@AuthenticationPrincipal UserDetails user,
                                   @PathVariable Long id,
                                   @RequestParam(value = "tab", defaultValue = "ALL") String tab,
                                   RedirectAttributes redirectAttributes) {
        financeItemService.markAsPaid(user.getUsername(), id);
        redirectAttributes.addFlashAttribute("successMessage", "Record marked as PAID.");
        return "redirect:/dashboard?tab=" + tab;
    }

    @PostMapping("/items/{id}/remind")
    public String remindNowFromForm(@AuthenticationPrincipal UserDetails user,
                                    @PathVariable Long id,
                                    @RequestParam(value = "tab", defaultValue = "ALL") String tab,
                                    RedirectAttributes redirectAttributes) {
        financeItemService.triggerManualReminder(user.getUsername(), id);
        redirectAttributes.addFlashAttribute("successMessage", "Due-date email reminder sent to your inbox!");
        return "redirect:/dashboard?tab=" + tab;
    }

    @PostMapping("/vault")
    public String saveVaultCredential(@AuthenticationPrincipal UserDetails user,
                                      @ModelAttribute CredentialVaultRequest request,
                                      RedirectAttributes redirectAttributes) {
        credentialVaultService.saveCredential(user.getUsername(), request);
        redirectAttributes.addFlashAttribute("successMessage",
                "Application credentials for '" + request.getAppTitle() + "' encrypted (AES-256) & saved!");
        return "redirect:/dashboard?tab=VAULT";
    }

    @PostMapping("/vault/{id}/delete")
    public String deleteVaultCredential(@AuthenticationPrincipal UserDetails user,
                                        @PathVariable Long id,
                                        RedirectAttributes redirectAttributes) {
        credentialVaultService.deleteCredential(user.getUsername(), id);
        redirectAttributes.addFlashAttribute("successMessage", "Credential entry removed from vault.");
        return "redirect:/dashboard?tab=VAULT";
    }
}
