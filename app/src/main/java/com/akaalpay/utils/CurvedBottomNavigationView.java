package com.akaalpay.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.akaalpay.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Curved "cradle" bottom navigation.
 * Selected item ek gol circle mein upar uthta hai aur bar mein uske neeche curve kat-ta hai.
 * Menu, IDs aur listener bilkul normal BottomNavigationView ki tarah kaam karte hain.
 */
public class CurvedBottomNavigationView extends BottomNavigationView {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private float curX = -1f;
    private int lastId = View.NO_ID;
    private ValueAnimator animator;
    private boolean animating = false;

    public CurvedBottomNavigationView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CurvedBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CurvedBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int color = ContextCompat.getColor(context, R.color.feno_primary);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(color);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(color);

        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        // Material 3 theme selected icon ke peeche purple pill (active indicator) dikhati hai.
        // Hamara apna circle hai, isliye ise band kar rahe hain.
        setItemActiveIndicatorEnabled(false);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        View menu = getChildCount() > 0 ? getChildAt(0) : null;
        int selId = getSelectedItemId();
        View sel = findViewById(selId);

        if (!(menu instanceof ViewGroup) || sel == null || sel.getWidth() == 0) {
            super.dispatchDraw(canvas);
            return;
        }

        ViewGroup menuGroup = (ViewGroup) menu;
        menuGroup.setClipChildren(false);
        menuGroup.setClipToPadding(false);

        float bump = getPaddingTop();      // bar ka top edge (upar circle ke liye jagah)
        float cy = bump - dp(3);           // circle ka center
        float lift = (menuGroup.getTop() + sel.getTop() + sel.getHeight() / 2f) - cy;
        float targetX = menuGroup.getLeft() + sel.getLeft() + sel.getWidth() / 2f;

        if (lastId != selId) {
            if (lastId == View.NO_ID || curX < 0) {
                curX = targetX;
            } else {
                animateTo(findViewById(lastId), sel, targetX, lift);
            }
            lastId = selId;
        }

        if (!animating) {
            curX = targetX;
            for (int i = 0; i < menuGroup.getChildCount(); i++) {
                View child = menuGroup.getChildAt(i);
                child.setTranslationY(child == sel ? -lift : 0f);
            }
        }

        drawBar(canvas, bump, cy);
        super.dispatchDraw(canvas);
    }

    private void animateTo(@Nullable final View prev, final View sel, final float targetX, final float lift) {
        final float fromX = curX;

        if (animator != null) {
            animator.cancel();
        }
        animating = true;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(320);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            curX = fromX + (targetX - fromX) * f;
            if (prev != null && prev != sel) {
                prev.setTranslationY(-lift * (1f - f));
            }
            sel.setTranslationY(-lift * f);
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animating = false;
                invalidate();
            }
        });
        animator.start();
    }

    private void drawBar(Canvas canvas, float bump, float cy) {
        float w = getWidth();
        float h = getHeight();
        float halfWidth = dp(48);   // curve ki chaudai (aadhi)
        float depth = dp(27);       // curve ki gehrai
        float edge = dp(50);

        path.reset();
        path.moveTo(-edge, bump);
        path.lineTo(curX - halfWidth, bump);
        path.cubicTo(curX - halfWidth * 0.55f, bump,
                curX - halfWidth * 0.55f, bump + depth,
                curX, bump + depth);
        path.cubicTo(curX + halfWidth * 0.55f, bump + depth,
                curX + halfWidth * 0.55f, bump,
                curX + halfWidth, bump);
        path.lineTo(w + edge, bump);
        path.lineTo(w + edge, h);
        path.lineTo(-edge, h);
        path.close();

        canvas.drawPath(path, barPaint);
        canvas.drawCircle(curX, cy, dp(25), circlePaint);
    }
}