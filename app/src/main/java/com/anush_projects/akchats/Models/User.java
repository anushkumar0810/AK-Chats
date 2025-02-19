package com.anush_projects.akchats.Models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String fcmToken;
    private String profileImageBase64;

    public User() {}

    public User(String userId, String name, String email, String fcmToken, String profileImageBase64) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.fcmToken = fcmToken;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getFcmToken() { return fcmToken; }
    public String getProfileImageBase64() { return profileImageBase64; }
}
