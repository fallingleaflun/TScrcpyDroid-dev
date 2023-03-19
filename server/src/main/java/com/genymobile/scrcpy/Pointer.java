package com.genymobile.scrcpy;

public class Pointer {

    /**
     * Pointer id as received from the client.
     */
    private final long id;

    /**
     * Local pointer id, using the lowest possible values to fill the {@link android.view.MotionEvent.PointerProperties PointerProperties}.
     * 传过来的是long，但是安卓的pointerId是int
     * 以及传过来的是控制端的id，可能和受控端的id不一样，需要处理一个映射
     * 实际是造了一个List存放(PointersState.pointers)，我觉得很奇怪，为什么不用set而是用list
     */
    private final int localId;

    private Point point;
    private float pressure;
    private boolean up;

    public Pointer(long id, int localId) {
        this.id = id;
        this.localId = localId;
    }

    public long getId() {
        return id;
    }

    public int getLocalId() {
        return localId;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public boolean isUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }
}
