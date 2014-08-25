
package com.example.pendemo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Class Name : NoteDetailActivity
 * @Description: 笔记本详情页面
 * @Author jiege@ceehoo.cn
 * @Date 2014年6月17日 下午8:13:29
 */
public class NoteDetailActivity extends Activity implements OnClickListener {
    private static final String TAG = NoteDetailActivity.class.getSimpleName();

    private BrushView mBrushView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        initView();
        mBrushView.setEdit();
        mBrushView.setDrawingCacheEnabled(true);
        mBrushView.buildDrawingCache(true);
        BrushManager.getInstance().setContext(this);
        ActionBar actionBar=getActionBar();
        if (actionBar!=null){
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mBrushView.setStrokeColor(Color.BLACK);
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
        Button mPaint = (Button) findViewById(R.id.bt_paint);
        Button mEraser = (Button) findViewById(R.id.bt_eraser);
        Button clearAll = (Button) findViewById(R.id.bt_clear_all);
        TextView mHomeworkName = (TextView) findViewById(R.id.homeWorkRbn);
        mBrushView = (BrushView) findViewById(R.id.note_detail_brushview);
        Button mCommit = (Button) findViewById(R.id.bt_commit);
        Button mLeaveMsg = (Button) findViewById(R.id.bt_leavemsg);
        mCommit.setOnClickListener(this);
        mLeaveMsg.setOnClickListener(this);
        clearAll.setOnClickListener(this);

        mPaint.setOnClickListener(this);
        mEraser.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_commit:
                BrushManager.getInstance().resetPage(1, true);
                saveNoteBook();
                break;
            // 笔
            case R.id.bt_paint:
                mBrushView.setEdit();
                mBrushView.setStrokeColor(Color.BLACK);
                BrushManager.getInstance().setStrokeWidth(BooxUtil.penDefaultWidth);
                BrushManager.getInstance().resetPage(0, false);
                break;
            // 橡皮
            case R.id.bt_eraser:
                mBrushView.setEraser();
                mBrushView.setStrokeColor(Color.WHITE);
                BrushManager.getInstance().setStrokeWidth(BooxUtil.eraseDefaultWidth);
                BrushManager.getInstance().resetPage(0, false);
                break;
            case R.id.bt_clear_all:
                BrushManager.getInstance().deletePage(NoteDetailActivity.this);
                BrushManager.getInstance().clear();
                mBrushView.cancelEdit();
                mBrushView.setEdit();
                Canvas canvas=mBrushView.getHolder().lockCanvas();
                if (mBrushView.getmBitmapEdit()==null){
                    canvas.drawColor(Color.WHITE);
                }else {
                    canvas.drawBitmap(mBrushView.getmBitmapEdit(),0,0,null);
                }
                mBrushView.getHolder().unlockCanvasAndPost(canvas);
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
                BrushManager.getInstance().setStrokeWidth(3);
                break;
            case R.id.bt_line_5:
                BrushManager.getInstance().setStrokeWidth(5);
                break;
            case R.id.bt_line_7:
                BrushManager.getInstance().setStrokeWidth(7);
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
            BrushManager.getInstance().setStrokeWidth(BooxUtil.penDefaultWidth);
            mBrushView.setStrokeColor(Color.BLACK);
            return true;
        }
        // 擦除
        else if (keyCode == KeyEvent.KEYCODE_CLEAR) {
            mBrushView.setEraser();
            BrushManager.getInstance().setStrokeWidth(BooxUtil.eraseDefaultWidth);
            mBrushView.setStrokeColor(Color.WHITE);
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
        BrushManager.getInstance().paintScribbles(tempCanvas, paint);
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

}
