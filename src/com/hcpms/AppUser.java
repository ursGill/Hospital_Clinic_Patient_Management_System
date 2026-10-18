package com.hcpms;

/** Authenticated HCPMS account and its access role. */
public final class AppUser {
    private final int userId;
    private final String username;
    private final String role;
    private final Integer doctorId;

    public AppUser(int userId, String username, String role, Integer doctorId) {
        this.userId = userId; this.username = username; this.role = role; this.doctorId = doctorId;
    }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Integer getDoctorId() { return doctorId; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isDoctor() { return "DOCTOR".equals(role); }
    public boolean isReceptionist() { return "RECEPTIONIST".equals(role); }
}
