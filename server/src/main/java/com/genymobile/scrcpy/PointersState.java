package com.genymobile.scrcpy;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 这个设计有点暴力，N**2是吧
 */
public class PointersState {

    public static final int MAX_POINTERS = 10;

    private final List<Pointer> pointers = new ArrayList<>();

    /**
     * 控制端id->受控端id，如果不存在这个id对应的pointer就返回-1
     */
    private int indexOf(long id) {
        for (int i = 0; i < pointers.size(); ++i) {
            Pointer pointer = pointers.get(i);
            if (pointer.getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 测试某个本地id对应的pointer是否存在，存在则不可用
     */
    private boolean isLocalIdAvailable(int localId) {
        for (int i = 0; i < pointers.size(); ++i) {
            Pointer pointer = pointers.get(i);
            if (pointer.getLocalId() == localId) {
                return false;
            }
        }
        return true;
    }


    private int nextUnusedLocalId() {
        for (int localId = 0; localId < MAX_POINTERS; ++localId) {
            if (isLocalIdAvailable(localId)) {
                return localId;
            }
        }
        return -1;
    }

    /**
     * 这个index是list的Index，不是pointer的Index
     */
    public Pointer get(int index) {
        return pointers.get(index);
    }

    /**
     * 控制端id->受控端id
     * 如果不存在这个id对应的pointer就新建一个并返回
     * 如果已经超过触点上限则返回-1
     */
    public int getPointerIndex(long id) {
        int index = indexOf(id);
        if (index != -1) {
            // already exists, return it
            return index;
        }
        if (pointers.size() >= MAX_POINTERS) {
            // it's full
            return -1;
        }
        // id 0 is reserved for mouse events
        int localId = nextUnusedLocalId();
        if (localId == -1) {
            throw new AssertionError("pointers.size() < maxFingers implies that a local id is available");
        }
        Pointer pointer = new Pointer(id, localId);
        pointers.add(pointer);
        // return the index of the pointer
        return pointers.size() - 1;
    }

    /**
     * Initialize the motion event parameters.
     *
     * @param props  the pointer properties
     * @param coords the pointer coordinates
     * @return The number of items initialized (the number of pointers).
     */
    public int update(MotionEvent.PointerProperties[] props, MotionEvent.PointerCoords[] coords) {
        int count = pointers.size();
        for (int i = 0; i < count; ++i) {
            Pointer pointer = pointers.get(i);

            // id 0 is reserved for mouse events
            props[i].id = pointer.getLocalId();

            Point point = pointer.getPoint();
            coords[i].x = point.getX();
            coords[i].y = point.getY();
            coords[i].pressure = pointer.getPressure();
        }
        cleanUp();
        return count;
    }

    /**
     * Remove all pointers which are UP.
     */
    private void cleanUp() {
        for (int i = pointers.size() - 1; i >= 0; --i) {
            Pointer pointer = pointers.get(i);
            if (pointer.isUp()) {
                pointers.remove(i);
            }
        }
    }
}
