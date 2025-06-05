package com.example.serveradminapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class GaugeView extends View {
    private int percent = 0;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    public GaugeView(Context context) {
        super(context);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setColor(Color.LTGRAY);
        fgPaint.setStyle(Paint.Style.STROKE);
        fgPaint.setColor(Color.GREEN);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                12, getResources().getDisplayMetrics()));
    }

    public void setPercent(int value) {
        percent = Math.max(0, Math.min(100, value));
        invalidate();
    }

    @Override

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        if (size == 0) {
            size = Math.max(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(size, size);
    }

    @Override

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.1f;
        bgPaint.setStrokeWidth(stroke);
        fgPaint.setStrokeWidth(stroke);
        float halfStroke = stroke / 2f;
        arcRect.set(halfStroke, halfStroke, size - halfStroke, size - halfStroke);
        canvas.drawArc(arcRect, 0, 360, false, bgPaint);
        canvas.drawArc(arcRect, -90, 360f * percent / 100f, false, fgPaint);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = size / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(percent + "%", size / 2f, textY, textPaint);
    }
}
