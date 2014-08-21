
package com.example.pendemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class BrushView extends View {
    private static final String TAG = BrushView.class.getSimpleName();

    private int mEditType = BrushManager.TYPE_DEFAULT;
    private int mWidth;
    private int mHeight;

    private Bitmap mBitmapEdit; // 临时画布中的临时图片
    private Bitmap mNewBitma;

    private String mPathStr;

    private BrushManager mBrushManager;

    private String mTeachMark;

    private ListView parentView;

    public String getPathStr() {
        return mPathStr;
    }

    public void setPathStr(String pathStr) {
        this.mPathStr = pathStr;
        if (!TextUtils.isEmpty(mPathStr)) {
            mBitmapEdit = BitmapFactory.decodeFile(mPathStr);
        }
        // mBrushManager.setBitmap(pathStr);
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

    public void initialize() {
        mBrushManager = new BrushManager(this);
        mBrushManager.initBrush(3, Color.BLACK, true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mBrushManager.onSizeChanged(w, h);
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

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmapEdit == null) {
            mBitmapEdit = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
        }
        canvas.drawBitmap(mBitmapEdit, 0, 0, null);

        mBrushManager.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "view size: " + this.getWidth() + ", " + this.getHeight() + ", event: " + event);
        // ignore multi touch
        if (event.getPointerCount() > 1) {
            return false;
        }

        setParentScrollAble(false);

        if (mEditType != BrushManager.TYPE_DEFAULT) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    distance(event);
                    break;
                case MotionEvent.ACTION_DOWN:
                    if ((BrushManager.TYPE_EDIT == mEditType)
                            || (BrushManager.TYPE_ERASE == mEditType)) {
                        mBrushManager.touchDown(event);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ((BrushManager.TYPE_EDIT == mEditType)
                            || (BrushManager.TYPE_ERASE == mEditType)) {
                        if (event.getX() > getLeft() && event.getY() > getTop()) {
                            mBrushManager.touchMove(event);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (BrushManager.TYPE_EDIT == mEditType) {
                        if (event.getX() > getLeft() && event.getY() > getTop()) {
                            mBrushManager.touchUp(event);
                        }
                        mEditType = BrushManager.TYPE_EDIT;
                        mBrushManager.setEdit();
                    }
                    mNewBitma = this.getCurrentBitmap();
                    break;
            }
            return true;
        }
        return false;
    }

    private float distance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    public void setEdit() {
        mEditType = BrushManager.TYPE_EDIT;
        mBrushManager.setEdit();
    }

    public void setEraser() {
        mEditType = BrushManager.TYPE_ERASE;
        mBrushManager.setEraser();
    }

    /**
     * 清除整个图像
     */
    public void clear() {
        mEditType = BrushManager.TYPE_DEFAULT;
        mBrushManager.clear();
        mNewBitma = mBitmapEdit;
    }

    public void cancelEdit() {
        mEditType = BrushManager.TYPE_DEFAULT;
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
        // if (mNewBitma != null && !mNewBitma.isRecycled()) {
        // // mNewBitma.recycle();
        // System.gc();
        // }
        Bitmap temp = getDrawingCache();
        if (temp != null) {

            try {
                mNewBitma = Bitmap.createBitmap(temp);
            } catch (Error OutOfMemoryError) {
                System.gc();
                mNewBitma = Bitmap.createBitmap(temp);
            }

            // mNewBitma = Bitmap.createBitmap(temp);
            // if (!temp.isRecycled()) {
            // temp.recycle();
            // temp = null;
            // System.gc();
            // }
        }

        return mNewBitma;
    }

    public Bitmap getCurrentBitmap() {
        return mBrushManager.getBitmap();
    }

    public Bitmap HandWriting() {
        mBrushManager.handWriting();

        return mBitmapEdit;
    }

    public String getmTeachMark() {
        return mTeachMark;
    }

    public void setmTeachMark(String teachMark) {
        mTeachMark = teachMark;
    }

    public void clearBitmapEdit() {
        mBitmapEdit = null;
    }

    public void setParentView(ListView parentView) {
        this.parentView = parentView;
    }

}
