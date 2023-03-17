package com.example.tscrcpydroid.data.entities

import com.example.tscrcpydroid.data.entities.Point
import java.util.Objects

class Position(val point: Point, screenSize: Size) {
    private val screenSize: Size

    init {
        this.screenSize = screenSize
    }

    constructor(x: Int, y: Int, screenWidth: Int, screenHeight: Int) : this(
        Point(x, y),
        Size(screenWidth, screenHeight)
    ) {
    }

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