
package com.example.pendemo;

import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.device.EpdController.UpdateMode;

import android.app.Activity;
import android.os.Build;
import android.view.View;

public class BooxUtil {

    public static void activityGCUpdate(final Activity activity, final int delay) {
        if (!Build.MODEL.contains(RefreshManager.MODEL_WENSHI)) {
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
