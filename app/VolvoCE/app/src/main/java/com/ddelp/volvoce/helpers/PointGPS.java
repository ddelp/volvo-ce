package com.ddelp.volvoce.helpers;

/**
 * A point with latitude/longitude rather than X/Y
 * Note: on a map latitude is Y and longitude is X
 */
public class PointGPS {
    float latitude;
    float longitude;

    PointGPS() {
        this.latitude = 0f;
        this.longitude = 0f;
    }

    PointGPS(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}