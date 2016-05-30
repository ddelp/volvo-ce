package com.ddelp.volvoce;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.ddelp.volvoce.helpers.CollisionDetecter;
import com.ddelp.volvoce.objects.Machine;
import com.ddelp.volvoce.objects.Worker;
import com.ddelp.volvoce.objects.Worksite;
import com.ddelp.volvoce.helpers.WorksiteView;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WorksiteViewActivity extends AppCompatActivity {

    /** Tag for logging */
    private static final String TAG = WorksiteViewActivity.class.getSimpleName();
    /** WorksiteView to display worksite and Workers/Machines */
    private WorksiteView worksiteView;
    /** Name of worksite */
    private String worksiteName;
    /** App Firebase reference */
    private Firebase firebaseRef;
    /** Reference to shared preferences */
    private SharedPreferences prefs;
    /** The worksite we are viewing */
    private Worksite thisWorksite;
    /** The Workers in the Worksite */
    Map<String, Worker> workers;
    /** The Machines in the Worksite */
    Map<String, Machine> machines;

    private CollisionDetecter collisionDetecter;
    private static final int COLLISION_DETECTION_RATE = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_worksite_view);

        // Hide the action bar if it's visible and make menu bar transparent
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Grab our MAC address from shared preferences
        prefs = getSharedPreferences(SplashActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
        String myAddress = prefs.getString(SplashActivity.MAC_ADDRESS_KEY, null).replace(":","");

        // Get the worksite name passed in the intent from WorksiteListFragment
        Bundle extras = getIntent().getExtras();
        try {
            worksiteName = extras.getString("worksite_name");
        } catch (Exception e) {
            Log.i(TAG, "Error getting Worksite name to load");
        }

        // Set up the worksite view, set its max scale, address and Icon select callback
        worksiteView = (WorksiteView) findViewById(R.id.newWorksiteView);
        //TODO: programmatically select image from worksite name
        if(worksiteName.contentEquals("worksite1")) {
            worksiteView.setImage(ImageSource.resource(R.drawable.worksite1));
        } else if(worksiteName.contentEquals("worksite2")) {
            worksiteView.setImage(ImageSource.resource(R.drawable.worksite2));
        } else {
            worksiteView.setImage(ImageSource.resource(R.drawable.worksite3));
        }
        worksiteView.setMaxScale(5f);
        worksiteView.setMyAddress(myAddress);
        worksiteView.setIconSelectListener(new WorksiteView.IconSelectListener() {
            @Override
            public void onIconSelect(String id) {
                Log.i(TAG, "Icon selected: " + id + ". Show information dialog");
            }
        });

        // Start loading worksite from Firebase
        firebaseRef = new Firebase(getResources().getString(R.string.firebase_app));
        Log.i(TAG, "Loading worksite from Firebase: " + worksiteName);
        Firebase worksiteRef = firebaseRef.child("worksites").child(worksiteName);
        if(worksiteRef != null) {
            worksiteRef.addListenerForSingleValueEvent(worksiteListener);
        } else {
            Log.i(TAG, "Firebase worksite reference null: " + worksiteName);
            showErrorDialog("Firebase worksite reference null: " + worksiteName);
        }

        workers = new HashMap<>();
        machines = new HashMap<>();
        collisionDetecter = new CollisionDetecter(this);
        new Handler().postDelayed(checkCollisions, COLLISION_DETECTION_RATE);
    }

    public Map<String,Worker> getWorkers() {
        return workers;
    }

    public Map<String,Machine> getMachines() {
        return machines;
    }

    /**
     * Runnable to open the worksite select activity after all data is loaded
     */
    private final Runnable checkCollisions = new Runnable() {
        @Override
        public void run() {
            Map<String, Boolean> collisions = collisionDetecter.detectCollisions(workers, machines);
            for(Worker worker : workers.values()) {
                Firebase alertRef = firebaseRef.child("alerts").child(worker.getID());
                if(collisions.containsKey(worker.getID())) {
                    alertRef.setValue(true);
                } else {
                    alertRef.setValue(false);
                }
            }
            for(Machine machine : machines.values()) {
                Firebase alertRef = firebaseRef.child("alerts").child(machine.getID());
                if(collisions.containsKey(machine.getID())) {
                    alertRef.setValue(true);
                } else {
                    alertRef.setValue(false);
                }
            }
            new Handler().postDelayed(checkCollisions, COLLISION_DETECTION_RATE);
        }
    };


    /************************************ Firebase callbacks ************************************/

    /**
     * worksiteListener
     */
    ValueEventListener worksiteListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.getValue() == null) {
                Log.i(TAG, "Worksite referenced from list, not found in worksites");
                showErrorDialog("Worksite not found: ");
                return;
            }
            thisWorksite = dataSnapshot.getValue(Worksite.class);
            worksiteView.setWorksiteGPS(thisWorksite.getTopLeft(), thisWorksite.getBottomRight());
            for(String workerID : thisWorksite.getWorkers().keySet()) {
                Firebase workerRef = firebaseRef.child("workers").child(workerID);
                if(workerRef != null) {
                    workerRef.addValueEventListener(workerListener);
                }
            }
            for(String machineID : thisWorksite.getMachines().keySet()) {
                Firebase machineRef = firebaseRef.child("machines").child(machineID);
                if(machineRef != null) {
                    machineRef.addValueEventListener(machineListener);
                }
            }

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.i(TAG, "Couldn't download worksite list");
            showErrorDialog(firebaseError.toString());
        }
    };

    /**
     * workerListener
     */
    ValueEventListener workerListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.getValue() == null) {
                Log.i(TAG, "Worker referenced in worksite, not found in workers");
                return;
            }
            Worker worker = dataSnapshot.getValue(Worker.class);
            workers.put(worker.getID(), worker);
            worksiteView.setWorker(worker);
            Log.i(TAG, "workerDownloadListener: " + worker.getID() + " added to database");
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.i(TAG, "Couldn't download worker");
            showErrorDialog(firebaseError.toString());
        }
    };

    /**
     * machineListener
     */
    ValueEventListener machineListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.getValue() == null) {
                Log.i(TAG, "Machine referenced in worksite, not found in machines");
                return;
            }
            Machine machine = dataSnapshot.getValue(Machine.class);
            machines.put(machine.getID(), machine);
            worksiteView.setMachine(machine);
            Log.i(TAG, "machineListener: " + machine.getID() + " added to database");
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.i(TAG, "Couldn't download machine");
            showErrorDialog(firebaseError.toString());
        }
    };

    /**
     * Helper to show an error to the user.
     * @param message The message to display in the dialogue
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
