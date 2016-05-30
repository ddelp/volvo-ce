package com.ddelp.volvoce;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ddelp.volvoce.helpers.DatabaseHelper;
import com.ddelp.volvoce.objects.Machine;
import com.ddelp.volvoce.objects.Worker;
import com.ddelp.volvoce.objects.Worksite;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private static final boolean FIREBASE_ACTIVE = true;
    private static final boolean FIREBASE_RESET_DB = false;
    private static final int SPLASH_DISPLAY_TIME = 1000;
    public static String MY_PREFERENCES = "myPrefs";
    public static String MAC_ADDRESS_KEY = "mac_address";

    private View splash;
    private TextView status;
    private ProgressBar progressBar;

    private Firebase firebaseRef;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Log.i(TAG, getString(R.string.app_name) + ": Start splash activity");

        // Hide the action bar if it's visible
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Hide the UI menus
        splash = findViewById(R.id.splash_image);
        if (splash != null) {
            splash.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        // Set textview and loading animation to let user know data is loading
        status = (TextView) findViewById(R.id.status_message);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Firebase anonymous login
        if(FIREBASE_ACTIVE) {
            status.setText(getString(R.string.firebase_connecting));
            firebaseRef = new Firebase(getResources().getString(R.string.firebase_app));
            firebaseRef.authAnonymously(loginResultHandler);
        } else {
            new Handler().postDelayed(openWorksiteSelectActivity, SPLASH_DISPLAY_TIME);
        }

        // Get device MAC address and add it to local shared preferences
        String address = getMacAddress();
        prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(MAC_ADDRESS_KEY, address);
        edit.apply();
    }

    /**
     * Runnable to open the worksite select activity after all data is loaded
     */
    private final Runnable openWorksiteSelectActivity = new Runnable() {
        @Override
        public void run() {
            /* Create an Intent that will start the Worksite Select Activity. */
            Log.i(TAG, getString(R.string.app_name) + ": Starting WorksiteSelectActivity");
            //Intent mainIntent = new Intent(SplashActivity.this, WorksiteSelectActivity.class);
            Intent mainIntent = new Intent(SplashActivity.this, ControlActivity.class);
            SplashActivity.this.startActivity(mainIntent);
            SplashActivity.this.finish();
        }
    };

    /**
     * Auth handler called when we try to connect to Firebase
     */
    private final Firebase.AuthResultHandler loginResultHandler = new Firebase.AuthResultHandler() {
        @Override
        public void onAuthenticated(AuthData authData) {
            Log.i(TAG, "Firebase - Successfully authenticated");
            if(FIREBASE_RESET_DB) {
                setUpFirebaseDB(); // Reset our database to default values...
            }
            status.setText(getString(R.string.firebase_dl_data));
            Firebase worksiteListRef = firebaseRef.child("worksites");
            worksiteListRef.addListenerForSingleValueEvent(worksitesDownloadListener);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.i(TAG, "Firebase - Failed to authenticate");
            showErrorDialog(firebaseError.toString());
        }
    };

    /**
     * worksiteListDownload
     */
    ValueEventListener worksitesDownloadListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
            for (DataSnapshot worksiteSnapshot: dataSnapshot.getChildren()) {
                String worksiteName = worksiteSnapshot.getKey();
                //Log.i(TAG, "Worksite name: " + worksiteName);
                databaseHelper.addWorksiteToList(worksiteName);
            }
            new Handler().postDelayed(openWorksiteSelectActivity, SPLASH_DISPLAY_TIME);
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.i(TAG, "Couldn't download worksite list");
            showErrorDialog(firebaseError.toString());
        }
    };

    /**
     * getMacAddress
     * http://stackoverflow.com/questions/31329733/how-to-get-the-missing-wifi-mac-address-on-android-m-preview/32948723#32948723
     *
     * @return the MAC address of the device as a string
     */
    public String getMacAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                String address = buf.toString();
                Log.i(TAG, "Device MAC Address: " + address);
                return address;
            }
        } catch (Exception ex) { // for now eat exceptions
            Log.i(TAG, "No MAC address or wi-fi is disabled");
        }
        return "";
    }

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

    /**
     * Reset firebase data for testing
     */
    private void setUpFirebaseDB() {
        // TODO: name worksites properly
        // Save a list of active Workers (ID is MAC address of device.. then username, GPS)
        Map<String, Worker> workersList = new HashMap<>();
        workersList.put("5430AF01EB81", new Worker("5430AF01EB81", "Bro1", "37.434500,-122.177000", "worksite1"));
        workersList.put("5430AF01EB82", new Worker("5430AF01EB82", "Bro2", "37.435500,-122.178000", "worksite1"));
        workersList.put("5430AF01EB83", new Worker("5430AF01EB83", "Bro3", "37.426000,-122.171747", "worksite2"));
        workersList.put("5430AF01EB84", new Worker("5430AF01EB84", "Bro4", "37.423550,-122.175514", "worksite3"));
        Firebase workersRef = firebaseRef.child("workers");
        workersRef.setValue(workersList);

        // Save a list of machines(ID is MAC address of device.. then username, GPS)
        Map<String, Machine> machinesList = new HashMap<>();
        machinesList.put("5430AF01EB89", new Machine("5430AF01EB89", Machine.MACHINE_TYPE_NONE, "37.43500,-122.177000", "worksite1"));
        Firebase machinesRef = firebaseRef.child("machines");
        machinesRef.setValue(machinesList);

        // Save the worksite information
        Map<String, Worksite> worksites = new HashMap<>();
        Worksite w1 = new Worksite("worksite1", "37.436178,-122.179197", "37.433425,-122.175617");
        w1.addWorker("5430AF01EB81");
        w1.addWorker("5430AF01EB82");
        w1.addMachine("5430AF01EB89");
        Worksite w2 = new Worksite("worksite2", "37.427066,-122.173747", "37.425358,-122.169676");
        w2.addWorker("5430AF01EB83");
        Worksite w3 = new Worksite("worksite3", "37.424550,-122.178514", "37.422344,-122.173473");
        w3.addWorker("5430AF01EB84");
        worksites.put("worksite1", w1);
        worksites.put("worksite2", w2);
        worksites.put("worksite3", w3);
        Firebase worksitesRef = firebaseRef.child("worksites");
        worksitesRef.setValue(worksites);

        // Notify user db was reset
        Toast.makeText(this, "Firebase information has been reset", Toast.LENGTH_SHORT).show();
    }
}
