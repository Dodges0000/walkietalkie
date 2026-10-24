package com.example.walkietalkie.voice;

/** Биквадный фильтр (RBJ cookbook). */
final class Biquad {
    private final double b0, b1, b2, a1, a2;
    private double z1, z2;

    private Biquad(double b0, double b1, double b2, double a1, double a2) {
        this.b0 = b0; this.b1 = b1; this.b2 = b2; this.a1 = a1; this.a2 = a2;
    }

    static Biquad lowPass(double fs, double f, double q) {
        double w = 2 * Math.PI * f / fs, cos = Math.cos(w), alpha = Math.sin(w) / (2 * q);
        double a0 = 1 + alpha;
        return new Biquad((1 - cos) / 2 / a0, (1 - cos) / a0, (1 - cos) / 2 / a0,
                -2 * cos / a0, (1 - alpha) / a0);
    }

    static Biquad highPass(double fs, double f, double q) {
        double w = 2 * Math.PI * f / fs, cos = Math.cos(w), alpha = Math.sin(w) / (2 * q);
        double a0 = 1 + alpha;
        return new Biquad((1 + cos) / 2 / a0, -(1 + cos) / a0, (1 + cos) / 2 / a0,
                -2 * cos / a0, (1 - alpha) / a0);
    }

    double process(double x) {
        double y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return y;
    }
}
