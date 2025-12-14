package com.lycorisfun.fun.Entity;

import java.util.Date;

public class User {
    private Integer userId;
    private String userName;
    private String email;
    private String password;
    private String gender;
    private Date registerTime;
    private String signature;
    private String avaterURL;
    private String PersonalIndexLink;
    private int status;

    public User() {
    }

    public User(Integer userId, String userName, String email, String password, String gender, Date registerTime, String signature, String avaterURL, String PersonalIndexLink, int status){
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.registerTime = registerTime;
        this.signature = signature;
        this.avaterURL = avaterURL;
        this.PersonalIndexLink = PersonalIndexLink;
        this.status = status;
    }

    public User(Integer id, String userName, String email, String password) {
        this.userId = id;
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

    public User(String userName, String email, String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public Date getRegisterTime() {
        return registerTime;
    }
    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }
    public String getSignature() {
        return signature;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getAvaterURL() {
        return avaterURL;
    }
    public void setAvaterURL(String avaterURL) {
        this.avaterURL = avaterURL;
    }
    public String getPersonalIndexLink() {
        return PersonalIndexLink;
    }
    public void setPersonalIndexLink(String personalIndexLink) {
        PersonalIndexLink = personalIndexLink;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + userId +
                ", name='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", gender='" + gender + '\'' +
                ", registerDate=" + registerTime +
                ", signature='" + signature + '\'' +
                ", avaterURL='" + avaterURL + '\'' +
                ", PersonalIndexLink='" + PersonalIndexLink + '\'' +
                ", status=" + status +
                '}';
    }
}
