package com.app.aifitness.exercise;

/**
 * Độ khó / độ nghiêm khắc khi chấm điểm form.
 *
 * EASY   = minScore 60
 * NORMAL = minScore 70
 * HARD   = minScore 80
 */
public enum Strictness {
    EASY(60),
    NORMAL(70),
    HARD(80);

    private final int minScore;

    Strictness(int minScore) {
        this.minScore = minScore;
    }

    /**
     * Điểm tối thiểu để được coi là "form đạt yêu cầu" ở mức độ này.
     */
    public int getMinScore() {
        return minScore;
    }
}
