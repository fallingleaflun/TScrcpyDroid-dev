package com.example.tscrcpydroid.data.entities

data class Resolution(
    val width: Int,
    val height: Int
){
    override fun toString(): String {
        return "${width}x${height}"
    }
}
