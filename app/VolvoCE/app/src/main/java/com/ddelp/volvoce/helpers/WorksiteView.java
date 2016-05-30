package com.ddelp.volvoce.helpers;

import android.content.Context;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.ddelp.volvoce.R;
import com.ddelp.volvoce.objects.Machine;
import com.ddelp.volvoce.objects.Worker;

import java.util.HashMap;
import java.util.Map;

public class WorksiteView extends SubsamplingScaleImageView implements View.OnTouchListener {

    /** Tag for logging */
    private static final String TAG = "WorksiteView";
    /** This devices MAC address */
    private String myAddress;
    /** Worksite GPS coordinates (Top,Bottom,Left,Right) */
    private WorksiteGPS gps;
    /** Map of icons displayed in WorksiteView */
    private Map<String, Icon> icons;
    /** Flag if motion event is simple click */
    private static boolean isClick = false;
    /** Listener for icon select callbacks */
    private IconSelectListener listener;


    public WorksiteView(Context context) {
        this(context, null);
    }

    public WorksiteView(Context context, AttributeSet attr) {
        super(context, attr);
        setOnTouchListener(this);
        this.listener = null;
        icons = new HashMap<>();
    }

    /**
     * Set this devices MAC address to draw unique icon on worksite if
     * device is present/
     *
     * @param address The MAC address of this device
     */
    public void setMyAddress(String address) {
        this.myAddress = address;
    }

    /**
     * Set the worksites GPS Top,Left,Bottom,Right coordinates
     *
     * @param topLeft TopLeft GPS coordinates of the image
     * @param bottomRight BottomRight GPS coordinates of the image
     */
    public void setWorksiteGPS(String topLeft, String bottomRight) {
        PointGPS tl = parseGPSPointFromString(topLeft);
        PointGPS br = parseGPSPointFromString(bottomRight);
        gps = new WorksiteGPS(tl.latitude, br.latitude, tl.longitude, br.longitude);
        Log.d(TAG, "WorksiteGPS set: top: " + gps.top + " left: " + gps.left + " bottom: "
                + gps.bottom + " right: " +gps.right);
        Log.d(TAG, "WorksiteGPS Height: " + gps.getHeight() + " Width: " + gps.getWidth());
        Log.d(TAG, "ImageDimensions: Height: " + getSHeight() + " Width: " + getSWidth());
    }

    /**
     * Add/Update a Worker to the worksite view
     *
     * @param worker The Worker to add/update
     */
    public void setWorker(Worker worker) {
        setIcon(Icon.ICON_TYPE_WORKER, worker.getID(), worker.getGPS());
    }

    /**
     * Add/Update a Machine to the worksite view
     *
     * @param machine The Machine to add/update
     */
    public void setMachine(Machine machine) {
        setIcon(Icon.ICON_TYPE_MACHINE, machine.getID(), machine.getGPS());
    }

    /**
     * Creates or updates an Icon to be drawn on the worksite map
     *
     * @param type Type (Worker/Machine)
     * @param id ID (Usually device MAC address)
     * @param GPS Coordinates formatted "latitude,longitude" in decimal-degree
     */
    public void setIcon(int type, String id, String GPS) {
        PointGPS gpsPoint = parseGPSPointFromString(GPS);
        PointF point = convertGPSPointToPixelLocation(gpsPoint);
        Icon icon = icons.get(id);
        if(icon != null) {
            // Icon already exists, update location only
            icon.point = point;
            icons.put(id, icon);
        } else {
            // Make a new icon with corresponding type bitmap
            int iconImage;
            if(id.equals(myAddress)) {
                iconImage = R.drawable.icon_location_blue;
            } else if(type == Icon.ICON_TYPE_MACHINE) {
                iconImage = R.drawable.icon_location_yellow;
            } else if(type == Icon.ICON_TYPE_WORKER) {
                iconImage = R.drawable.icon_location_green;
            } else {
                iconImage = R.drawable.icon_location_green;
            }
            float density = getResources().getDisplayMetrics().densityDpi;
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), iconImage);
            float w = (density/420f) * bitmap.getWidth()/25;
            float h = (density/420f) * bitmap.getHeight()/25;
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)w, (int)h, true);
            icons.put(id, new Icon(type, point, bitmap));
        }
        invalidate();
    }

    /**
     * Parse a string to a PointGPS.
     *
     * @param gpsPoint The String to parse (formatted "latitude,longitude" in decimal-degree)
     * @return a PointGPS representing the parsed string
     */
    private PointGPS parseGPSPointFromString(String gpsPoint) {
        try {
            String[] coordinates = gpsPoint.split(",");
            float latitude = Float.parseFloat(coordinates[0]);
            float longitude = Float.parseFloat(coordinates[1]);
            return new PointGPS(latitude, longitude);
        } catch (Exception e) {
            Log.d(TAG, "parseGPSPointFromString: Invalid formatting of gpsPoint: " + gpsPoint);
            return new PointGPS();
        }
    }

    /**
     * Assumes picture orientation is North to the top.
     * Thus max/min latitude is the worksites gps.top/gps.bottom and
     * max/min longitude is right/left (since longitude is signed)
     * Note: This works as long as image doesn't cross international date line
     *
     * @param p PointGPS to convert
     * @return Pixel point to draw icon
     */
    private PointF convertGPSPointToPixelLocation(PointGPS p) {
        float latitudePix;
        float longitudePix;

        if((gps.top > p.latitude || gps.bottom < p.latitude) // latitude check
                && (gps.left < p.longitude && gps.right > p.longitude)) { // longitude check
            latitudePix = Math.round(((gps.top - p.latitude)/gps.getHeight())*getSHeight());
            longitudePix = Math.round(((p.longitude - gps.left)/gps.getWidth())*getSWidth());
        } else { // Icon is out of image frame...
            latitudePix = -50f;
            longitudePix = -50f;
        }
        return new PointF(longitudePix, latitudePix);
    }

    /**
     * Remove a worker from the worksite
     * @param id ID to remove
     */
    public void removeWorker(String id) {
        removeIcon(id);
    }

    /**
     * Remove a machine from the worksite
     * @param id ID to remove
     */
    public void removeMachine(String id) {
        removeIcon(id);
    }

    private void removeIcon(String id) {
        icons.remove(id);
    }

    /**
     * Get an icon with the corresponding ID
     * @param id The Icon ID
     * @return Icon with requested ID
     */
    public Icon getIcon(String id) {
        return icons.get(id);
    }

    /**
     * Get icons associated with worksite
     * @return Map of Icons in the format <IconID,Icon>
     */
    public Map<String, Icon> getIcons() {
        return icons;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady()) {
            return;
        }
        // Draw all icons
        if(icons == null) {
            return;
        }
        for(Icon icon : icons.values()) {
            if (icon.isNull()) {
                Log.d(TAG, "Icon is null");
                continue;
            }
            PointF vIcon = sourceToViewCoord(icon.point);
            float vX = vIcon.x - (icon.bitmap.getWidth()/2);
            float vY = vIcon.y - icon.bitmap.getHeight();
            canvas.drawBitmap(icon.bitmap, vX, vY, icon.paint);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean iconClick = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isClick = true;
                break;
            case MotionEvent.ACTION_MOVE:
                isClick = false;
                break;
            case MotionEvent.ACTION_UP:
                if(isClick) {
                    float x = event.getX();
                    float y = event.getY();

                    for (Map.Entry<String, Icon> entry : icons.entrySet()) {
                        String id = entry.getKey();
                        Icon icon = entry.getValue();
                        if (icon.isNull()) {
                            continue;
                        }
                        PointF vIcon = sourceToViewCoord(icon.point);
                        int width = icon.bitmap.getWidth();
                        int height = icon.bitmap.getHeight();
                        float vX = vIcon.x - (width/2);
                        float vY = vIcon.y - height;

                        //Check if the x and y position of the touch is inside the bitmap
                        if( x > vX && x < vX + width && y > vY && y < vY + height ) {
                            Log.i(TAG, "Clicked an icon! ID: " + id);
                            iconClick = true;
                            listener.onIconSelect(id);
                            break;
                        }
                    }

                }
                break;
        }
        // Use parent to handle pinch and two-finger pan.
        if(iconClick) return iconClick;
        return super.onTouchEvent(event);
    }


    /********************************** Callback Implementation **********************************/

    /**
     * Interface definition for Icon select callback
     */
    public interface IconSelectListener {
        public void onIconSelect(String id);
    }

    /**
     * Assign the listener implementing events interface that will receive the events
     *
     * @param listener
     */
    public void setIconSelectListener(IconSelectListener listener) {
        this.listener = listener;
    }

}

