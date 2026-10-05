package com.yourteam.lostfound.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends User {

    private String department;
    private String staffBadgeNumber;

    public Admin() {
        super();
    }

    public Admin(String name, String email, String password, String department, String staffBadgeNumber) {
        super(name, email, password, "ROLE_ADMIN");
        this.department = department;
        this.staffBadgeNumber = staffBadgeNumber;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStaffBadgeNumber() { return staffBadgeNumber; }
    public void setStaffBadgeNumber(String staffBadgeNumber) { this.staffBadgeNumber = staffBadgeNumber; }
}