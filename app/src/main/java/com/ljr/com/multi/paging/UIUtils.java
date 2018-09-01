package com.ljr.com.multi.paging;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class UIUtils {
    private static Display sDisplay;
    private static DisplayMetrics sMetrics;

    // Note: this init function may be called several times (for each process).
    private static synchronized void lazyInit(Context context) {
        if (sDisplay == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            sDisplay = wm.getDefaultDisplay();
            sMetrics = new DisplayMetrics();
            sDisplay.getMetrics(sMetrics);
        }
    }

    public static float dpToPixels(float dp, Context context) {
        lazyInit(context);
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, sMetrics);
    }

    public static boolean isLayoutRtl(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL) {
                return true;
            }
        }
        return false;
    }
}
