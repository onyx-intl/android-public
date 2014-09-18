package com.onyx.scribbleTest;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import android.graphics.*;
import android.view.Display;
import android.view.SurfaceView;

import android.view.WindowManager;
import com.onyx.android.sdk.device.EpdController;


import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhuzeng
 * Date: 6/6/14
 * Time: 9:54 AM
 * To change this template use File | Settings | File Templates.
 */

public class PaintView extends SurfaceView {
    private static final String TAG = PaintView.class.getSimpleName();
    private List<Path> paths = new ArrayList<Path>();
    private Path currentPath;
    private List<Point> points = new ArrayList<Point>();
    private int size = 3;
    private Paint paint = new Paint();
    private Matrix mapMatrix = null;

    private int lastX, lastY;
    private boolean drawOnScreen = true;

    public static int displayWidth, displayHeight;
    public static int INCH_8_WIDTH = 1200;
    public static int INCH_97_WIDTH = 825;
    public static int INCH_68_WIDTH = 1080;
    public static int INCH_68_HEIGHT = 1440;

    public void initDisplay(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }

    static public boolean is8Inch() {
        return displayWidth == INCH_8_WIDTH;
    }

    static public boolean is97Inch() {
        return displayWidth == INCH_97_WIDTH;
    }

    static public boolean is68Inch() {
        return displayWidth == INCH_68_WIDTH;
    }

    public PaintView(android.content.Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        initDisplay(context);
        initMapMatrix();
    }

    public PaintView(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
        initDisplay(context);
        initMapMatrix();
    }

    public PaintView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBackgroundColor(Color.WHITE);
        initDisplay(context);
        initMapMatrix();
    }

    private void initMapMatrix() {
        if (is97Inch()) {
            initMapMatrix97();
        } else if (is8Inch()) {
            initMapMatrix8();
        } else if (is68Inch()) {
            initMapMatrix68();
        }
    }

    private void initMapMatrix97() {
        mapMatrix = new Matrix();
        mapMatrix.postRotate(270);
        mapMatrix.postTranslate(0, INCH_97_WIDTH);
    }

    private void initMapMatrix8() {
        mapMatrix = new Matrix();
        mapMatrix.postRotate(270);
        mapMatrix.postTranslate(0, INCH_8_WIDTH);
    }

    private void initMapMatrix68() {
        mapMatrix = new Matrix();
        mapMatrix.postRotate(90);
        mapMatrix.postTranslate(INCH_68_HEIGHT, 0);
    }

    public void init(int w, int h) {

    }

    float [] mapPoint(float x, float y) {
        int viewLocation[] = {0, 0};
        getLocationOnScreen(viewLocation);
        float screenPoints[] = {viewLocation[0] + x, viewLocation[1] + y};
        float dst[] = {0, 0};
        mapMatrix.mapPoints(dst, screenPoints);
        return dst;
    }

}
