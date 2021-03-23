package com.android.calendar.datetimepicker.listeners;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.android.calendar.datetimepicker.widget.WheelView;

final public class WheelViewGestureListener extends GestureDetector.SimpleOnGestureListener {

    private final WheelView wheelView;

    public WheelViewGestureListener(WheelView wheelView) {
        this.wheelView = wheelView;
    }

    @Override
    public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        wheelView.scrollBy(velocityY);
        return true;
    }
}
