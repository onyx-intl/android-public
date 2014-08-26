
package com.example.pendemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ListView;

public class BrushView extends SurfaceView {
    private static final String TAG = BrushView.class.getSimpleName();

    private int mWidth;
    private int mHeight;

    private ListView parentView;
    private Bitmap mBackgroundBitmap = null; // 临时画布中的临时图片
    private String mPathStr;

    private BrushManager mBrushManager;

    private Bitmap mNewBitmap;
    private String mTeachMark;

    public BrushManager getBrushManager() {
        return mBrushManager;
    }

    public String getPathStr() {
        return mPathStr;
    }

    public void setPathStr(String pathStr) {
        this.mPathStr = pathStr;
        if (!TextUtils.isEmpty(mPathStr)) {
            Bitmap old = mBackgroundBitmap;
            mBackgroundBitmap = BitmapFactory.decodeFile(pathStr);
            if (old != null && !old.isRecycled()) {
                old.recycle();
            }
        }
    }

    public Bitmap getBackgroundBitmap() {
        return mBackgroundBitmap;
    }

    public void setParentView(ListView parentView) {
        this.parentView = parentView;
    }

    public static Bitmap getscaleBitmap(Bitmap srcBitmap, int w, int h) {
        Bitmap bitmapNew;

        float bitmapWidth = w;
        float bitmapHeight = h;

        Matrix matrix = new Matrix();
        bitmapNew = Bitmap.createBitmap(srcBitmap, 0, 0, (int) bitmapWidth, (int) bitmapHeight,
                matrix, true).copy(Bitmap.Config.ARGB_8888, true);
        return bitmapNew;
    }

    public Bitmap getImgBitmap() {
        Bitmap temp = getDrawingCache();
        if (temp != null) {

            try {
                mNewBitmap = Bitmap.createBitmap(temp);
            } catch (Error OutOfMemoryError) {
                System.gc();
                mNewBitmap = Bitmap.createBitmap(temp);
            }
        }

        return mNewBitmap;
    }

    public Bitmap getCurrentBitmap() {
        return mBrushManager.getScribbleBitmap();
    }

    public Bitmap HandWriting() {
        return mBackgroundBitmap;
    }

    public String getmTeachMark() {
        return mTeachMark;
    }

    public void setmTeachMark(String teachMark) {
        mTeachMark = teachMark;
    }

    public void clearBitmapEdit() {
        mBackgroundBitmap = null;
    }

    public BrushView(Context context) {
        super(context);
        initialize();
    }

    public BrushView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public BrushView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        mBrushManager = new BrushManager(this, BooxUtil.penDefaultWidth);

        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas canvas = holder.lockCanvas();
                drawBackground(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Canvas canvas = holder.lockCanvas();
                drawBackground(canvas);
                holder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "onSizeChanged: " + this.getWidth() + ", " + this.getHeight());
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mBrushManager.onSizeChanged(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBackgroundBitmap == null) {
            mBackgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        }
        canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
        mBrushManager.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: " + event);
        // ignore multi touch
        if (event.getPointerCount() > 1) {
            return false;
        }
        setParentScrollAble(false);
        return mBrushManager.onTouchEvent(event);
    }

    /**
     * 是否把滚动事件交给父scrollview
     *
     * @param flag
     */
    private void setParentScrollAble(boolean flag) {
        if (null != parentView) {
            parentView.requestDisallowInterceptTouchEvent(!flag);
        }

    }

    public void drawBackground(Canvas canvas) {
        if (mBackgroundBitmap == null) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
        }
    }

}
