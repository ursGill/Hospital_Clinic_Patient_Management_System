
package com.hcpms;

import java.sql.Timestamp;

public class Doctor {
    private int doctorId;
    private String fullName;
    private String specialization;
    private String phone;
    private double consultationFee;
    private Timestamp createdAt;

    public Doctor(int doctorId, String fullName,
                  String specialization, String phone,
                  double consultationFee, Timestamp createdAt) {
        this.doctorId = doctorId;
        this.fullName = fullName;
        this.specialization = specialization;
        this.phone = phone;
        this.consultationFee = consultationFee;
        this.createdAt = createdAt;
    }

    public Doctor(String fullName, String specialization,
                  String phone, double consultationFee) {
        this(0, fullName, specialization, phone,
             consultationFee, null);
    }

    public int getDoctorId() { return doctorId; }
    public String getFullName() { return fullName; }
    public String getSpecialization() { return specialization; }
    public String getPhone() { return phone; }
    public double getConsultationFee() { return consultationFee; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setConsultationFee(double fee) {
        this.consultationFee = fee;
    }
}
