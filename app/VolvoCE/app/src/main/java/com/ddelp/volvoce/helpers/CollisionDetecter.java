package com.ddelp.volvoce.helpers;

import android.content.Context;
import android.util.Log;

import com.ddelp.volvoce.objects.Machine;
import com.ddelp.volvoce.objects.Worker;
import com.firebase.client.Firebase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Denny on 5/16/16.
 */
public class CollisionDetecter {

    private static final String TAG = "CollisionDetecter";
    private Firebase firebaseRef;
    private static final double ALERT_THRESHOLD = 0.0001; //Get actual value

    public CollisionDetecter(Context context) {
    }

    public Map<String, Boolean> detectCollisions(Map<String,Worker> workers,
                                              Map<String,Machine> machines) {

        Map<String, Boolean> collisionIDs = new HashMap<>();

        for(Worker worker: workers.values()) {
            String[] workerGPS = worker.getGPS().split(",");
            float workerLat = Float.parseFloat(workerGPS[0]);
            float workerLong = Float.parseFloat(workerGPS[1]);
//            Log.i(TAG, "Worker Location: " + workerLat + ", " + workerLong);
            for(Machine machine : machines.values()) {
                String[] machineGPS = machine.getGPS().split(",");
                float machineLat = Float.parseFloat(machineGPS[0]);
                float machineLong = Float.parseFloat(machineGPS[1]);
//                Log.i(TAG, "Machine Location: " + machineLat + ", " + machineLong);
                double deltaLat = Math.abs(workerLat - machineLat);
                double deltaLong = Math.abs(workerLong - machineLong);
//                Log.i(TAG, "Delta: " + deltaLat + ", " + deltaLong);
                double distance = Math.sqrt(Math.pow(deltaLat,2) + Math.pow(deltaLong,2));
//                Log.i(TAG, "Calculated distance: WorkerID: " + worker.getID() + "  MachineID: "
//                        + machine.getID() + " Distance: " + distance);
                if(distance <= ALERT_THRESHOLD) {
                    Log.i(TAG, "Collision detected! Worker:" + worker.getID() + " Machine: " + machine.getID());
                    collisionIDs.put(worker.getID(), true);
                    collisionIDs.put(machine.getID(), true);
                }
            }
        }
        return collisionIDs;
    }



}

