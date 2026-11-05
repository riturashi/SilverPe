package com.silverpe.utils;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class StatusBarUtils {

    public static void setupStatusBar(
            android.app.Activity activity,
            int headerId
    ) {
        Window window = activity.getWindow();

        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window, window.getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);

        View header = activity.findViewById(headerId);

        if (header == null) return;

        int originalHeight = header.getLayoutParams().height;
        int left = header.getPaddingLeft();
        int right = header.getPaddingRight();
        int bottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
            int topInset = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
            ).top;

            view.setPadding(left, topInset, right, bottom);

            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = originalHeight + topInset;
            view.setLayoutParams(params);

            return insets;
        });

        ViewCompat.requestApplyInsets(header);
    }
}