package com.onyx.scribbleTest;

import android.app.Activity;
import android.graphics.Path;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import com.onyx.android.sdk.device.DeviceInfo;
import com.onyx.android.sdk.device.EpdController;

import java.util.ArrayList;
import java.util.List;

public class ScribbleActivity extends Activity {


    private PaintView paintView ;
    private static final String TAG = ScribbleActivity.class.getSimpleName();
    private  PowerManager.WakeLock lock;
    private float currentWidth = 5;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        paintView = (PaintView)findViewById(R.id.paintView);
        paintView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                EpdController.enterScribbleMode(paintView);
                onTouchEvent(ScribbleActivity.this, event);
                return true;
            }
        });



        Button button = (Button)findViewById(R.id.clear_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EpdController.leaveScribbleMode(paintView);
                paintView.invalidate();

            }
        });


        lock = DeviceInfo.currentDevice.newWakeLock(this, "Scribble Test.");
        lock.acquire();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lock != null) {
            lock.release();
            lock = null;
        }
    }

    public boolean onTouchEvent(ScribbleActivity activity, MotionEvent e) {
        // ignore multi touch
        if (e.getPointerCount() > 1) {
            return false;
        }

        final float baseWidth = 5;
        paintView.init(paintView.getWidth(), paintView.getHeight());

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case (MotionEvent.ACTION_DOWN):
                float dst[] = paintView.mapPoint(e.getX(), e.getY());
                EpdController.startStroke(baseWidth, dst[0], dst[1], e.getPressure(), e.getSize(), e.getEventTime());
                Log.i(TAG, "Pressure: " + e.getPressure());
                return true;
            case (MotionEvent.ACTION_CANCEL):
            case (MotionEvent.ACTION_OUTSIDE):
                break;
            case MotionEvent.ACTION_UP:
                dst = paintView.mapPoint(e.getX(), e.getY());
                EpdController.finishStroke(baseWidth, dst[0], dst[1], e.getPressure(), e.getSize(), e.getEventTime());
                Log.i(TAG, "Pressure: " + e.getPressure());
                return true;
            case MotionEvent.ACTION_MOVE:
                int n = e.getHistorySize();
                for (int i = 0; i < n; i++) {
                    dst = paintView.mapPoint(e.getHistoricalX(i), e.getHistoricalY(i));
                    EpdController.addStrokePoint(baseWidth,  dst[0], dst[1],  e.getHistoricalPressure(i), e.getHistoricalSize(i), e.getEventTime());
                    Log.i(TAG, "Pressure: " + e.getHistoricalPressure(i));
                }
                dst = paintView.mapPoint(e.getX(), e.getY());
                EpdController.addStrokePoint(baseWidth, dst[0], dst[1], e.getPressure(), e.getSize(), e.getEventTime());
                return true;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true;
    }

}
