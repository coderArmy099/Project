package com.example.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Course {
    private String courseName;
    private String teacherName;
    private int creditHour;
    private List<String> topics;
    private List<String> completedTopics;
    private String referenceBooks;
    private double progress; // Stored as a double (0.0 to 1.0)

    public Course(String courseName, String teacherName, int creditHour, List<String> topics, String referenceBooks) {
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.creditHour = creditHour;
        this.topics = new ArrayList<>(topics); // Ensure a mutable list
        this.completedTopics = new ArrayList<>(); // Initialize empty
        this.referenceBooks = referenceBooks;
        this.progress = calculateProgress(); // Calculate initial progress
    }

    // Constructor for loading from CSV
    public Course(String courseName, String teacherName, int creditHour, List<String> topics, List<String> completedTopics, String referenceBooks) {
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.creditHour = creditHour;
        this.topics = new ArrayList<>(topics);
        this.completedTopics = new ArrayList<>(completedTopics);
        this.referenceBooks = referenceBooks;
        this.progress = calculateProgress();
    }

    // Getters
    public String getCourseName() {
        return courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public int getCreditHour() {
        return creditHour;
    }

    public List<String> getTopics() {
        return new ArrayList<>(topics); // Return a copy to prevent external modification
    }

    public List<String> getCompletedTopics() {
        return new ArrayList<>(completedTopics); // Return a copy
    }

    public String getReferenceBooks() {
        return referenceBooks;
    }

    public double getProgress() {
        return progress;
    }

    // New methods to mark topics as completed or incomplete
    public void markTopicCompleted(String topic) {
        if (topics.contains(topic) && !completedTopics.contains(topic)) {
            completedTopics.add(topic);
            calculateProgress();
        }
    }

    public void markTopicIncomplete(String topic) {
        if (topics.contains(topic) && completedTopics.contains(topic)) {
            completedTopics.remove(topic);
            calculateProgress();
        }
    }

    // Method to calculate progress
    public double calculateProgress() {
        if (topics.isEmpty()) {
            this.progress = 0.0;
        } else {
            this.progress = (double) completedTopics.size() / topics.size();
        }
        return this.progress;
    }

    // Convert Course object to CSV string for saving
    public String toCsvString() {
        // Escape commas and semicolons in fields that might contain them
        String escapedCourseName = escapeCsv(courseName);
        String escapedTeacherName = escapeCsv(teacherName);
        String escapedTopics = escapeCsv(String.join(";", topics)); // Semicolon-separated
        String escapedCompletedTopics = escapeCsv(String.join(";", completedTopics)); // Semicolon-separated
        String escapedReferenceBooks = escapeCsv(referenceBooks);

        return String.format("%s,%s,%d,%s,%s,%s",
                escapedCourseName,
                escapedTeacherName,
                creditHour,
                escapedTopics,
                escapedCompletedTopics,
                escapedReferenceBooks);
    }

    // Create Course object from CSV string for loading
    public static Course fromCsvString(String csvString) {
        String[] parts = csvString.split(",", -1); // -1 to keep trailing empty strings
        if (parts.length != 6) {
            System.err.println("Malformed CSV string for Course: " + csvString);
            return null;
        }
        try {
            String courseName = unescapeCsv(parts[0]);
            String teacherName = unescapeCsv(parts[1]);
            int creditHour = Integer.parseInt(parts[2]);
            List<String> topics = parseListString(unescapeCsv(parts[3]));
            List<String> completedTopics = parseListString(unescapeCsv(parts[4]));
            String referenceBooks = unescapeCsv(parts[5]);

            return new Course(courseName, teacherName, creditHour, topics, completedTopics, referenceBooks);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number from CSV: " + csvString);
            e.printStackTrace();
            return null;
        }
    }

    // Helper to parse a semicolon-separated string into a list
    private static List<String> parseListString(String listString) {
        if (listString == null || listString.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(listString.split(";")));
    }

    // Simple CSV escaping/unescaping (for commas within fields)
    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String unescapeCsv(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }
}