package com.wirehall.audiorecorder.visualizer.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * base class that contains common implementation for all visualizers.
 */

abstract public class BaseVisualizerView extends View {
    protected byte[] bytes;
    protected Paint paint = new Paint();
    protected int color = Color.BLUE;

    public BaseVisualizerView(Context context) {
        super(context);
        init(null);
    }

    public BaseVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BaseVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     * set color to visualizer with color resource id.
     *
     * @param color resource id of color.
     */
    public void setColor(int color) {
        this.color = color;
        this.paint.setColor(this.color);
    }

    public void setBytes(byte[] bytes) {
        BaseVisualizerView.this.bytes = bytes;
    }

    protected abstract void init(@Nullable AttributeSet attributeSet);
}