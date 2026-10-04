package com.ieltsaitutor.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo")
public class LocalDemoAccountProperties {
    private String studentEmail = "";
    private String studentFirstName = "Demo Student";
    private String studentPassword = "";
    private String adminEmail = "";
    private String adminFirstName = "Demo Admin";
    private String adminPassword = "";

    public LocalDemoAccountProperties() {}

    public LocalDemoAccountProperties(String studentEmail, String studentFirstName, String studentPassword,
            String adminEmail, String adminFirstName, String adminPassword) {
        this.studentEmail = studentEmail;
        this.studentFirstName = studentFirstName;
        this.studentPassword = studentPassword;
        this.adminEmail = adminEmail;
        this.adminFirstName = adminFirstName;
        this.adminPassword = adminPassword;
    }

    public String studentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String studentFirstName() { return studentFirstName; }
    public void setStudentFirstName(String studentFirstName) { this.studentFirstName = studentFirstName; }
    public String studentPassword() { return studentPassword; }
    public void setStudentPassword(String studentPassword) { this.studentPassword = studentPassword; }
    public String adminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String adminFirstName() { return adminFirstName; }
    public void setAdminFirstName(String adminFirstName) { this.adminFirstName = adminFirstName; }
    public String adminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
}
