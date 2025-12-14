package com.app.aifitness.Model;

import java.util.Map;
import java.util.List;

public class Exercise {
    public String id;
    public String name;
    public String description;
    public String focusArea;
    public int intensity;
    public int difficulty;

    public double caloriesPerMin;
    public boolean requiresEquipment;
    public Map<String, Double> muscle_group;
    public List<String> issues;
    public String videoUrl;
    public String thumbnailUrl;
    public boolean isDynamic;
    public String cameraSide;
    public Exercise() {}

    public Exercise(String id, String name, String description, String focusArea, int intensity, int difficulty, double caloriesPerMin, boolean requiresEquipment, Map<String, Double> muscle_group, List<String> issues, String videoUrl, String thumbnailUrl, boolean isDynamic, String cameraSide) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.focusArea = focusArea;
        this.intensity = intensity;
        this.difficulty = difficulty;
        this.caloriesPerMin = caloriesPerMin;
        this.requiresEquipment = requiresEquipment;
        this.muscle_group = muscle_group;
        this.issues = issues;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.isDynamic = isDynamic;
        this.cameraSide = cameraSide;
    }
}