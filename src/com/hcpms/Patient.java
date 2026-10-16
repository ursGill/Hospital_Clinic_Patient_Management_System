package com.hcpms;

import java.sql.Timestamp;

public class Patient {
    private int patientId;
    private String fullName;
    private int age;
    private String gender;
    private String phone;
    private String address;
    private Timestamp createdAt;

    public Patient(int patientId, String fullName, int age, String gender,
                   String phone, String address, Timestamp createdAt) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt;
    }

    public Patient(String fullName, int age, String gender, String phone, String address) {
        this(0, fullName, age, gender, phone, address, null);
    }

    public int getPatientId() { return patientId; }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setPatientId(int patientId) { this.patientId = patientId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
}
