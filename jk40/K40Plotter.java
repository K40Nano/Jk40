/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jk40;

/**
 *
 * @author Tat
 */
public class K40Plotter {

    static final String LASER_ON = "D";
    static final String LASER_OFF = "U";
    static final String RIGHT = "B";
    static final String LEFT = "T";
    static final String TOP = "L";
    static final String BOTTOM = "R";

    static final String[] DISTANCE_LOOKUP = new String[]{"",
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", /*z*/
        "|a", "|b", "|c", "|d", "|e", "|f", "|g", "|h", "|i", "|j", "|k", "|l", "|m",
        "|n", "|o", "|p", "|q", "|r", "|s", "|t", "|u", "|v", "|w", "|x", "|y", "|z"};

    static final String getDistance(int v) {
        StringBuilder b = new StringBuilder();
        if (v >= 255) {
            int z_count = v / 255;
            v %= 255;

            for (int i = 0; i < z_count; i++) {
                b.append("z");
            }
        }
        if (v >= 52) {
            b.append(String.format("%03d", v));
        } else {
            b.append(DISTANCE_LOOKUP[v]);
        }
        return b.toString();
    }

    static final String getSpeed(double mm_per_second) {
        mm_per_second = validateSpeed(mm_per_second);
        double b = 5120.0;
        double m = 11148.0;
        double frequency_kHz = mm_per_second / 25.4;
        double period_in_ms = 1.0 / frequency_kHz;
        double period_value = m * period_in_ms + b;
        int speed_value = 65536 - (int) period_value;
        int step_value = (int) mm_per_second;
        double d_value = 0.2612 * m * period_in_ms / (double) step_value;
        int diag_add = (int) d_value;
        return String.format(
                "CV%03d%03d%1d%03d%03d%03d",
                (speed_value >> 8) & 0xFF, (speed_value & 0xFF),
                1,
                step_value,
                (diag_add >> 8) & 0xFF, (diag_add & 0xFF));
    }

    static final double validateSpeed(double s) {
        return s;
    }
}
