package com.example.tscrcpydroid.util

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

/**
 * 不能直接在主线程进行IO，需要一个线程来管理ControlMessage的发送
 */
class ControlMessageSender(private val dataOutputStream: DataOutputStream) {
    private val controlMessageQueue = ArrayBlockingQueue<ByteArray>(20)

    fun pushContorlMessage(buffer: ByteArray){
        controlMessageQueue.put(buffer) //不能用offer,offer不会阻塞
    }

    fun loop(){
        while (!Thread.currentThread().isInterrupted){
            val buffer = controlMessageQueue.take()//不能用poll,poll不会阻塞
            try {
                dataOutputStream.write(buffer)
            }catch (e:IOException){
                Log.e("ZLT", "ControlMessage send failed")
            }
        }
    }

}