package com.ddelp.volvoce.helpers;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PointF;

/**
 * Store the information for an icon we need to draw on the worksite map.
 */
public class Icon {
    public static final int ICON_TYPE_WORKER = 0;
    public static final int ICON_TYPE_MACHINE = 1;

    int type;
    PointF point;
    Bitmap bitmap;
    Paint paint;

    Icon(int type) {
        this(type, null, null);
    }

    Icon(int type, PointF sPoint, Bitmap point) {
        this.type = type;
        this.point = sPoint;
        this.bitmap = point;
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    boolean isWorker(){
        return type == ICON_TYPE_WORKER;
    }

    boolean isMachine(){
        return type == ICON_TYPE_MACHINE;
    }

    boolean isNull() {
        return (this.point == null || this.bitmap == null);
    }
}