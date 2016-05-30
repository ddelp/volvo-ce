package com.ddelp.volvoce.objects;

/**
 * Created by Denny on 5/9/16.
 */
//TODO: Update for timestamp, velocity and direction
public class Machine {
    public static final int MACHINE_TYPE_NONE = 0;

    private String id;
    private int type;
    private String gps;
    private String worksite;
//    String velocity;
//    String direction;
//    String timestamp;

    public Machine() {
    }

    public Machine(String id) {
        this(id, MACHINE_TYPE_NONE, null, null);
    }

    public Machine(String id, int type) {
        this(id, type, null, null);
    }

    public Machine(String id, int type, String gps) {
        this(id, type, gps, null);
    }

    public Machine(String id, int type, String gps, String worksite) {
        this.id = id;
        this.type = type;
        this.gps = gps;
        this.worksite = worksite;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return this.id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
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