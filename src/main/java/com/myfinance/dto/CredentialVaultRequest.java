package com.myfinance.dto;

public class CredentialVaultRequest {
    private String appTitle;
    private String appUrl;
    private String loginUsername;
    private String password;
    private String category;
    private String notes;

    public String getAppTitle() { return appTitle; }
    public void setAppTitle(String appTitle) { this.appTitle = appTitle; }
    public String getAppUrl() { return appUrl; }
    public void setAppUrl(String appUrl) { this.appUrl = appUrl; }
    public String getLoginUsername() { return loginUsername; }
    public void setLoginUsername(String loginUsername) { this.loginUsername = loginUsername; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
