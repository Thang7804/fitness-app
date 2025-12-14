package com.app.aifitness.Model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    public String email;
    public String goal;
    public Integer availableTime;
    public String experience;
    public Boolean hasEquipment;
    public String focusArea;
    public boolean isNew;
    public String Dob;
    public String gender;
    public  Float height;
    public  Float weight;
    public  Float goalWeight;
    public Integer level;
    public Integer currentDay;
    public Integer dayPerWeek;
    public Integer totalWorkoutDays; // Tổng số ngày workout trong schedule (tính dựa trên dayPerWeek)
    public String healthIssue;
    public Map<String, Map<String, Object>> schedule;
    public User(){}
    public User(String email) {
        this.email = email;
        this.goal = null;
        this.experience = null;
        this.availableTime = null;
        this.hasEquipment = false;
        this.focusArea = null;
        this.isNew= true;
        this.Dob=null;
        this.gender=null;
        this.height=null;
        this.weight=null;
        this.goalWeight =null;
        this.level=null;
        this.dayPerWeek=null;
        this.totalWorkoutDays=null;
        this.healthIssue=null;
        this.schedule = new HashMap<>();
        this.currentDay=null;
    }

    public User(String email, String goal, Integer availableTime, String experience, Boolean hasEquipment, String focusArea, boolean isNew, String dob, String gender, Float height, Float weight, Float goalWeight, Integer level, Integer currentDay, Integer dayPerWeek, Integer totalWorkoutDays, String healthIssue, Map<String, Map<String, Object>> schedule) {
        this.email = email;
        this.goal = goal;
        this.availableTime = availableTime;
        this.experience = experience;
        this.hasEquipment = hasEquipment;
        this.focusArea = focusArea;
        this.isNew = isNew;
        Dob = dob;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.goalWeight = goalWeight;
        this.level = level;
        this.currentDay = currentDay;
        this.dayPerWeek = dayPerWeek;
        this.totalWorkoutDays = totalWorkoutDays;
        this.healthIssue = healthIssue;
        this.schedule = schedule;
    }
}
