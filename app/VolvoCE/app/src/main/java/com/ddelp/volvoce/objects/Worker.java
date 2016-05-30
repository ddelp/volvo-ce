package com.ddelp.volvoce.objects;

/**
 * Created by Denny on 5/5/16.
 *
 * Worker class to old ID, name, and GPS
 */
//TODO: Update for timestamp, velocity and direction
public class Worker {
    private String id;
    private String gps;
    private String name;
    private String worksite;
//    String velocity;
//    String direction;
//    String timestamp;

    public Worker() {
    }

    public Worker(String id) {
        this.id = id;
    }

    public Worker(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Worker(String id, String name, String gps) {
        this.id = id;
        this.name = name;
        this.gps = gps;
    }

    public Worker(String id, String name, String gps, String worksite) {
        this.id = id;
        this.name = name;
        this.gps = gps;
        this.worksite = worksite;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setGPS(String gps) {
        this.gps = gps;
    }

    public String getGPS() {
        return this.gps;
    }

    public void setWorksite(String worksite) {
        this.worksite = worksite;
    }

    public String getWorksite() {
        return this.worksite;
    }
}
