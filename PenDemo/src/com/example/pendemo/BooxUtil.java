
package com.example.pendemo;

import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.device.EpdController.UpdateMode;

import android.app.Activity;
import android.os.Build;
import android.view.View;

public class BooxUtil {

    public static int penDefaultWidth=3;
    public static int eraseDefaultWidth=20;

    private static final String MODEL_WENSHI = "E970B";

    public static boolean isE970B() {
        return Build.MODEL.contains(MODEL_WENSHI);
    }

    public static void activityGCUpdate(final Activity activity, final int delay) {
        if (!isE970B()) {
            return;
        }
        final View view = activity.getWindow().getDecorView();
        // EpdController.enableScreenUpdate(view, false);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                // EpdController.enableScreenUpdate(activity.getWindow().getDecorView(),
                // true);

                // EpdController.invalidate(view, UpdateMode.GC);
                EpdController.invalidate(view, UpdateMode.GU);
            }
        }, delay);
    }

    public static int defaultGCDelay() {
        return 300;
    }
}
