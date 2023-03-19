package com.example.tscrcpydroid.util

import android.util.Log
import java.nio.ByteBuffer

/**
 * 使用扩展函数进行类型转换
 */
fun ByteArray.toLong(): Long{
    return ByteBuffer.wrap(this).long
}

fun ByteArray.toInt(): Int{
    return ByteBuffer.wrap(this).int
}

fun Long.toByteArray(): ByteArray{
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.putLong(this)
    return buffer.array()
}

fun Int.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
    buffer.putInt(this)
    return buffer.array()
}

fun Short.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(Short.SIZE_BYTES)
    buffer.putShort(this)
    return buffer.array()
}

/**
 * 将一个0.0f到1.0f之间的float转为一个unsigned 16-bit fixed-point value
 */
fun Float.toU16FixedPoint(): UShort {
    if(0.0F<=this && this<=1.0F){
        val x = 65536.0F
        val u = (this * x).toUInt()
        if(u>= 65535u && u == 65536u){
            return u.toUShort()
        }
    }
    Log.e("ZLT", "Convert to U16FixedPoint error")
    return UShort.MIN_VALUE
}