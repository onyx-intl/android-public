
package com.example.pendemo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.onyx.android.sdk.data.cms.OnyxScribble;
import com.onyx.android.sdk.data.cms.OnyxScribblePoint;
import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.ui.data.ScribbleFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @Class Name : NoteDetailActivity
 * @Description: 笔记本详情页面
 * @Author jiege@ceehoo.cn
 * @Date 2014年6月17日 下午8:13:29
 */
public class NoteDetailActivity extends Activity implements OnClickListener {
    private static final String TAG = NoteDetailActivity.class.getSimpleName();

    private BrushView mBrushView;
    private TextView mHomewrokName;
    private Button mCommit;
    private Button mLeaveMsg;

    private Button mPaint;
    private Button mEraser;
    private Rect surfaceViewScribbleRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        initView();
        mBrushView.setEdit();
        mBrushView.setDrawingCacheEnabled(true);
        mBrushView.buildDrawingCache(true);
        BrushManager.getInstance().setHostActivity(this);
        ActionBar actionBar=getActionBar();
        if (actionBar!=null){
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mBrushView.surfaceViewScribbleRegion=updateSurfaceViewScribbleRegion();
        EpdController.setStrokeStyle(Color.BLACK);
        BrushManager.getInstance().prepareScribbles(this,BrushManager.FAKE_MD5);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        BrushManager.getInstance().resetPage(1, false);

        if (item.getItemId() == android.R.id.home) {
            saveNoteBook();
            finish();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        BrushManager.getInstance().saveScribbles(this,BrushManager.FAKE_MD5);
        super.onDestroy();
        BrushManager.getInstance().releaseWakeLock(true);
    }

    public void initView() {
        mPaint = (Button) findViewById(R.id.bt_paint);
        mEraser = (Button) findViewById(R.id.bt_eraser);
        Button clearAll = (Button) findViewById(R.id.bt_clear_all);
        mHomewrokName = (TextView) findViewById(R.id.homeWorkRbn);
        mBrushView = (BrushView) findViewById(R.id.note_detail_brushview);
        mCommit = (Button) findViewById(R.id.bt_commit);
        mLeaveMsg = (Button) findViewById(R.id.bt_leavemsg);
        mCommit.setOnClickListener(this);
        mLeaveMsg.setOnClickListener(this);
        clearAll.setOnClickListener(this);

        mPaint.setOnClickListener(this);
        mEraser.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        flushPendingPost();
        switch (v.getId()) {
            case R.id.bt_commit:
                BrushManager.getInstance().resetPage(1, true);
                saveNoteBook();
                break;
            // 笔
            case R.id.bt_paint:
                mBrushView.setEdit();
                EpdController.setStrokeStyle(Color.BLACK);
                BrushManager.getInstance().setmPaintWidth(3);
                BrushManager.getInstance().resetPage(0, false);
                break;
            // 橡皮
            case R.id.bt_eraser:
                mBrushView.setEraser();
                EpdController.setStrokeStyle(Color.WHITE);
                BrushManager.getInstance().setmPaintWidth(20);
                BrushManager.getInstance().resetPage(0, false);
                break;
            case R.id.bt_clear_all:
                BrushManager.getInstance().clear();
                mBrushView.cancelEdit();
                mBrushView.clearBitmapEdit();
                mBrushView.setEdit();
                break;
            case R.id.bt_save:
                saveNoteBook();
                BrushManager.getInstance().resetPage(1, true);
                break;
            case R.id.bt_quit:
                BrushManager.getInstance().resetPage(1, false);
                saveNoteBook();
                finish();
                break;
            case R.id.bt_line_3:
                BrushManager.getInstance().setmPaintWidth(3);
                break;
            case R.id.bt_line_5:
                BrushManager.getInstance().setmPaintWidth(5);
                break;
            case R.id.bt_line_7:
                BrushManager.getInstance().setmPaintWidth(7);
                break;
            case R.id.bt_test_canvas_redraw:
                startActivity(new Intent(NoteDetailActivity.this,testActivity.class));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestcode, int resultcode, Intent data) {
        super.onActivityResult(requestcode, resultcode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 编辑
        if (keyCode == KeyEvent.KEYCODE_BUTTON_START) {
            mBrushView.setEdit();
            BrushManager.getInstance().setmPaintWidth(3);
            EpdController.setStrokeStyle(Color.BLACK);
            return true;
        }
        // 擦除
        else if (keyCode == KeyEvent.KEYCODE_CLEAR) {
            mBrushView.setEraser();
            BrushManager.getInstance().setmPaintWidth(20);
            EpdController.setStrokeStyle(Color.WHITE);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_POWER) {
            BrushManager.getInstance().resetPage(1, false);
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            BrushManager.getInstance().resetPage(1, true);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            BrushManager.getInstance().resetPage(1, false);
            saveNoteBook();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 保存笔记本
     * 
     * @return
     */
    private boolean saveNoteBook() {
        BrushManager.getInstance().saveScribbles(this,BrushManager.FAKE_MD5);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        final Bitmap baseBitmap = Bitmap.createBitmap(825,1200, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas=new Canvas(baseBitmap);
        tempCanvas.drawColor(Color.WHITE);
        BrushManager.getInstance().paintScribbles(NoteDetailActivity.this,tempCanvas,paint);
        File saveFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/test"+BrushManager.getInstance().currentPage+ ".png");
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
        return false;
    }

    public Rect updateSurfaceViewScribbleRegion() {
        int top = mBrushView.getTop();
        int bottom = mBrushView.getBottom();
        return new Rect(mBrushView.getLeft(), top, mBrushView.getRight(), bottom);
    }

    private boolean execCommand(String cmd) {
        try {
            Log.d(TAG, "exec: " + cmd);
            Process proc = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            int n;
            while((n = in.read()) != -1) {
                System.out.write(n);
            }
            Log.d(TAG, "Done reading stdout");
            in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            while((n = in.read()) != -1) {
                System.out.write(n);
            }
            Log.d(TAG, "Done reading stderr");
            int res = proc.waitFor();
            Log.d(TAG, "command result: " + res);
            return res == 0;
        } catch (Throwable tr) {
            Log.w(TAG, tr);
            return false;
        }
    }

    private void flushPendingPost(){
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        Canvas tempCanvas=mBrushView.getHolder().lockCanvas();
        tempCanvas.drawColor(Color.WHITE);
        BrushManager.getInstance().paintScribbles(NoteDetailActivity.this,tempCanvas,paint);
        mBrushView.getHolder().unlockCanvasAndPost(tempCanvas);
    }
}
