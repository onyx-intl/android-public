
package com.example.pendemo;

import android.content.Context;
import android.graphics.*;

import com.onyx.android.sdk.data.cms.OnyxCmsCenter;
import com.onyx.android.sdk.data.cms.OnyxScribble;
import com.onyx.android.sdk.data.cms.OnyxScribblePoint;
import com.onyx.android.sdk.device.DeviceInfo;
import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.ui.data.ScribbleFactory;

import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;
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
    public static String FAKE_MD5="asdadadasdfrqgewgwe";
    private static final boolean DEBUG = true;

    public static final int TYPE_DEFAULT = 0;// 默认状态
    public static final int TYPE_EDIT = 1;// 编辑状态
    public static final int TYPE_ERASE = 2;// 擦除状态

    private List<PointF> pointList = new ArrayList<PointF>();
    private List<OnyxScribble> pendingScribble = new ArrayList<OnyxScribble>();
    private HashMap<String, List<OnyxScribble>> scribbleMap = new HashMap<String, List<OnyxScribble>>();
    private HashSet<String> scribblePositionsInDb = new HashSet<String>();
    private OnyxScribble currentScribble = null;

    private static BrushManager sInstance;
    private boolean mEnableWakeLock;
    private BrushView mMainView;

    private Matrix mMapMatrix;

    // brush res
    private Path mPath;

    private int mWidth;
    private int mHeight;
    private Bitmap mTempBitmap;

    private Canvas mTmpCanvas;
    private Paint mPaint;

    public void setmPaintWidth(int mPaintWidth) {
        this.mPaintWidth = mPaintWidth;
    }

    private int mPaintWidth;
    private int mPaintColor;

    private int mEditType = TYPE_DEFAULT;
    private PowerManager.WakeLock mLock;
    private boolean continuousWriting;

    // TODO(BinBin) 设置是否画线的标志
    private boolean mWriteFlage = false;

    private Handler mHandler;
    private Context mContext;
    boolean scribbleInterruptedBecauseOutOfRegion=false;
    public int currentPage = new Random().nextInt(1000);
    private int pageCount = 1;
    private float pageScale = 1.0f;
    private Point pageTranslate = new Point();
    Timer timer = new Timer();
    private long uptime=0;
    private long downtime=0;

    public BrushManager(BrushView mainView) {
        super();

        mMainView = mainView;
        mMapMatrix = new Matrix();
        sInstance = this;

        mHandler = new Handler();
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

    public void scribbleProcess(Context context,MotionEvent event) {
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            float dst[] = mapPoint(event.getX(), event.getY());
            OnyxScribblePoint point;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (DEBUG) Log.d(TAG, "--->>>touchDown()");
                    downtime=event.getEventTime();
                    if (uptime!=0){
                        if (downtime-uptime<400){
                            timer.cancel();
                            timer.purge();
                            timer=new Timer();
                        }
                    }
                    point=OnyxScribblePoint.fromEvent(event);
                    EpdController.enterScribbleMode(mMainView);
                    point.setSize(EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime()));
                    startScribble(mContext,currentPage,pageScale,0,0,point);
                    pointList.add(new PointF(event.getX(), event.getY()));
                    break;
                case MotionEvent.ACTION_MOVE:
                    mWriteFlage = true;
                    //Todo use rawX to fix right boundary still drawing problem.
                    if (!updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                        scribbleInterruptedBecauseOutOfRegion = true;
                        return;
                    }
                    if (scribbleInterruptedBecauseOutOfRegion) {
                        scribbleInterruptedBecauseOutOfRegion = false;
                        point=OnyxScribblePoint.fromEvent(event);
                        EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        EpdController.enablePost(mMainView, 0);
                        point.setSize(EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime()));
                        startScribble(mContext,currentPage,pageScale,0,0,point);
                        return;
                    } else {
                        int n = event.getHistorySize();
                        for (int i = 0; i < n; i++) {
                            point=OnyxScribblePoint.fromHistoricalEvent(event,i);
                            dst = mapPoint(event.getHistoricalX(i), event.getHistoricalY(i));
                            point.setSize(EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                    event.getSize(), event.getEventTime()));
                            addStrokePoints(pageScale,0,0,point,false);
                        }
                        dst = mapPoint(event.getX(), event.getY());
                        point=OnyxScribblePoint.fromEvent(event);
                        point.setSize(EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime()));
                        addStrokePoints(pageScale,0,0,point,false);
                    }
                    pointList.add(new PointF(event.getX(), event.getY()));
                    break;
                case MotionEvent.ACTION_UP:
                    if (DEBUG) Log.d(TAG, "--->>>touchUp()");
                    uptime=event.getEventTime();
                    if (mEditType == BrushManager.TYPE_EDIT) {
                        if (updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                            EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                    event.getSize(), event.getEventTime());
                        }
                    }
                    FlushingPostTask flushingPostTask =new FlushingPostTask(context,mMainView);
                    finishScribble(currentPage, FAKE_MD5);
                    timer.schedule(flushingPostTask,400);
                    pointList.add(new PointF(event.getX(), event.getY()));
                    break;
                default:
                    break;
            }
        }
    }


    public void eraseProcess(Context context,MotionEvent event) {
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            float dst[] = mapPoint(event.getX(), event.getY());
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (DEBUG) Log.d(TAG, "--->>>touchDown()");
                    downtime=event.getEventTime();
                    if (uptime!=0){
                        if (downtime-uptime<400){
                            timer.cancel();
                            timer.purge();
                            timer=new Timer();
                        }
                    }
                    EpdController.enterScribbleMode(mMainView);
                    EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                            event.getSize(), event.getEventTime());
                    startScribble(mContext,currentPage,pageScale,0,0,OnyxScribblePoint.fromEvent(event));
                    pointList.add(new PointF(event.getX(), event.getY()));
                    break;
                case MotionEvent.ACTION_MOVE:
                    mWriteFlage = true;
                    //Todo: use rawX to fix right boundary still drawing problem.
                    if (!updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                        scribbleInterruptedBecauseOutOfRegion = true;
                        return;
                    }
                    if (scribbleInterruptedBecauseOutOfRegion) {
                        scribbleInterruptedBecauseOutOfRegion = false;
                        EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        EpdController.enablePost(mMainView, 0);
                        EpdController.startStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        startScribble(mContext,currentPage,pageScale,0,0,OnyxScribblePoint.fromEvent(event));
                        return;
                    } else {
                        int n = event.getHistorySize();
                        for (int i = 0; i < n; i++) {
                            dst = mapPoint(event.getX(), event.getY());
                            EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                    event.getSize(), event.getEventTime());
                            addStrokePoints(pageScale,0,0,OnyxScribblePoint.fromHistoricalEvent(event,i),false);
                        }
                        dst = mapPoint(event.getX(), event.getY());
                        EpdController.addStrokePoint(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                event.getSize(), event.getEventTime());
                        addStrokePoints(pageScale,0,0,OnyxScribblePoint.fromEvent(event),false);
                    }
                    pointList.add(new PointF(event.getX(), event.getY()));
                    break;
                case MotionEvent.ACTION_UP:
                    if (DEBUG) Log.d(TAG, "--->>>touchUp()");
                    uptime=event.getEventTime();
                    if (mEditType == BrushManager.TYPE_EDIT) {
                        if (updateSurfaceViewScribbleRegion().contains((int) event.getRawX(), (int) event.getY())) {
                            EpdController.finishStroke(mPaintWidth, dst[0], dst[1], event.getPressure(),
                                    event.getSize(), event.getEventTime());
                        }
                    }
                    FlushingPostTask flushingPostTask =new FlushingPostTask(context,mMainView);
                    finishScribble(currentPage, FAKE_MD5);
                    setEdit();
                    timer.schedule(flushingPostTask,400);
                    pointList.add(new PointF(event.getX(), event.getY()));
                    break;
                default:
                    break;
            }
        }
    }


    private void redrawPage() {
//        Rect region = renderer.getDirtyRegion();
//        Canvas canvas = .lockCanvas(region);
//        renderer.drawDirtyStroke(canvas, rendererPainter);
//        holder.unlockCanvasAndPost(canvas);
    }

    public void resetPage(final int type, boolean invalidate) {
        if (Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
            mHandler.postDelayed(new Runnable() {
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
        mPaint.setAlpha(255);
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

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void saveScribbles(Context context, String md5) {
        for (OnyxScribble scribble : pendingScribble) {
            scribble.setMD5(md5);
            OnyxCmsCenter.insertScribble(context, scribble);
        }
        pendingScribble.clear();
    }

    public void startScribble(Context context, int page, double scale, int px, int py, OnyxScribblePoint screenPoint) {
        currentScribble = ScribbleFactory.singleton().newScribble(context, page, scale);
        currentScribble.allocatePoints(512);
        OnyxScribblePoint normalizedPoint = new OnyxScribblePoint((float) ((screenPoint.getX() - px) / scale),
                (float) ((screenPoint.getY() - py) / scale),
                screenPoint.getPressure(), screenPoint.getSize(), screenPoint.getEventTime());
        currentScribble.getPoints().add(normalizedPoint);
    }

    public void addStrokePoints(double scale, int px, int py, OnyxScribblePoint screenPoint,boolean touchDown) {
        OnyxScribblePoint normalizedPoint = new OnyxScribblePoint((float) ((screenPoint.getX() - px) / scale),
                (float) ((screenPoint.getY() - py) / scale),
                screenPoint.getPressure(), screenPoint.getSize(), screenPoint.getEventTime());
        currentScribble.getPoints().add(normalizedPoint);
    }

    public void finishScribble(int page, final String md5) {
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

    public void prepareScribbles(Context context, String md5) {
        scribbleMap.clear();
        scribblePositionsInDb.clear();
        if (pendingScribble != null) {
            pendingScribble.clear();
        }
        currentScribble = null;

        OnyxCmsCenter.getScribblePositions(context, context.getPackageName(), md5, scribblePositionsInDb);
    }

    public ArrayList<OnyxScribble> loadScribblesOfPosition(Context context, String md5, String position) {
        ArrayList<OnyxScribble> scribbles = new ArrayList<OnyxScribble>();
        if (OnyxCmsCenter.getScribbles(context, context.getPackageName(),
                md5, position, scribbles)) {
            return scribbles;
        }
        return null;
    }

    public void paintScribbles(Context context,Canvas canvas,Paint paint) {
        ArrayList<OnyxScribble> scribbleList=loadScribblesOfPosition(context,
                BrushManager.FAKE_MD5,
                String.valueOf(BrushManager.getInstance().currentPage));
        for (OnyxScribble scribble :scribbleList){
            ArrayList<OnyxScribblePoint> tempPoints=scribble.getPoints();
            for (int i = 0; i < tempPoints.size() - 1; i++) {
                if (tempPoints.get(i).getSize()==0){
                    paint.setStrokeWidth(20);
                    paint.setColor(Color.WHITE);
                }else {
                    paint.setStrokeWidth(tempPoints.get(i).getSize());
                    paint.setColor(Color.BLACK);
                }canvas.drawLine(tempPoints.get(i).getX(), tempPoints.get(i).getY(),
                        tempPoints.get(i + 1).getX(), tempPoints.get(i + 1).getY(), paint);
            }
        }
    }

    private void flushPendingPost(Context context){
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        Canvas tempCanvas=mMainView.getHolder().lockCanvas();
        if (mMainView.getmBitmapEdit()==null){
            tempCanvas.drawColor(Color.WHITE);
        }else {
            tempCanvas.drawBitmap(mMainView.getmBitmapEdit(),0,0,null);
        }
        BrushManager.getInstance().paintScribbles(context,tempCanvas,paint);
        mMainView.getHolder().unlockCanvasAndPost(tempCanvas);
    }


    public class FlushingPostTask extends TimerTask {
        Context mContext;
        View mView;

        public FlushingPostTask(Context context, View view) {
            mContext = context;
            mView = view;
        }

        public void run() {
            saveScribbles(mContext, FAKE_MD5);
            EpdController.leaveScribbleMode(mView);
            flushPendingPost(mContext);
        }
    }

    public void deletePage(Context context) {
        List<OnyxScribble> list = loadScribblesOfPosition(context, FAKE_MD5, String.valueOf(currentPage));
        for (OnyxScribble s : list) {
            OnyxCmsCenter.deleteScribble(context, s);
        }
    }

    public Rect updateSurfaceViewScribbleRegion() {
        int top = mMainView.getTop();
        int bottom = mMainView.getBottom();
        return new Rect(mMainView.getLeft(), top, mMainView.getRight(), bottom);
    }
}
