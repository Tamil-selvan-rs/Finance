package com.myfinance.dto;

import com.myfinance.entity.CredentialVault;

public class CredentialVaultResponse {
    private Long id;
    private String appTitle;
    private String appUrl;
    private String loginUsername;
    private String plainPassword;
    private String category;
    private String notes;

    public static CredentialVaultResponse fromEntity(CredentialVault v, String decryptedPassword) {
        CredentialVaultResponse res = new CredentialVaultResponse();
        res.id = v.getId();
        res.appTitle = v.getAppTitle();
        res.appUrl = v.getAppUrl();
        res.loginUsername = v.getLoginUsername();
        res.plainPassword = decryptedPassword;
        res.category = v.getCategory();
        res.notes = v.getNotes();
        return res;
    }

    public Long getId() { return id; }
    public String getAppTitle() { return appTitle; }
    public String getAppUrl() { return appUrl; }
    public String getLoginUsername() { return loginUsername; }
    public String getPlainPassword() { return plainPassword; }
    public String getCategory() { return category; }
    public String getNotes() { return notes; }
}
