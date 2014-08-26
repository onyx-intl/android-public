
package com.example.pendemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.onyx.android.sdk.data.cms.OnyxCmsCenter;
import com.onyx.android.sdk.data.cms.OnyxScribble;
import com.onyx.android.sdk.data.cms.OnyxScribblePoint;
import com.onyx.android.sdk.device.DeviceInfo;
import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.ui.data.ScribbleFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Control brush line
 *
 * @author shlf
 * @DATE 2014-07-11
 */
public class BrushManager {
    private static final String TAG = "SHLF-BrushManager";
    private static final boolean DEBUG = true;

    public static enum BrushType {View, Scribble, Erase}

    private static class FlushingPostTask extends TimerTask {
        private View mView;
        private BrushManager brushManager;

        public FlushingPostTask(View view, BrushManager brushManager) {
            mView = view;
            this.brushManager = brushManager;
        }

        public void run() {
            EpdController.leaveScribbleMode(mView);
            brushManager.flushPendingPost();
        }
    }

    private String mMD5 = null;

    private List<PointF> pointList = new ArrayList<PointF>();
    private List<OnyxScribble> pendingScribble = new ArrayList<OnyxScribble>();
    private HashMap<String, List<OnyxScribble>> scribbleMap = new HashMap<String, List<OnyxScribble>>();
    private HashSet<String> scribblePositionsInDb = new HashSet<String>();
    private OnyxScribble currentScribble = null;

    private static BrushManager sInstance;
    private boolean mEnableWakeLock;
    private BrushView mBrushView;

    private Matrix mMapMatrix;

    private int mWidth;
    private int mHeight;
    private Bitmap mDrawingBitmap;

    private Paint mPaint;

    private int mStrokeWidth = BooxUtil.penDefaultWidth;
    private int mPaintWidth;
    private int mPaintColor;

    private BrushType mEditType = BrushType.View;
    private PowerManager.WakeLock mLock;

    private Handler mHandler;
    boolean scribbleInterruptedBecauseOutOfRegion = false;
    public int currentPage = new Random().nextInt(1000);
    private float pageScale = 1.0f;

    Timer timer = new Timer();
    private long touchDownTime = 0;
    private long touchUpTime = 0;

    public static BrushManager getInstance() {
        return sInstance;
    }

    public BrushManager(BrushView mainView, int defaultPaintWidth, int defaultPaintColor, boolean enableWakeLock) {
        super();

        mBrushView = mainView;
        mMapMatrix = new Matrix();
        sInstance = this;

        mHandler = new Handler();

        initBrush(defaultPaintWidth, defaultPaintColor, enableWakeLock);
    }

    public void onSizeChanged(int w, int h) {
        if (DEBUG) Log.d(TAG, "--->>>onSizeChanged()");
        mWidth = w;
        mHeight = h;

        try {
            Bitmap old = mDrawingBitmap;
            mDrawingBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
            if (old != null && !old.isRecycled()) {
                old.recycle();
            }
        } catch (Error OutOfMemoryError) {
            System.gc();
            mDrawingBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        }
    }

    public void onDraw(final Canvas canvas) {
        if (canvas != null && mDrawingBitmap != null) {
            canvas.drawBitmap(mDrawingBitmap, 0, 0, null);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!BooxUtil.isE970B()) {
            return false;
        }
        switch (mEditType) {
            case View:
                return false;
            case Scribble:
                processScribble(event);
                break;
            case Erase:
                processErase(event);
                break;
        }
        return true;
    }

    public void releaseWakeLock(boolean leave) {
        if (BooxUtil.isE970B()) {
            if (mEnableWakeLock && mLock != null) {
                mLock.release();
                mLock = null;
            }

            if (leave) DeviceInfo.currentDevice.leaveScribbleMode(mBrushView);
        }
    }

    public void prepareScribbles(Context context, String md5) {
        mMD5 = md5;

        scribbleMap.clear();
        scribblePositionsInDb.clear();
        if (pendingScribble != null) {
            pendingScribble.clear();
        }
        currentScribble = null;

        OnyxCmsCenter.getScribblePositions(context, context.getPackageName(), md5, scribblePositionsInDb);
    }

    public void setStrokeWidth(int mPaintWidth) {
        this.mStrokeWidth = mPaintWidth;
        this.mPaintWidth = mPaintWidth;
    }

    public void setStrokeColor(int strokeColor) {
        EpdController.setStrokeStyle(strokeColor);
    }

    public boolean saveNoteBookToStorage(String path) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        final Bitmap baseBitmap = Bitmap.createBitmap(825, 1200, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(baseBitmap);
        tempCanvas.drawColor(Color.WHITE);
        BrushManager.getInstance().paintScribbles(tempCanvas, paint);
        File saveFile = new File(path);
        if (!saveFile.exists()) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 保存笔记本
     *
     * @return
     */
    public boolean saveNoteBookToStorage() {
        String path = Environment.getExternalStorageDirectory() + "/DCIM/test" + BrushManager.getInstance().currentPage + ".png";
        return saveNoteBookToStorage(path);
    }

    public Bitmap getBitmap() {
        return mDrawingBitmap;
    }

    public void resetPage(final int type, boolean invalidate) {
        if (BooxUtil.isE970B()) {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    EpdController.enablePost(mBrushView, type);
                }
            }, ((0 == type) ? 2000 : 0));
        }
        if (invalidate) {
            if (null != mBrushView) {
                mBrushView.invalidate();
            }
        }
    }

    public void setEdit() {
        if (DEBUG) Log.d(TAG, "--->>>setEdit()");
        mPaintWidth = mStrokeWidth;
        EpdController.setStrokeStyle(Color.BLACK);
        mEditType = BrushType.Scribble;
    }

    public void setEraser() {
        if (DEBUG) Log.d(TAG, "--->>>setEraser()");
        mPaintWidth = BooxUtil.eraseDefaultWidth;
        EpdController.setStrokeStyle(Color.WHITE);
        mEditType = BrushType.Erase;
    }

    /**
     * clear tmpCanvas.
     */
    public void clear() {
        deletePage(mBrushView.getContext());

        EpdController.enablePost(mBrushView, 1);
        try {
            Bitmap old = mDrawingBitmap;
            mDrawingBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
            if (old != null && !old.isRecycled()) {
                old.recycle();
            }
        } catch (Error OutOfMemoryError) {
            System.gc();
            mDrawingBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        }
        RefreshManager.invalidateGC4(mBrushView, false);
        Canvas canvas = mBrushView.getHolder().lockCanvas();
        if (mBrushView.getBackgroundBitmap() == null) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvas.drawBitmap(mBrushView.getBackgroundBitmap(), 0, 0, null);
        }
        mBrushView.getHolder().unlockCanvasAndPost(canvas);
    }

    public void saveScribbles(Context context, String md5) {
        for (OnyxScribble scribble : pendingScribble) {
            scribble.setMD5(md5);
            OnyxCmsCenter.insertScribble(context, scribble);
        }
        pendingScribble.clear();
    }

    private void startScribble(Context context, int page, double scale, int px, int py, OnyxScribblePoint screenPoint) {
        currentScribble = ScribbleFactory.singleton().newScribble(context, page, scale);
        currentScribble.allocatePoints(512);
        OnyxScribblePoint normalizedPoint = new OnyxScribblePoint((float) ((screenPoint.getX() - px) / scale),
                (float) ((screenPoint.getY() - py) / scale),
                screenPoint.getPressure(), screenPoint.getSize(), screenPoint.getEventTime());
        currentScribble.getPoints().add(normalizedPoint);
    }

    private void addStrokePoints(double scale, int px, int py, OnyxScribblePoint screenPoint, boolean touchDown) {
        OnyxScribblePoint normalizedPoint = new OnyxScribblePoint((float) ((screenPoint.getX() - px) / scale),
                (float) ((screenPoint.getY() - py) / scale),
                screenPoint.getPressure(), screenPoint.getSize(), screenPoint.getEventTime());
        currentScribble.getPoints().add(normalizedPoint);
    }

    private void finishScribble(int page, final String md5) {
        String key = pageKey(page);
        List<OnyxScribble> list = scribbleMap.get(key);
        if (list == null) {
            list = new ArrayList<OnyxScribble>();
            scribbleMap.put(key, list);
        }
        list.add(currentScribble);
        currentScribble.setMD5(md5);
        currentScribble.setPosition(key);
        if (!pendingScribble.contains(currentScribble)) {
            pendingScribble.add(currentScribble);
        }
        currentScribble = null;
    }

    private final String pageKey(int page) {
        return String.valueOf(page);
    }

    // init brush
    private void initBrush(int defaultPaintWidth, int defaultPaintColor, boolean enableWakeLock) {
        mBrushView.setDrawingCacheEnabled(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mPaintColor = defaultPaintColor;
        mPaint.setColor(mPaintColor);

        if (BooxUtil.isE970B()) {
            mPaintWidth = defaultPaintWidth;
            mPaint.setStrokeWidth(mPaintWidth);

            mMapMatrix.postRotate(270);
            mMapMatrix.postTranslate(0, 825);

            mEnableWakeLock = enableWakeLock;
            if (mEnableWakeLock) {
                mLock = DeviceInfo.currentDevice.newWakeLock(mBrushView.getContext(),
                        "Sark Scribble");
                mLock.acquire();
            }
        } else {
            mPaintWidth = defaultPaintWidth;
            mPaint.setStrokeWidth(mPaintWidth);
        }
    }

    private ArrayList<OnyxScribble> loadScribblesOfPosition(Context context, String md5, String position) {
        ArrayList<OnyxScribble> scribbles = new ArrayList<OnyxScribble>();
        if (OnyxCmsCenter.getScribbles(context, context.getPackageName(),
                md5, position, scribbles)) {
            return scribbles;
        }
        return null;
    }

    private void paintScribbles(Canvas canvas, Paint paint) {
        List<OnyxScribble> sendToCanvasScribble = new ArrayList<OnyxScribble>();
        sendToCanvasScribble.addAll(pendingScribble);
        for (OnyxScribble scribble : sendToCanvasScribble) {
            ArrayList<OnyxScribblePoint> tempPoints = scribble.getPoints();
            for (int i = 0; i < tempPoints.size() - 1; i++) {
                if (tempPoints.get(i).getSize() == 0) {
                    paint.setStrokeWidth(BooxUtil.eraseDefaultWidth);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(0);
                    Xfermode xFermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
                    paint.setXfermode(xFermode);
                    canvas.drawLine(tempPoints.get(i).getX(), tempPoints.get(i).getY(),
                            tempPoints.get(i + 1).getX(), tempPoints.get(i + 1).getY(), paint);
                    paint.setStrokeWidth(BooxUtil.penDefaultWidth);
                    paint.setColor(Color.BLACK);
                    paint.setAlpha(255);
                    paint.setXfermode(null);
                } else {
                    paint.setStrokeWidth(tempPoints.get(i).getSize());
                    paint.setColor(Color.BLACK);
                    canvas.drawLine(tempPoints.get(i).getX(), tempPoints.get(i).getY(),
                            tempPoints.get(i + 1).getX(), tempPoints.get(i + 1).getY(), paint);
                }
            }
        }
    }

    private void flushPendingPost() {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(BooxUtil.penDefaultWidth);
        Canvas canvas = mBrushView.getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        if (mBrushView.getBackgroundBitmap() == null) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvas.drawBitmap(mBrushView.getBackgroundBitmap(), 0, 0, null);
        }

        Canvas scribble_canvas = new Canvas(mDrawingBitmap);
        paintScribbles(scribble_canvas, paint);

        canvas.drawBitmap(mDrawingBitmap, 0, 0, null);
        mBrushView.getHolder().unlockCanvasAndPost(canvas);
    }

    private void deletePage(Context context) {
        if (pendingScribble.isEmpty()) {
            List<OnyxScribble> list = loadScribblesOfPosition(context, mMD5, String.valueOf(currentPage));
            for (OnyxScribble s : list) {
                OnyxCmsCenter.deleteScribble(context, s);
            }
        } else {
            pendingScribble.clear();
        }

    }

    private Rect updateSurfaceViewScribbleRegion() {
        int top = mBrushView.getTop();
        int bottom = mBrushView.getBottom();
        return new Rect(mBrushView.getLeft(), top, mBrushView.getRight(), bottom);
    }

    private float[] mapPoint(float x, float y) {
        int viewLocation[] = {
                0, 0
        };
        mBrushView.getLocationOnScreen(viewLocation);
        float screenPoints[] = {
                viewLocation[0] + x, viewLocation[1] + y
        };
        float dst[] = {
                0, 0
        };
        mMapMatrix.mapPoints(dst, screenPoints);
        return dst;
    }

    private void processScribble(MotionEvent event) {
        float dst[] = mapPoint(event.getX(), event.getY());
        OnyxScribblePoint point;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (DEBUG) Log.d(TAG, "--->>>touchDown()");
                touchDownTime = event.getEventTime();
                if (touchUpTime != 0) {
                    if (touchDownTime - touchUpTime < 400) {
                        timer.cancel();
                        timer.purge();
                        timer = new Timer();
                    }
                }
                point = OnyxScribblePoint.fromEvent(event);
                EpdController.enterScribbleMode(mBrushView);
                point.setSize(EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                        event.getSize(), event.getEventTime()));
                startScribble(mBrushView.getContext(), currentPage, pageScale, 0, 0, point);
                pointList.add(new PointF(event.getX(), event.getY()));
                break;
            case MotionEvent.ACTION_MOVE:
                //Todo use rawX to fix right boundary still drawing problem.
                if (!updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                    scribbleInterruptedBecauseOutOfRegion = true;
                    return;
                }
                if (scribbleInterruptedBecauseOutOfRegion) {
                    scribbleInterruptedBecauseOutOfRegion = false;
                    point = OnyxScribblePoint.fromEvent(event);
                    EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    EpdController.enablePost(mBrushView, 0);
                    point.setSize(EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime()));
                    startScribble(mBrushView.getContext(), currentPage, pageScale, 0, 0, point);
                    return;
                } else {
                    int n = event.getHistorySize();
                    for (int i = 0; i < n; i++) {
                        point = OnyxScribblePoint.fromHistoricalEvent(event, i);
                        dst = mapPoint(event.getHistoricalX(i), event.getHistoricalY(i));
                        point.setSize(EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime()));
                        addStrokePoints(pageScale, 0, 0, point, false);
                    }
                    dst = mapPoint(event.getX(), event.getY());
                    point = OnyxScribblePoint.fromEvent(event);
                    point.setSize(EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime()));
                    addStrokePoints(pageScale, 0, 0, point, false);
                }
                pointList.add(new PointF(event.getX(), event.getY()));
                break;
            case MotionEvent.ACTION_UP:
                if (DEBUG) Log.d(TAG, "--->>>touchUp()");
                touchUpTime = event.getEventTime();
                if (mEditType == BrushManager.BrushType.Scribble) {
                    if (updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                        EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                    }
                }
                FlushingPostTask flushingPostTask = new FlushingPostTask(mBrushView, this);
                finishScribble(currentPage, mMD5);
                timer.schedule(flushingPostTask, 400);
                pointList.add(new PointF(event.getX(), event.getY()));
                break;
            default:
                break;
        }
    }

    private void processErase(MotionEvent event) {
        float dst[] = mapPoint(event.getX(), event.getY());
        OnyxScribblePoint point;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (DEBUG) Log.d(TAG, "--->>>touchDown()");
                touchDownTime = event.getEventTime();
                if (touchUpTime != 0) {
                    if (touchDownTime - touchUpTime < 400) {
                        timer.cancel();
                        timer.purge();
                        timer = new Timer();
                    }
                }
                EpdController.enterScribbleMode(mBrushView);
                EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                        event.getSize(), event.getEventTime());
                point = OnyxScribblePoint.fromEvent(event);
                point.setSize(0);
                startScribble(mBrushView.getContext(), currentPage, pageScale, 0, 0, point);
                pointList.add(new PointF(event.getX(), event.getY()));
                break;
            case MotionEvent.ACTION_MOVE:
                //Todo: use rawX to fix right boundary still drawing problem.
                if (!updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                    scribbleInterruptedBecauseOutOfRegion = true;
                    return;
                }
                if (scribbleInterruptedBecauseOutOfRegion) {
                    scribbleInterruptedBecauseOutOfRegion = false;
                    EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    EpdController.enablePost(mBrushView, 0);
                    EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    point = OnyxScribblePoint.fromEvent(event);
                    point.setSize(0);
                    startScribble(mBrushView.getContext(), currentPage, pageScale, 0, 0, point);
                    return;
                } else {
                    int n = event.getHistorySize();
                    for (int i = 0; i < n; i++) {
                        dst = mapPoint(event.getX(), event.getY());
                        EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        point = OnyxScribblePoint.fromHistoricalEvent(event, i);
                        point.setSize(0);
                        addStrokePoints(pageScale, 0, 0, point, false);
                    }
                    dst = mapPoint(event.getX(), event.getY());
                    EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    point = OnyxScribblePoint.fromEvent(event);
                    point.setSize(0);
                    addStrokePoints(pageScale, 0, 0, point, false);
                }
                pointList.add(new PointF(event.getX(), event.getY()));
                break;
            case MotionEvent.ACTION_UP:
                if (DEBUG) Log.d(TAG, "--->>>touchUp()");
                touchUpTime = event.getEventTime();
                if (mEditType == BrushManager.BrushType.Scribble) {
                    if (updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                        EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                    }
                }
                FlushingPostTask flushingPostTask = new FlushingPostTask(mBrushView, this);
                finishScribble(currentPage, mMD5);
                setEdit();
                timer.schedule(flushingPostTask, 400);
                pointList.add(new PointF(event.getX(), event.getY()));
                break;
            default:
                break;
        }
    }

}
