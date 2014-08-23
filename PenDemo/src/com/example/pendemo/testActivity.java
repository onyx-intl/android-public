package com.example.pendemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;

import com.onyx.android.sdk.data.cms.OnyxScribble;
import com.onyx.android.sdk.data.cms.OnyxScribblePoint;
import com.onyx.android.sdk.device.EpdController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by solskjaer49 on 14/8/22 20:02.
 */
public class testActivity extends Activity {
    private Paint paint = new Paint();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_test);
        final SurfaceView surfaceView=(SurfaceView)findViewById(R.id.test);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        final Bitmap baseBitmap = Bitmap.createBitmap(825,1200, Bitmap.Config.ARGB_8888);

        //Save png and show last paint.
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Canvas canvas=surfaceView.getHolder().lockCanvas();
                Canvas tempCanvas=new Canvas(baseBitmap);
                canvas.drawColor(Color.WHITE);
                tempCanvas.drawColor(Color.WHITE);
                paintScribbles(canvas);
                paintScribbles(tempCanvas);
                surfaceView.getHolder().unlockCanvasAndPost(canvas);
                File saveFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/test" + ".png");
                if (!saveFile.exists()){
                    try {
                        saveFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    FileOutputStream iStream = new FileOutputStream(saveFile);
                    baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, iStream);
                    iStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void paintScribbles(Canvas canvas) {
        ArrayList<OnyxScribble> scribbleList=BrushManager.getInstance().loadScribblesOfPosition(testActivity.this, BrushManager.FAKE_MD5, "1");
        for (OnyxScribble scribble :scribbleList){
            ArrayList<OnyxScribblePoint> tempPoints=scribble.getPoints();
            for (int i = 0; i < tempPoints.size() - 1; i++) {
                paint.setStrokeWidth(tempPoints.get(i).getSize());
                canvas.drawLine(tempPoints.get(i).getX(), tempPoints.get(i).getY(),
                        tempPoints.get(i + 1).getX(), tempPoints.get(i + 1).getY(), paint);
            }
        }
    }
}
