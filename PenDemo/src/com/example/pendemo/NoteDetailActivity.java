
package com.example.pendemo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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

    private static String FAKE_MD5 = "asdadadasdfrqgewgwe";

    BrushView mBrushView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        initView();
        mBrushView.getBrushManager().setEdit();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mBrushView.getBrushManager().setStrokeColor(Color.BLACK);
        mBrushView.getBrushManager().start(FAKE_MD5, true);

        mBrushView.setPathStr("/sdcard/DCIM/snapshot_20140826_150822.png");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mBrushView.getBrushManager().saveNoteBookToStorage();
            finish();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        mBrushView.getBrushManager().finish(true);
        super.onDestroy();
    }

    public void initView() {
        mBrushView = (BrushView) findViewById(R.id.note_detail_brushview);
        Button mPaint = (Button) findViewById(R.id.bt_paint);
        Button mEraser = (Button) findViewById(R.id.bt_eraser);
        Button clearAll = (Button) findViewById(R.id.bt_clear_all);
        TextView mHomeworkName = (TextView) findViewById(R.id.homeWorkRbn);
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
                mBrushView.getBrushManager().saveNoteBookToStorage();
                break;
            // 笔
            case R.id.bt_paint:
                mBrushView.getBrushManager().setEdit();
                break;
            // 橡皮
            case R.id.bt_eraser:
                mBrushView.getBrushManager().setEraser();
                break;
            case R.id.bt_clear_all:
                mBrushView.getBrushManager().clear();
                break;
            case R.id.bt_save:
                mBrushView.getBrushManager().saveNoteBookToStorage();
                break;
            case R.id.bt_quit:
                mBrushView.getBrushManager().finish(false);
                finish();
                break;
            case R.id.bt_line_3:
                mBrushView.getBrushManager().setStrokeWidth(3);
                break;
            case R.id.bt_line_5:
                mBrushView.getBrushManager().setStrokeWidth(5);
                break;
            case R.id.bt_line_7:
                mBrushView.getBrushManager().setStrokeWidth(7);
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
            mBrushView.getBrushManager().setEdit();
            return true;
        }
        // 擦除
        else if (keyCode == KeyEvent.KEYCODE_CLEAR) {
            mBrushView.getBrushManager().setEraser();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_POWER) {
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            mBrushView.getBrushManager().saveNoteBookToStorage();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
