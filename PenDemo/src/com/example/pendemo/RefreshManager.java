
package com.example.pendemo;

import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.device.EpdController.UpdateMode;

import android.app.Activity;
import android.os.Build;
import android.view.View;

public class RefreshManager {
    public static final String MODEL_WENSHI = "E970B";

    // for activity refresh
    public static void onCreate(Activity activity) {
        if (Build.MODEL.contains(MODEL_WENSHI)) {
            // EpdController.invalidate(activity.getWindow().getDecorView(),
            // UpdateMode.GC);
            EpdController.invalidate(activity.getWindow().getDecorView(), UpdateMode.GU);
        }
    }

    // for activity refresh
    public static void onPostResume(Activity activity) {
        if (Build.MODEL.contains(MODEL_WENSHI)) {
            BooxUtil.activityGCUpdate(activity, BooxUtil.defaultGCDelay());
        }
    }

    public static void invalidateGC4(View view, boolean isSet) {
        if (Build.MODEL.contains(MODEL_WENSHI)) {
            if (isSet) {
                EpdController.setViewDefaultUpdateMode(view, UpdateMode.GC4);
            } else {
                EpdController.invalidate(view, UpdateMode.GC4);
            }
        } else {
            view.invalidate();
        }
    }

    public static void invalidateGUFAST(View view, boolean isSet) {
        if (Build.MODEL.contains(MODEL_WENSHI)) {
            if (isSet) {
                EpdController.setViewDefaultUpdateMode(view, UpdateMode.GU_FAST);
            } else {
                EpdController.invalidate(view, UpdateMode.GU_FAST);
            }
        } else {
            view.invalidate();
        }
    }

    public static void invalidateGC(View view, boolean isSet) {
        if (Build.MODEL.contains(MODEL_WENSHI)) {
            if (isSet) {
                // EpdController.setViewDefaultUpdateMode(view, UpdateMode.GC);
                EpdController.setViewDefaultUpdateMode(view, UpdateMode.GU);
            } else {
                // EpdController.invalidate(view, UpdateMode.GC);
                EpdController.invalidate(view, UpdateMode.GU);
            }
        } else {
            view.invalidate();
        }
    }

    public static void postInvalidateGC4(View view) {
        if (Build.MODEL.contains(MODEL_WENSHI)) {
            EpdController.postInvalidate(view, UpdateMode.GC4);
        } else {
            view.postInvalidate();
        }
    }
}
