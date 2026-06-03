package com.arriva.touristguideapp;

import java.util.List;

public class User {
    private String uid;
    private String name;
    private String fullName; // Added
    private String email;
    private String profileImage;
    private String profilePhoto; // Added (alias or different field in DB)
    private String role = "user"; // Default role: user, admin
    private String bio; // Added
    private String phoneNumber; // Added
    private String city; // Added
    private String state; // Added
    private String country; // Added
    private String preferredLanguage; // Added
    private List<String> travelInterests; // Added
    private long createdAt; // Added
    private long updatedAt; // Added
    private int emergencyContactsCount; // Added
    private String username;
    private String gender;
    private String dateOfBirth;
    private String favoriteTravelCategory;
    private boolean admin; // Added

    // Empty constructor for Firestore
    public User() {
    }

    public User(String uid, String name, String email, String profileImage) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        if (this.name == null) this.name = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
        if (this.profileImage == null) this.profileImage = profilePhoto;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public List<String> getTravelInterests() {
        return travelInterests;
    }

    public void setTravelInterests(List<String> travelInterests) {
        this.travelInterests = travelInterests;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getEmergencyContactsCount() {
        return emergencyContactsCount;
    }

    public void setEmergencyContactsCount(int emergencyContactsCount) {
        this.emergencyContactsCount = emergencyContactsCount;
    }

    public boolean isAdmin() {
        return admin || "admin".equals(role);
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getFavoriteTravelCategory() {
        return favoriteTravelCategory;
    }

    public void setFavoriteTravelCategory(String favoriteTravelCategory) {
        this.favoriteTravelCategory = favoriteTravelCategory;
    }
}
