package com.example.project;

public class CurrentUser {
    public static String username;
    private static String school;

    public static void setUsername(String username) {CurrentUser.username = username;}
    public static void setSchool(String school) {CurrentUser.school = school;}

    public static void clearUserData() {
        username = null;
        school = null;
    }

    public static String getSchool() {return school;}
    public static String getUsername() {return username;}
}