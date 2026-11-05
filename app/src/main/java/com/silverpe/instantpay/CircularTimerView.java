package com.silverpe.instantpay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * A simple, self-drawn circular countdown ring.
 *
 * We stopped using Material's CircularProgressIndicator because its
 * rendering came out off-center / with an uneven (thin) patch on some
 * devices. This view draws the track and the progress arc itself on
 * the canvas, so the ring is always perfectly centered and an even
 * thickness all the way round, regardless of device/library version.
 *
 * Usage from Java:
 *   timerProgress.setMax(30);
 *   timerProgress.setProgress(secondsLeft);
 *   timerProgress.setRingColor(Color.parseColor("#2E7D32"));
 */
public class CircularTimerView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private int max = 30;
    private int progress = 30;
    private final float strokeWidthPx;

    public CircularTimerView(Context context) {
        super(context);
        strokeWidthPx = dpToPx(8);
        init();
    }

    public CircularTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        strokeWidthPx = dpToPx(8);
        init();
    }

    public CircularTimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        strokeWidthPx = dpToPx(8);
        init();
    }

    private void init() {

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setColor(Color.parseColor("#E3E7F1"));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#2E7D32"));
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
        invalidate();
    }

    public void setRingColor(int color) {
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Inset by half the stroke width on every side so the stroke
        // itself (not just the path) stays fully inside the view's
        // bounds - this is what guarantees even centering.
        float inset = strokeWidthPx / 2f;

        arcBounds.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Full background track (a complete circle)
        canvas.drawArc(arcBounds, 0, 360, false, trackPaint);

        // Foreground progress arc - starts at 12 o'clock, sweeps clockwise
        float sweepAngle = 360f * progress / max;

        canvas.drawArc(arcBounds, -90, sweepAngle, false, progressPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}