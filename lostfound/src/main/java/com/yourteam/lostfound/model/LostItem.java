package com.yourteam.lostfound.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "lost_items")
@DiscriminatorValue("LOST")
public class LostItem extends Item {

    private String reward;

    public LostItem() {
        super();
    }

    public LostItem(String title, String description, String category, LocalDate date,
                    String location, String status, String reward) {
        super(title, description, category, date, location, status);
        this.reward = reward;
    }

    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }
}