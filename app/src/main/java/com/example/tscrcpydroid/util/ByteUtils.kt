package com.example.tscrcpydroid.util

import java.nio.ByteBuffer

/**
 * byte数组转换为Long
 * Int: [........ ........ ........ ........]
 * Long:[........ ........ ........ ........ ........ ........ ........ ........]
 *
 */
fun byteToLong(buffer: ByteArray): Long{
    return ByteBuffer.wrap(buffer).long
}

fun byteToInt(buffer: ByteArray): Int{
    return ByteBuffer.wrap(buffer).int
}