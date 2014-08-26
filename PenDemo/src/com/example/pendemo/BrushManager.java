
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
import android.view.MotionEvent;

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

    private static class UpdateBrushViewTask extends TimerTask {
        private BrushManager brushManager;

        public UpdateBrushViewTask(BrushManager brushManager) {
            this.brushManager = brushManager;
        }

        public void run() {
            brushManager.updateBrushView();
        }
    }

    private String mMD5 = null;
    private PowerManager.WakeLock mLock;
    private Handler mHandler = new Handler();

    private boolean mReady = false;

    private BrushView mBrushView;
    private Bitmap mScribbleBitmap;

    private BrushType mEditType = BrushType.View;

    private Paint mPaint = new Paint();
    private Matrix mMapMatrix = new Matrix();
    private int mStrokeWidth = BooxUtil.penDefaultWidth;
    private int mPaintWidth;

    private PointF mLastTouchMovePoint = new PointF();

    private List<OnyxScribble> mPendingScribble = new ArrayList<OnyxScribble>();
    private HashMap<String, List<OnyxScribble>> mScribbleMap = new HashMap<String, List<OnyxScribble>>();
    private HashSet<String> mScribblePositionsInDb = new HashSet<String>();
    private OnyxScribble mCurrentScribble = null;

    boolean mScribbleInterruptedBecauseOutOfRegion = false;
    public int mCurrentPage = new Random().nextInt(1000);
    private float mPageScale = 1.0f;

    private long mLastTouchDownTime = 0;
    private long mLastTouchUpTime = 0;

    private Runnable mDelayedLeaveScribbleModeAndUpdateBrushView = new Runnable() {
        @Override
        public void run() {
            if (mLastTouchUpTime > mLastTouchDownTime) {
                EpdController.leaveScribbleMode(mBrushView);
                updateBrushView();
            }
        }
    };

    public BrushManager(BrushView brushView, int defaultStrokeWidth) {
        mBrushView = brushView;
        mBrushView.setDrawingCacheEnabled(true);
        mStrokeWidth = defaultStrokeWidth;
        if (BooxUtil.isE970B()) {
            mMapMatrix.postRotate(270);
            mMapMatrix.postTranslate(0, 825);
        }
    }

    public void onSizeChanged(int w, int h) {
        if (mReady) {
            resetScribbleBitmap(w, h);
        }
    }

    public void onDraw(final Canvas canvas) {
        if (canvas != null && mScribbleBitmap != null && !mScribbleBitmap.isRecycled()) {
            canvas.drawBitmap(mScribbleBitmap, 0, 0, null);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!BooxUtil.isE970B()) {
            return false;
        }
        if (mEditType == BrushType.Scribble || mEditType == BrushType.Erase) {
            applyFastScribble(event, mEditType == BrushType.Erase);
            return true;
        }
        return false;
    }

    public void start(String md5, boolean holdWakeLock) {
        if (holdWakeLock) {
            mLock = DeviceInfo.currentDevice.newWakeLock(mBrushView.getContext(), TAG);
            mLock.acquire();
        }
        mMD5 = md5;
        loadScribbles(md5);
        mReady = true;
    }

    /**
     * save scribbles, and recycle scribble bitmap to avoid resource leaking
     *
     * @param releaseWakeLock if holdWakeLock is true in start(), then must release wake lock here
     */
    public void finish(boolean releaseWakeLock) {
        mReady = false;
        saveScribbles(mMD5);
        if (mScribbleBitmap != null && !mScribbleBitmap.isRecycled()) {
            mScribbleBitmap.recycle();
            mScribbleBitmap = null;
        }
        if (BooxUtil.isE970B()) {
            if (releaseWakeLock && mLock != null) {
                mLock.release();
                mLock = null;
            }

            EpdController.leaveScribbleMode(mBrushView);
        }
    }

    /**
     * after finish(), scribble bitmap will be no longer available
     *
     * @return
     */
    public Bitmap getScribbleBitmap() {
        return mScribbleBitmap;
    }

    public void setEdit() {
        mPaintWidth = mStrokeWidth;
        EpdController.setStrokeStyle(Color.BLACK);
        mEditType = BrushType.Scribble;
    }

    public void setEraser() {
        mPaintWidth = BooxUtil.eraseDefaultWidth;
        EpdController.setStrokeStyle(Color.WHITE);
        mEditType = BrushType.Erase;
    }

    public void setStrokeWidth(int mPaintWidth) {
        this.mStrokeWidth = mPaintWidth;
        this.mPaintWidth = mPaintWidth;
    }

    public void setStrokeColor(int strokeColor) {
        EpdController.setStrokeStyle(strokeColor);
    }

    /**
     * clear tmpCanvas.
     */
    public void clear() {
        Canvas canvas = mBrushView.getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            mBrushView.drawBackground(canvas);
            if (mReady) {
                deletePage(mBrushView.getContext());
                resetScribbleBitmap(mBrushView.getWidth(), mBrushView.getHeight());;
            }
        } finally {
            mBrushView.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public boolean saveNoteBookToStorage(String path) {
        final Bitmap bmp = Bitmap.createBitmap(mBrushView.getWidth(), mBrushView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(mScribbleBitmap, 0, 0, null);
        try {
            FileOutputStream is = new FileOutputStream(new File(path));
            try {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, is);
            } finally {
                is.close();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bmp != null && !bmp.isRecycled()) {
            bmp.recycle();
        }
        return false;
    }

    /**
     * 保存笔记本
     *
     * @return
     */
    public boolean saveNoteBookToStorage() {
        String path = Environment.getExternalStorageDirectory() + "/DCIM/test" + mCurrentPage + ".png";
        return saveNoteBookToStorage(path);
    }

    private void resetScribbleBitmap(int w, int h) {
        try {
            Bitmap old = mScribbleBitmap;
            mScribbleBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
            if (old != null && !old.isRecycled()) {
                old.recycle();
            }
        } catch (Error OutOfMemoryError) {
            System.gc();
            mScribbleBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        }
    }

    private void loadScribbles(String md5) {
        mMD5 = md5;
        mScribbleMap.clear();
        mScribblePositionsInDb.clear();
        if (mPendingScribble != null) {
            mPendingScribble.clear();
        }
        mCurrentScribble = null;

        OnyxCmsCenter.getScribblePositions(mBrushView.getContext(), mBrushView.getContext().getPackageName(), md5, mScribblePositionsInDb);
    }

    private void saveScribbles(String md5) {
        for (OnyxScribble scribble : mPendingScribble) {
            scribble.setMD5(md5);
            OnyxCmsCenter.insertScribble(mBrushView.getContext(), scribble);
        }
        mPendingScribble.clear();
    }

    private void startScribble(Context context, int page, double scale, int px, int py, OnyxScribblePoint screenPoint) {
        mCurrentScribble = ScribbleFactory.singleton().newScribble(context, page, scale);
        mCurrentScribble.allocatePoints(512);
        OnyxScribblePoint normalizedPoint = new OnyxScribblePoint((float) ((screenPoint.getX() - px) / scale),
                (float) ((screenPoint.getY() - py) / scale),
                screenPoint.getPressure(), screenPoint.getSize(), screenPoint.getEventTime());
        mCurrentScribble.getPoints().add(normalizedPoint);
    }

    private void addStrokePoints(double scale, int px, int py, OnyxScribblePoint screenPoint, boolean touchDown) {
        OnyxScribblePoint normalizedPoint = new OnyxScribblePoint((float) ((screenPoint.getX() - px) / scale),
                (float) ((screenPoint.getY() - py) / scale),
                screenPoint.getPressure(), screenPoint.getSize(), screenPoint.getEventTime());
        mCurrentScribble.getPoints().add(normalizedPoint);
    }

    private void finishScribble(int page, final String md5) {
        String key = pageKey(page);
        List<OnyxScribble> list = mScribbleMap.get(key);
        if (list == null) {
            list = new ArrayList<OnyxScribble>();
            mScribbleMap.put(key, list);
        }
        list.add(mCurrentScribble);
        mCurrentScribble.setMD5(md5);
        mCurrentScribble.setPosition(key);
        if (!mPendingScribble.contains(mCurrentScribble)) {
            mPendingScribble.add(mCurrentScribble);
        }
        mCurrentScribble = null;
    }

    private final String pageKey(int page) {
        return String.valueOf(page);
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
        sendToCanvasScribble.addAll(mPendingScribble);
        for (OnyxScribble scribble : sendToCanvasScribble) {
            ArrayList<OnyxScribblePoint> points = scribble.getPoints();
            for (int i = 0; i < points.size() - 1; i++) {
                if (isErasePoint(points.get(i))) {
                    paint.setStrokeWidth(BooxUtil.eraseDefaultWidth);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(0);
                    Xfermode xFermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
                    paint.setXfermode(xFermode);
                    canvas.drawLine(points.get(i).getX(), points.get(i).getY(),
                            points.get(i + 1).getX(), points.get(i + 1).getY(), paint);
                    paint.setStrokeWidth(BooxUtil.penDefaultWidth);
                    paint.setColor(Color.BLACK);
                    paint.setAlpha(255);
                    paint.setXfermode(null);
                } else {
                    paint.setStrokeWidth(points.get(i).getSize());
                    paint.setColor(Color.BLACK);
                    canvas.drawLine(points.get(i).getX(), points.get(i).getY(),
                            points.get(i + 1).getX(), points.get(i + 1).getY(), paint);
                }
            }
        }
    }

    private void updateBrushView() {
        Canvas canvas = mBrushView.getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        try {
            mBrushView.drawBackground(canvas);
            if (mReady && mScribbleBitmap != null && !mScribbleBitmap.isRecycled()) {
                Canvas scribble_canvas = new Canvas(mScribbleBitmap);
                paintScribbles(scribble_canvas, mPaint);
                canvas.drawBitmap(mScribbleBitmap, 0, 0, null);
            }
        } finally {
            mBrushView.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void deletePage(Context context) {
        if (mPendingScribble.isEmpty()) {
            List<OnyxScribble> list = loadScribblesOfPosition(context, mMD5, String.valueOf(mCurrentPage));
            for (OnyxScribble s : list) {
                OnyxCmsCenter.deleteScribble(context, s);
            }
        } else {
            mPendingScribble.clear();
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

    private void setErasePointSize(OnyxScribblePoint point) {
        point.setSize(-1);
    }

    private boolean isErasePoint(OnyxScribblePoint point) {
        return point.getSize() < 0;
    }

    private void updatePointSize(OnyxScribblePoint point, float size, boolean erasing) {
        if (erasing) {
            setErasePointSize(point);
        } else {
            point.setSize(size);
        }
    }

    private boolean isMotionEventInsideView(MotionEvent event) {
        return updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY());
    }

    private void applyFastScribble(MotionEvent event, boolean erasing) {
        float dst[] = mapPoint(event.getX(), event.getY());

        float size;
        OnyxScribblePoint point;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchDownTime = event.getEventTime();
                EpdController.enterScribbleMode(mBrushView);
                mHandler.removeCallbacks(mDelayedLeaveScribbleModeAndUpdateBrushView);

                point = OnyxScribblePoint.fromEvent(event);
                size = EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                        event.getSize(), event.getEventTime());
                updatePointSize(point, size, erasing);
                startScribble(mBrushView.getContext(), mCurrentPage, mPageScale, 0, 0, point);
                break;
            case MotionEvent.ACTION_MOVE:
                // Todo use rawX to fix right boundary still drawing problem.
                if (!isMotionEventInsideView(event)) {
                    if (!mScribbleInterruptedBecauseOutOfRegion) {
                        float last[] = mapPoint(mLastTouchMovePoint.x, mLastTouchMovePoint.y);
                        EpdController.finishStroke(mPaintWidth, last[0], last[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        finishScribble(mCurrentPage, mMD5);
                    }
                    mScribbleInterruptedBecauseOutOfRegion = true;
                    return;
                } else {
                    mLastTouchMovePoint.set(event.getX(), event.getY());
                }

                if (mScribbleInterruptedBecauseOutOfRegion) {
                    mScribbleInterruptedBecauseOutOfRegion = false;
                    point = OnyxScribblePoint.fromEvent(event);
                    size = EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    updatePointSize(point, size, erasing);
                    startScribble(mBrushView.getContext(), mCurrentPage, mPageScale, 0, 0, point);
                    return;
                } else {
                    int n = event.getHistorySize();
                    for (int i = 0; i < n; i++) {
                        point = OnyxScribblePoint.fromHistoricalEvent(event, i);
                        dst = mapPoint(event.getHistoricalX(i), event.getHistoricalY(i));
                        size = EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        updatePointSize(point, size, erasing);
                        addStrokePoints(mPageScale, 0, 0, point, false);
                    }
                    dst = mapPoint(event.getX(), event.getY());
                    point = OnyxScribblePoint.fromEvent(event);
                    size = EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    updatePointSize(point, size, erasing);
                    addStrokePoints(mPageScale, 0, 0, point, false);
                }
                break;
            case MotionEvent.ACTION_UP:
                mLastTouchUpTime = event.getEventTime();
                if (erasing) {
                    // we want to see the result of erasing as soon as possible
                    mHandler.post(mDelayedLeaveScribbleModeAndUpdateBrushView);
                } else {
                    mHandler.postDelayed(mDelayedLeaveScribbleModeAndUpdateBrushView, 500);
                }

                if (!mScribbleInterruptedBecauseOutOfRegion) {
                    EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    finishScribble(mCurrentPage, mMD5);
                }
                if (erasing) {
                    setEdit();
                }
                break;
            default:
                break;
        }
    }

}
