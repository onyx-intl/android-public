package com.onyx.scribbleTest;

import android.view.MotionEvent;

/**
 * Created by joy on 6/6/14.
 */
public class SketchPoint {

    private final float x;
    private final float y;
    private final float pressure;
    private final float size;
    private final long eventTime;

    public SketchPoint(float x, float y, float pressure, float size, long eventTime) {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.size = size;
        this.eventTime = eventTime;
    }

    public static SketchPoint createFromEvent(MotionEvent e) {
        return new SketchPoint(e.getX(), e.getY(),
                e.getPressure(), e.getSize(), e.getEventTime());
    }

    public static SketchPoint createFromHistoricalEvent(MotionEvent e, int pos) {
        return new SketchPoint(e.getHistoricalX(pos), e.getHistoricalY(pos),
                e.getHistoricalPressure(pos), e.getHistoricalSize(pos),
                e.getHistoricalEventTime(pos));
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getPressure() {
        return pressure;
    }

    public float getSize() {
        return size;
    }

    public long getEventTime() {
        return eventTime;
    }

    public float distanceTo(SketchPoint point) {
        float dx = x - point.getX();
        float dy = y - point.getY();
        return (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    public float velocityFrom(SketchPoint point) {
        return distanceTo(point) / (eventTime - point.getEventTime());
    }

    @Override
    public String toString() {
        return "{x: " + x + ", y: " + y + ", pressure: " + pressure +
                ", size: " + size + ", event time: " + eventTime + "}";
    }
}
