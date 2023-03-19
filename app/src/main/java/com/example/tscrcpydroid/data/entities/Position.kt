package com.example.tscrcpydroid.data.entities

import java.util.Objects

/**
 * Position必须要附带屏幕大小，不然得自己手动转换，以及旋转可能会有点难处理
 */
class Position(val point: Point, screenSize: Size) {
    private val screenSize: Size

    init {
        this.screenSize = screenSize
    }

    constructor(x: Int, y: Int, screenWidth: Int, screenHeight: Int) : this(
        Point(x, y),
        Size(screenWidth, screenHeight)
    )

    fun getScreenSize(): Size {
        return screenSize
    }

    fun rotate(rotation: Int): Position {
        return when (rotation) {
            1 -> Position(
                Point(
                    screenSize.height - point.y,
                    point.x
                ), screenSize.rotate()
            )

            2 -> Position(
                Point(
                    screenSize.width - point.x,
                    screenSize.height - point.y
                ), screenSize
            )

            3 -> Position(
                Point(
                    point.y,
                    screenSize.width - point.x
                ), screenSize.rotate()
            )

            else -> this
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val position = o as Position
        return point == position.point && screenSize == position.screenSize
    }

    override fun hashCode(): Int {
        return Objects.hash(point, screenSize)
    }

    override fun toString(): String {
        return "Position{point=$point, screenSize=$screenSize}"
    }
}