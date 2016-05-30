package com.ddelp.volvoce.objects;

import com.ddelp.volvoce.objects.Machine;
import com.ddelp.volvoce.objects.Worker;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Denny on 5/7/16.
 *
 * Holds the GPS information of a worksite along with a list
 * of workers on the site
 */
public class Worksite {
    public String name;
    public String topLeft;
    public String bottomRight;
    public HashMap<String,Boolean> workers;
    public HashMap<String,Boolean> machines;

    public Worksite() {
        this(null, null, null);
    }

    public Worksite(String name) {
        this(name, null, null);
    }

    public Worksite(String name, String topLeft, String bottomRight) {
        this.name = name;
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        this.workers = new HashMap<>();
        this.machines = new HashMap<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTopLeft(String topLeft) {
        this.topLeft = topLeft;
    }

    public String getTopLeft() {
        return topLeft;
    }

    public void setBottomRight(String bottomRight) {
        this.bottomRight = bottomRight;
    }

    public String getBottomRight() {
        return bottomRight;
    }

    public void addWorker(Worker worker) {
        addWorker(worker.getID());
    }

    public void addWorker(String id) {
        workers.put(id, true);
    }

    public void addMachine(Machine machine) {
        addMachine(machine.getID());
    }

    public void addMachine(String id) {
        machines.put(id, true);
    }

    public HashMap<String, Boolean> getWorkers() {
        return this.workers;
    }

    public void setWorkers(HashMap<String, Boolean> workers) {
        this.workers = workers;
    }

    public HashMap<String, Boolean> getMachines() {
        return this.machines;
    }

    public void setMachines(HashMap<String, Boolean> machines) {
        this.machines = machines;
    }

    public void addWorkers(ArrayList<Worker> workers) {
        for(Worker worker : workers) {
            this.workers.put(worker.getID(), true);
        }
    }
}
