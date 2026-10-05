package com.yourteam.lostfound.dto;

import java.time.LocalDate;

public class ItemRequestDTO {

    private String title;
    private String description;
    private String category;
    private LocalDate date;
    private String location;
    private String status;
    private String imageUrl;
    private String verificationQuestion;
    private Double latitude;
    private Double longitude;
    private String reward;
    private String custodyDesk;
    private String storageBin;

    public ItemRequestDTO() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVerificationQuestion() { return verificationQuestion; }
    public void setVerificationQuestion(String verificationQuestion) {
        this.verificationQuestion = verificationQuestion;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }

    public String getCustodyDesk() { return custodyDesk; }
    public void setCustodyDesk(String custodyDesk) { this.custodyDesk = custodyDesk; }

    public String getStorageBin() { return storageBin; }
    public void setStorageBin(String storageBin) { this.storageBin = storageBin; }
}