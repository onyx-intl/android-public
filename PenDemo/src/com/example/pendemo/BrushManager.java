
package com.example.pendemo;

import android.graphics.*;
import com.onyx.android.sdk.device.DeviceInfo;
import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.ui.data.ScribbleFactory;

import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Control brush line
 * 
 * @author shlf
 * @DATE 2014-07-11
 */
public class BrushManager {
    private static final String TAG = "SHLF-BrushManager";
    private static final boolean DEBUG = true;

    public static final int TYPE_DEFAULT = 0;// 默认状态
    public static final int TYPE_EDIT = 1;// 编辑状态
    public static final int TYPE_ERASE = 2;// 擦除状态

    private List<PointF> pointList = new ArrayList<PointF>();

    private static BrushManager sInstance;
    private boolean mEnableWakeLock;
    private View mMainView;

    private Matrix mMapMatrix;

    // brush res
    private Path mPath;
    private float mPathX;
    private float mPathY;

    private int mWidth;
    private int mHeight;
    private Bitmap mTempBitmap;

    private Canvas mTmpCanvas;
    private Paint mPaint;
    private int mPaintWidth;
    private int mPaintColor;

    private int mEditType = TYPE_DEFAULT;
    private PowerManager.WakeLock mLock;

    // TODO(BinBin) 设置是否画线的标志
    private boolean mWriteFlage = false;

    private Handler mHander;
    private NoteDetailActivity mActivity;
    boolean scribbleInterruptedBecauseOutOfRegion=false;

    public BrushManager(View mainView) {
        super();

        mMainView = mainView;
        mMapMatrix = new Matrix();
        sInstance = this;

        mHander = new Handler();
    }

    public static BrushManager getInstance() {
        return sInstance;
    }

    // init brush
    public void initBrush(int defaultPaintWidth, int defaultPaintColor, boolean enableWakeLock) {
        mMainView.setDrawingCacheEnabled(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mPaintColor = defaultPaintColor;
        mPaint.setColor(mPaintColor);

        mPath = new Path();

        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            mPaintWidth = defaultPaintWidth;
            // mPaintWidth =
            // ScribbleFactory.singleton().getThickness(mMainView.getContext());

            DeviceInfo.currentDevice.enterScribbleMode(mMainView);

            mPaint.setStrokeWidth(mPaintWidth);

            mMapMatrix.postRotate(270);
            mMapMatrix.postTranslate(0, 825);

            mEnableWakeLock = enableWakeLock;
            if (mEnableWakeLock) {
                mLock = DeviceInfo.currentDevice.newWakeLock(mMainView.getContext(),
                        "Sark Scribble");
                mLock.acquire();
            }
        } else {
            mPaintWidth = defaultPaintWidth;
            mPaint.setStrokeWidth(mPaintWidth);
        }
    }

    public void releaseWakeLock(boolean leave) {
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            if (mEnableWakeLock && mLock != null) {
                mLock.release();
                mLock = null;
            }

            if (leave) DeviceInfo.currentDevice.leaveScribbleMode(mMainView);
        }
    }

    public void leaveScribbleMode() {
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            DeviceInfo.currentDevice.leaveScribbleMode(mMainView);
        }
    }

    public Bitmap getBitmap() {
        return mTempBitmap;
    }

    // TODO(BinBin)由路径获得图片
    public void setBitmap(String path) {
        if (!TextUtils.isEmpty(path) && (new File(path)).exists()) {
            try {
                if (null != BitmapFactory.decodeFile(path)) {
                    mTempBitmap = BitmapFactory.decodeFile(path)
                            .copy(Bitmap.Config.ARGB_4444, true);
                }
            } catch (Error OutOfMemoryError) {
                System.gc();
                System.gc();
                if (null != BitmapFactory.decodeFile(path)) {
                    mTempBitmap = BitmapFactory.decodeFile(path)
                            .copy(Bitmap.Config.ARGB_4444, true);
                }
            }

        }
    }

    // TODO(BinBin) 获取图片的是否被画
    public boolean getWriteFlage() {
        return mWriteFlage;
    }

    public void setWriteFlage(boolean writeFlage) {
        mWriteFlage = writeFlage;
    }

    public void setPaintWidth(int width) {
        mPaintWidth = width;
    }

    public void setPaintColor(int color) {
        mPaintColor = color;
    }

    public void onSizeChanged(int w, int h) {
        if (DEBUG) Log.d(TAG, "--->>>onSizeChanged()");

        mWidth = w;
        mHeight = h;

        try {
            mTempBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        } catch (Error OutOfMemoryError) {
            System.gc();
            mTempBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        }

        if (mTempBitmap != null) {
            mTmpCanvas = new Canvas(mTempBitmap);
        } else {
            Log.e(TAG, "on Sizechangeed Create bitmap error");
        }

    }

    public void onDraw(final Canvas canvas) {
        if (canvas != null && mTempBitmap != null) canvas.drawBitmap(mTempBitmap, 0, 0, null);
    }

    public float[] mapPoint(float x, float y) {
        int viewLocation[] = {
                0, 0
        };
        mMainView.getLocationOnScreen(viewLocation);
        float screenPoints[] = {
                viewLocation[0] + x, viewLocation[1] + y
        };
        float dst[] = {
                0, 0
        };
        mMapMatrix.mapPoints(dst, screenPoints);
        return dst;
    }

    public void touchDown(MotionEvent event) {
        if (DEBUG) Log.d(TAG, "--->>>touchDown()");
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            float dst[] = mapPoint(event.getX(), event.getY());
            if (mEditType == BrushManager.TYPE_EDIT) {
                EpdController.enablePost(mMainView, 0);

                EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                        event.getSize(), event.getEventTime());

            }
        }
        pointList.add(new PointF(event.getX(), event.getY()));
        /*
        // draw in repaint, don't draw here. collect these points.
        mPath.reset();
        mPath.moveTo(event.getX(), event.getY());

        mPathX = event.getX();
        mPathY = event.getY();

        mTmpCanvas.drawPath(mPath, mPaint);

        mMainView.setDrawingCacheEnabled(true);
        */
    }

    public void touchMove(MotionEvent event) {
        mWriteFlage = true;
        //Todo use rawX to fix right boundary still drawing problem.
        if (!mActivity.updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
            scribbleInterruptedBecauseOutOfRegion = true;
            return;
        }
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            float dst[] = mapPoint(event.getX(), event.getY());
            if (mEditType == BrushManager.TYPE_EDIT) {
                if (scribbleInterruptedBecauseOutOfRegion) {
                    scribbleInterruptedBecauseOutOfRegion=false;
                    EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    EpdController.enablePost(mMainView, 0);
                    EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    return;
                }else {
                    int n = event.getHistorySize();
                    for (int i = 0; i < n; i++) {
                        EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                    }
                    EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                }
            }
        }
        pointList.add(new PointF(event.getX(), event.getY()));
        /*
        mPath.quadTo(mPathX, mPathY, (event.getX() + mPathX) / 2, (event.getY() + mPathY) / 2);
        mPathX = event.getX();
        mPathY = event.getY();

        mTmpCanvas.drawPath(mPath, mPaint);

        if (mEditType == BrushManager.TYPE_ERASE) {
            mMainView.invalidate();
        }
        */
    }

    public void touchUp(MotionEvent event) {
        if (DEBUG) Log.d(TAG, "--->>>touchUp()");
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            float dst[] = mapPoint(event.getX(), event.getY());
            if (mEditType == BrushManager.TYPE_EDIT) {
                if (mActivity.updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                    EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                }
            }
        }
        pointList.add(new PointF(event.getX(), event.getY()));
        /*
        mPath.lineTo(event.getX(), event.getY());
        mPath.reset();
        */
    }

    private void redrawPage() {

    }

    public void resetPage(final int type, boolean invalidate) {
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            mHander.postDelayed(new Runnable() {
                public void run() {
                    EpdController.enablePost(mMainView, type);
                }
            }, ((0 == type) ? 2000 : 0));
        }
        if (invalidate) {
            if (null != mMainView) mMainView.invalidate();
        }
    }

    public void setEdit() {
        if (DEBUG) Log.d(TAG, "--->>>setEdit()");

        mEditType = TYPE_EDIT;

        mPaint.setColor(mPaintColor);
        mPaint.setStrokeWidth(mPaintWidth);

        mPaint.setXfermode(null);
        mPaint.setAlpha(255);
    }

    public void setEraser() {
        if (DEBUG) Log.d(TAG, "--->>>setEraser()");

        mEditType = TYPE_ERASE;
        mPaint.setAlpha(0);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mPaint.setStrokeWidth(mPaintWidth);
    }

    /**
     * clear tmpCanvas.
     */
    public void clear() {
        if (DEBUG) Log.d(TAG, "--->>>clear()");
        EpdController.enablePost(mMainView, 1);
        try {
            mTempBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        } catch (Error OutOfMemoryError) {
            System.gc();
            mTempBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        }
        mTmpCanvas = new Canvas(mTempBitmap);
        RefreshManager.invalidateGC4(mMainView, false);
    }

    public void handWriting() {
        Canvas canvas = new Canvas(mTempBitmap);
        canvas.drawPath(mPath, mPaint);
    }

    public void setHostActivity(NoteDetailActivity mActivity) {
        this.mActivity = mActivity;
    }
}
