
package com.example.pendemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        initView();
        mBrushView.setEdit();

        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
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
        switch (v.getId()) {

            case R.id.bt_commit:
                BrushManager.getInstance().resetPage(1, true);
                saveNoteBook();
                break;

            // 笔
            case R.id.bt_paint:
                mBrushView.setEdit();
                BrushManager.getInstance().resetPage(0, false);
                break;

            // 橡皮
            case R.id.bt_eraser:
                mBrushView.setEraser();
                BrushManager.getInstance().resetPage(1, true);
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
            return true;
        }
        // 擦除
        else if (keyCode == KeyEvent.KEYCODE_CLEAR) {
            mBrushView.setEraser();
            BrushManager.getInstance().resetPage(1, true);
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
        Bitmap bitmap = mBrushView.getImgBitmap();
        return false;
    }
}
