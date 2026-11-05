package com.silverpe.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import com.silverpe.R;

public class UiUtils {

    private static AlertDialog progressDialog;

    /**
     * ✅ Custom Fintech Toast
     * @param context Activity context
     * @param message Message to display
     * @param success true for Green/Success, false for Red/Error
     */
    public static void showToast(Context context, String message, boolean success) {

        // FIX: context null ya Activity destroy ho chuki ho to Toast bhi
        // skip kar do - warna yaha bhi crash aa sakta hai.
        if (context == null) return;

        try {

            // Custom Layout inflate karein
            View layout = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null);

            // Views find karein
            View container = layout.findViewById(R.id.toast_container);
            TextView tvIcon = layout.findViewById(R.id.tv_toast_icon);
            TextView tvMessage = layout.findViewById(R.id.tv_toast_message);

            // Message set karein
            tvMessage.setText(message);

            if (success) {
                // Success Style (Green)
                container.setBackgroundResource(R.drawable.bg_toast_success);
                tvIcon.setText("✓");
                tvIcon.setTextColor(Color.parseColor("#2E7D32")); // Dark Green Text
            } else {
                // Error Style (Red)
                container.setBackgroundResource(R.drawable.bg_toast_error);
                tvIcon.setText("!");
                tvIcon.setTextColor(Color.parseColor("#C62828")); // Dark Red Text
            }

            // Toast setup
            Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();

        } catch (Exception ignored) {
            // Context destroyed ya inflate fail - silently ignore, crash mat hone do
        }
    }

    /**
     * 🔄 Advanced Common Loader (Please Wait...)
     * Isme background dim hoga aur box ke andar right side mein rotation chalega.
     */
    public static void showLoading(Context context) {

        // FIX: context null hone par ya Activity already finishing/destroyed
        // hone par dialog banane ki koshish hi mat karo.
        if (context == null) return;

        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        // FIX: agar purana dialog kisi wajah se abhi bhi reference me hai
        // (dusri Activity se), pehle usko safely dismiss/null karo taaki
        // static progressDialog stale window ko point na kare.
        dismissLoading();

        try {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            View view = LayoutInflater.from(context).inflate(R.layout.layout_common_loader, null);
            builder.setView(view);
            builder.setCancelable(false);

            progressDialog = builder.create();

            if (progressDialog.getWindow() != null) {

                // Transparent card background
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // ✅ FULL SCREEN DIM (perfect fintech feel)
                progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                progressDialog.getWindow().setDimAmount(0.5f); // 0.3 light, 0.5 best, 0.7 dark
            }

            progressDialog.show();

            // DOTS
            View dot1 = view.findViewById(R.id.dot1);
            View dot2 = view.findViewById(R.id.dot2);
            View dot3 = view.findViewById(R.id.dot3);

            Handler handler = new Handler(Looper.getMainLooper());

            Runnable runnable = new Runnable() {
                int step = 0;

                @Override
                public void run() {

                    // FIX: dialog already dismiss ho chuka ho to animation
                    // loop yaha ruk jaye - warna yeh Handler hamesha chalta
                    // rahega aur dismiss ho chuke dialog ki views touch karega.
                    if (progressDialog == null || !progressDialog.isShowing()) {
                        return;
                    }

                    dot1.setAlpha(0.2f);
                    dot2.setAlpha(0.2f);
                    dot3.setAlpha(0.2f);

                    if (step == 0) animateDot(dot1);
                    else if (step == 1) animateDot(dot2);
                    else if (step == 2) animateDot(dot3);

                    step = (step + 1) % 3;

                    handler.postDelayed(this, 250);
                }
            };

            handler.post(runnable);

        } catch (Exception e) {
            // Window token invalid ya Activity mid-destroy - crash mat hone do
            progressDialog = null;
        }
    }

    private static void animateDot(View dot) {
        dot.setAlpha(1f);

        dot.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .setDuration(120)
                .withEndAction(() -> dot.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120));
    }

    /**
     * 🛑 Stop Loader
     *
     * FIX: yeh asli crash-fix hai. Pehle sirf "isShowing()" check karke
     * "dismiss()" call hota tha - lekin agar dialog jis Activity/Window se
     * bana tha wo already destroy ho chuki ho (user back dabakar nikal gaya,
     * ya screen rotate hui), to Android ka internal "isShowing()" state
     * stale (purana/galat) ho sakta hai aur dismiss() call karne par
     * "IllegalArgumentException: View not attached to window manager" crash
     * deta hai. Ab poora dismiss() try-catch me hai, aur crash chahe jo bhi
     * ho, progressDialog hamesha null ho jayega (finally block) taaki
     * agli baar fresh dialog bane.
     */
    public static void dismissLoading() {

        if (progressDialog == null) return;

        try {

            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

        } catch (IllegalArgumentException | IllegalStateException e) {

            // "View not attached to window manager" / activity destroyed
            // wale cases yaha silently handle ho jayenge - crash nahi hoga.

        } finally {

            progressDialog = null;
        }
    }
}