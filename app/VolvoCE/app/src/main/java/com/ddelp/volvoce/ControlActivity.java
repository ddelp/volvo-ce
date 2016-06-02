package com.ddelp.volvoce;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ddelp.volvoce.fragments.SettingsFragment;
import com.ddelp.volvoce.fragments.WorksiteListFragment;
import com.ddelp.volvoce.helpers.BluetoothHelper;
import com.ddelp.volvoce.helpers.LocationHelper;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.vistrav.ask.Ask;

import java.util.ArrayList;
import java.util.List;

public class ControlActivity extends AppCompatActivity {

    private static final String TAG = ControlActivity.class.getSimpleName();
    private static final int LOCATION_UPDATE_RATE = 2000;
    private static final int ALERT_DELAY_TIME = 2000;
    private static boolean alert = false;
    private static String myAddress;

    private Firebase firebaseRef;
    private SharedPreferences prefs;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private int[] tabIcons = {
            R.drawable.icon_worksite_select,
            R.drawable.icon_settings
    };

    /** GPS stuff */
    LocationHelper locationHelper;
    /** Bluetooth low energy stuff */
    BluetoothHelper bleHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new WorksiteListFragment(), "WORKSITES");
        adapter.addFragment(new SettingsFragment(), "SETTINGS");
        viewPager.setAdapter(adapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);

        locationHelper = LocationHelper.getInstance(this);
        new Handler().postDelayed(getLocation, LOCATION_UPDATE_RATE);

        bleHelper = BluetoothHelper.getInstance(this); // Register/start ble scan
        //bleHelper.startScan(); // TODO: change to manual connection with selected MAC address

        // Make sure we have permissions set up
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Request permission for fine location");
            Ask.on(this).forPermissions(Manifest.permission.ACCESS_FINE_LOCATION).go();
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Request permission for coarse location");
            Ask.on(this).forPermissions(Manifest.permission.ACCESS_COARSE_LOCATION).go();
        }

        // Set alert listener for our MAC address from shared preferences
        prefs = getSharedPreferences(SplashActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
        myAddress = prefs.getString(SplashActivity.MAC_ADDRESS_KEY, null).replace(":","");
        firebaseRef = new Firebase(getResources().getString(R.string.firebase_app));
        Firebase alertRef = firebaseRef.child("alerts").child(myAddress);
        if(alertRef != null) {
            alertRef.addValueEventListener(alertListener);
        } else {
            Log.d(TAG, "Alert Reference is wrong: " + myAddress);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationHelper != null) {
            locationHelper.stopUsingGPS();
        }
        if(bleHelper != null) {
            bleHelper.stopScan();
            bleHelper.disconnect();
        }
        Log.d(TAG, "onDestroy() event");
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    /**
     * Get the users location and report it to firebase (persists until app is destroyed...)
     */
    private final Runnable getLocation = new Runnable() {
        @Override
        public void run() {
            // only report if the location is different
            Location myLocation = locationHelper.getCurrentBestLocation();
            if(myLocation != null) {
                Firebase myGPSRef = firebaseRef.child("workers").child(myAddress).child("gps");
                double latitude = myLocation.getLatitude();
                double longitude = myLocation.getLongitude();
                String gps = latitude + "," + longitude;
                myGPSRef.setValue(gps);
                //Log.i(TAG, "Sending users location to firebase: " + gps);
            }
            new Handler().postDelayed(getLocation, LOCATION_UPDATE_RATE);
        }
    };

    /**
     * Runnable to play the alert sound to the user (persists until app is destroyed...)
     */
    private final Runnable playAlertSound = new Runnable() {
        @Override
        public void run() {
            if(alert) {
                Log.i(TAG, "Playing alert sound!");
                MediaPlayer mp = MediaPlayer.create(getApplication(), R.raw.alert); // sound is inside res/raw/mysound
                mp.start();
                new Handler().postDelayed(playAlertSound, ALERT_DELAY_TIME);
            }
        }
    };

    /**
     * Listener to detect if this device has an active alert
     */
    ValueEventListener alertListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.getValue() == null) {
                Log.i(TAG, "Alert is null");
                return;
            }
            alert = (boolean)dataSnapshot.getValue();
            if(alert) {
                Log.i(TAG, "Alarm Status: on");
                new Handler().post(playAlertSound);
                bleHelper.sendData("A"); // Send alert data packet
            } else {
                Log.i(TAG, "Alarm Status: off");
                bleHelper.sendData("O"); // Send alert off data packet
            }

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.i(TAG, "Couldn't download worksite list");
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
