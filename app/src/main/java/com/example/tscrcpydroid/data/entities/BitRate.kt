package com.example.tscrcpydroid.data.entities

/**
 * Byte per second
 */
data class BitRate(val value: Int) {
    override fun toString(): String {
        return "${value/1024/1024}Mb/s"
    }

    fun asParam(): Int{
        return value
    }
}