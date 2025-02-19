package com.anush_projects.akchats.Models;

public class ContactsModel {
    private String userId;
    private String name;
    private String profileImageBase64;

    public ContactsModel() {}

    public ContactsModel(String userId, String name, String profileImageBase64) {
        this.userId = userId;
        this.name = name;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }
}
