package com.ddelp.volvoce.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ddelp.volvoce.R;
import com.ddelp.volvoce.SplashActivity;
import com.ddelp.volvoce.helpers.BluetoothHelper;
import com.ddelp.volvoce.helpers.LocationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;


public class SettingsFragment extends Fragment{

    private static final String TAG = "SettingsFragment";

    BluetoothHelper ble;
    BluetoothHelper.ConnectionListener connectionListener;
    private SharedPreferences prefs;

    TextView vConnection;
    TextView vAddress;
    TextView vGPSStatus;
    TextView vGPSLocation;
    TextView vGPSTimestamp;
    Button bConnect;
    Button bAlarmOn;
    Button bAlarmOff;

    LocationHelper locationHelper;
    LocationHelper.GPSListener locationListener;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        vConnection = (TextView) rootView.findViewById(R.id.ble_status);
        vAddress = (TextView) rootView.findViewById(R.id.id);
        vGPSStatus = (TextView) rootView.findViewById(R.id.gps_status);
        vGPSLocation = (TextView) rootView.findViewById(R.id.gps_location);
        vGPSTimestamp = (TextView) rootView.findViewById(R.id.gps_timestamp);
        bConnect = (Button) rootView.findViewById(R.id.connect);
        bAlarmOn = (Button) rootView.findViewById(R.id.a);
        bAlarmOff = (Button) rootView.findViewById(R.id.o);

        prefs = getActivity().getSharedPreferences(SplashActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
        String myAddress = prefs.getString(SplashActivity.MAC_ADDRESS_KEY, null);
        vAddress.setText(myAddress);

        ble = BluetoothHelper.getInstance(getActivity());
        connectionListener = new BluetoothHelper.ConnectionListener() {
            @Override
            public void onConnectionChange(boolean connectionStatus) {
                Log.i(TAG, "Connection changed: " + connectionStatus);
                updateUIConnectionStatus(connectionStatus);
            }
        };
        ble.setConnectionListener(connectionListener);

        locationHelper = LocationHelper.getInstance(getActivity());
        locationListener = new LocationHelper.GPSListener() {
            @Override
            public void onGPSChanged(Location location) {
                updateUILocation(location);
            }
        };
        locationHelper.setGPSListener(locationListener);

        bConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Button pressed: Connected status: " + ble.isConnected());
                if(ble.isConnected()) {
                    ble.disconnect();
                } else {
                    new MaterialDialog.Builder(getActivity())
                            .title("Select Hard Hat")
                            .items(R.array.hard_hats)
                            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    String address = text.toString();
                                    address = address.split(" ")[1]; // Assumes correct formatting in arrays.xml
                                    Log.i(TAG, "Selected BLE module: " + address);
                                    ble.startScan(address);
                                    return true;
                                }
                            })
                            .positiveText("Connect")
                            .show();
                }
            }
        });

        bAlarmOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String testPacket = "A";
                Log.i(TAG, "Sending alarm packet: ");
                ble.sendData(testPacket);

                MediaPlayer mp = MediaPlayer.create(getActivity(), R.raw.alert); // sound is inside res/raw/mysound
                mp.start();
            }
        });

        bAlarmOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String testPacket = "O";
                Log.i(TAG, "Sending ok packet: ");
                ble.sendData(testPacket);
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Removing callbacks from settings fragment here!");
    }

    public void updateUIConnectionStatus(final boolean connection) {
        if(getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(connection) {
                    vConnection.setText("Connected");
                    bConnect.setText("Disconnect");
                } else {
                    vConnection.setText("Disconnected");
                    bConnect.setText("Connect");
                }
            }
        });
    }

    public void updateUILocation(final Location location) {
        if(getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status = "Location Source: " + location.getProvider();
                String here = location.getLatitude() + ", " + location.getLongitude();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String time = sdf.format(new Date());
                vGPSStatus.setText(status);
                vGPSLocation.setText(here);
                vGPSTimestamp.setText(time);
            }
        });
    }

}