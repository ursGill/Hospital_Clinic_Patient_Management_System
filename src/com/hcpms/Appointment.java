
package com.hcpms;

import java.sql.Timestamp;

public class Appointment {
    private int appointmentId;
    private int patientId;
    private String patientName;
    private int doctorId;
    private String doctorName;
    private Timestamp appointmentTime;
    private String status;

    public Appointment(int appointmentId, int patientId,
                       String patientName, int doctorId,
                       String doctorName, Timestamp appointmentTime,
                       String status) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public int getAppointmentId() { return appointmentId; }
    public int getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public int getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public Timestamp getAppointmentTime() { return appointmentTime; }
    public String getStatus() { return status; }
}
