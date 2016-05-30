package com.ddelp.volvoce.helpers;

/**
 * Stores the GPS coordinates of the worksite edges
 */
public class WorksiteGPS {
    float top;
    float bottom;
    float left;
    float right;

    WorksiteGPS(float top, float bottom, float left, float right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    float getWidth() {
        return Math.abs(right - left);
    }

    float getHeight() {
        return Math.abs(bottom - top);
    }
}
