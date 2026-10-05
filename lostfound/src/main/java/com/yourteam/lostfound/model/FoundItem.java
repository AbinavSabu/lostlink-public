package com.yourteam.lostfound.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "found_items")
@DiscriminatorValue("FOUND")
public class FoundItem extends Item {

    private String storageLocation;
    private String handoffInstructions;

    public FoundItem() {
        super();
    }

    public FoundItem(String title, String description, String category, LocalDate date,
                     String location, String status, String storageLocation, String handoffInstructions) {
        super(title, description, category, date, location, status);
        this.storageLocation = storageLocation;
        this.handoffInstructions = handoffInstructions;
    }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public String getHandoffInstructions() { return handoffInstructions; }
    public void setHandoffInstructions(String handoffInstructions) { this.handoffInstructions = handoffInstructions; }
}