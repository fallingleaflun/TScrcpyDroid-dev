package com.example.tscrcpydroid.util

import android.view.MotionEvent
import com.example.tscrcpydroid.data.entities.TYPE_INJECT_TOUCH_EVENT
import java.io.DataOutputStream

class ControlMessageWriter(private val sender: ControlMessageSender) {
    private val TYPE_INJECT_LENGTH = 1 //c++的char
    private val INJECT_KEYCODE_PAYLOAD_LENGTH = 13 //
    private val INJECT_TOUCH_EVENT_PAYLOAD_LENGTH = 27
    private val INJECT_SCROLL_EVENT_PAYLOAD_LENGTH = 20
    private val BACK_OR_SCREEN_ON_LENGTH = 1
    private val SET_SCREEN_POWER_MODE_PAYLOAD_LENGTH = 1
    private val GET_CLIPBOARD_LENGTH = 1
    private val SET_CLIPBOARD_FIXED_PAYLOAD_LENGTH = 9
    private val ACTION_LENGH = 1 //

    private val MESSAGE_MAX_SIZE = 1 shl 18; // 256k

    val CLIPBOARD_TEXT_MAX_LENGTH =
        MESSAGE_MAX_SIZE - 14; // type: 1 byte; sequence: 8 bytes; paste flag: 1 byte; length: 4 bytes
    val INJECT_TEXT_MAX_LENGTH = 300;

    /**
     * MotionEvent编码并写到输出流中
     * 写入到流的操作是否需要开子线程操作？如果是，socket是否会发生乱序？
     */
    fun writeMotionEvent(event: MotionEvent, width: Int, height: Int, output: DataOutputStream) {
        val buffer = ByteArray(TYPE_INJECT_LENGTH + INJECT_TOUCH_EVENT_PAYLOAD_LENGTH)
        //type: 1字节
        //action: 1字节
        //pointerid: 8字节(一个Long)
        //position: 12字节(x和y分别是int32, width和height分别是short)
        //pressure: 2字节
        //button: 4字节
        buffer[0] = TYPE_INJECT_TOUCH_EVENT.toByte()
        buffer[1] = event.action.toByte()
        writeToBuffer(event.actionIndex.toLong(), buffer, 2) //我不知道为什么协议要用Long
        writeToBuffer(event.x.toInt(), buffer, 10)
        writeToBuffer(event.y.toInt(), buffer, 14)
        writeToBuffer(width.toShort(), buffer, 18)
        writeToBuffer(height.toShort(), buffer, 20)
        writeToBuffer(event.pressure.toU16FixedPoint().toShort(), buffer, 22) //UShort和Shor应该可以直接转
        writeToBuffer(event.actionButton, buffer, 24)
        sender.pushContorlMessage(buffer)
    }

    fun writeToBuffer(value: Long, buffer: ByteArray, offset: Int) {
        val tmp = value.toByteArray()
        tmp.copyInto(buffer, offset, 0, tmp.size)
    }

    fun writeToBuffer(value: Int, buffer: ByteArray, offset: Int) {
        val tmp = value.toByteArray()
        tmp.copyInto(buffer, offset, 0, tmp.size)
    }

    fun writeToBuffer(value: Short, buffer: ByteArray, offset: Int){
        val tmp =value.toByteArray()
        tmp.copyInto(buffer, offset, 0, tmp.size)
    }
}