package com.bolarde.oauth2login.Entity;

public class Contact {
    private String name;
    private String email;
    private String phone;
    private String jobTitleAndCompany;

    

    public Contact() {
    }

    public Contact(String name, String email, String phone, String jobTitleAndCompany) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.jobTitleAndCompany = jobTitleAndCompany;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getJobTitleAndCompany() { return jobTitleAndCompany; }
}
