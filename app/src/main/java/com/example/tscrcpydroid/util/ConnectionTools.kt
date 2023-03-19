package com.example.tscrcpydroid.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.tscrcpydroid.data.entities.BitRate
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import java.io.IOException
import java.net.ConnectException
import java.net.Socket
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


/**
 * scrcpy客户端的实现是调用PC上的adb的adb connect ip:port连接到手机上的adbd
 * 由于控制端如果是手机的话我们并没有运行adb
 * 只能直接参照scrcpy-android：直接调adblib库链接到ip:port, 并且直接把server.jar当做echo参数在受控手机上执行命令
 * 执行完之后，受控手机应该要对应端口而不进行端口转发
 */
object ConnectionTools {

    /**
     * 0表示未就绪
     * 1表示就绪
     * 2表示错误
     */
    var status: AtomicInteger = AtomicInteger(0)


    /**
     * 用socket发送scrcpy-server.jar到目标设备并启动server
     * @return: 成功返回true否则返回false
     */
    fun startRemoteServer(
        context: Context, localIP: String,
        targetIP: String, targetPort: Int, fileBase64: ByteArray,
        bitRate: BitRate, size: Int, timeOut: Int
    ): Boolean {

        /**
         * ip: 控制端ip
         * port: server运行的端口
         * max_size: 最大尺寸, 传一个最大的就好，server会自动计算比例
         * bit_rate: 传输bitrate
         */
        // CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server 1.0 ip=192.168.1.192 port=13432 max_size=1920 bit_rate=1048576
        val appProcessCommand =
            " CLASSPATH=/data/local/tmp/scrcpy-server.jar " +
            "app_process / com.genymobile.scrcpy.Server 1.0 " +
            "ip=${localIP} port=${targetPort} max_size=${size} bit_rate=${bitRate.asParam()} >> log.txt\n"

        status.set(0)
        thread {
            try {
                adbWrite(context, targetIP, getFileBase64(context), appProcessCommand)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        //TODO: 下面这段代码会造成ANR，需要progressbar, 之后要用compose搞一个
        var cnt = 0
        while (status.get() == 0 && cnt < 100) {
            Log.i("ZLT", "waiting for adb, ${cnt} tries")
            Thread.sleep(timeOut / 100L) // timeOut等分成100份
            cnt++
        }
        if (cnt == 100) {
            status.set(2)
        }
        return status.get() == 1
    }

    /**
     * 此函数需要在子线程运行
     */
    fun adbWrite(context: Context, targetIP: String, fileBase64: ByteArray, cmd: String) {
        var adb: AdbConnection? = null
        var sock: Socket? = null
        var crypto: AdbCrypto? = null
        var stream: AdbStream? = null

        //先看看是否已经有密钥，否则需要创建密钥
        try {
            crypto = AdbCrypto.loadAdbKeyPair(
                AdbBase64Impl,
                context.getFileStreamPath("pri.key"),
                context.getFileStreamPath("pub.key")
            )
        } catch (e: Exception) {
            crypto = null
        }
        if (crypto == null) {
            crypto = AdbCrypto.generateAdbKeyPair(
                AdbBase64Impl
            )
            crypto?.saveAdbKeyPair(
                context.getFileStreamPath("pri.key"),
                context.getFileStreamPath("pub.key")
            )
        }

        try {
            Log.i("ZLT", "trying to connect ${targetIP}:5555")
            sock = Socket(targetIP, 5555) // 这个不是server运行的端口，而是adbd运行的端口
        } catch (e: UnknownHostException) {
            status.set(2)
            close(adb, sock)
            throw UnknownHostException("${targetIP}is no valid ip address")
        } catch (e: ConnectException) {
            status.set(2)
            close(adb, sock)
            throw ConnectException("Cannot connect ${targetIP}:5555")
        } catch (e: IOException) {
            e.printStackTrace()
            status.set(2)
            return
        }

        try {
            Log.i("ZLT", "trying to create adb connection")
            adb = AdbConnection.create(sock, crypto)
            val connected = adb?.connect(100000L, TimeUnit.MILLISECONDS, false)
            if(connected!=null && !connected){
                Log.i("ZLT", "create adb connection failed")
                status.set(2)
                close(adb, sock)
                return
            }
            stream = adb?.open("shell:")
            stream?.write(" \n");
        } catch (e: IOException) {
            Log.e("ZLT", "test empty fail: ${e}")
            status.set(2)
            close(adb, sock)
            return
        }

        var finished = false
        while (stream != null && !finished) {
            try {
                val response = stream?.read()?.let { String(it) }
                if (!response.isNullOrBlank() && (response.contains('$') || response.contains('#'))) {
                    finished = true
                }
            } catch (e: IOException) {
                Log.e("ZLT", "test read fail: ${e}")
                status.set(2)
                close(adb, sock)
                return
            }
        }

        //把用base64加密后的scrcpy-server.jar作为echo命令的参数写到目标设备上
        Log.i("ZLT", "trying to write server.jar")
        if (stream != null) {
            val filelength = fileBase64.size
            val PARTSIZE = 4056
            val part = ByteArray(PARTSIZE)
            var offset = 0
            try {
                stream?.write(" cd /data/local/tmp && rm scrcpy-server.jar\n")
                val response = stream?.read()?.let {
                    String(it, StandardCharsets.US_ASCII)
                }
                Log.i("ZLT", "cd response: ${response ?: ""}")
                var partStr: String? = null
                while (offset < filelength) {
                    if (offset + PARTSIZE <= filelength) {
                        System.arraycopy(fileBase64, offset, part, 0, PARTSIZE)
                        offset += PARTSIZE
                        partStr = String(part, StandardCharsets.US_ASCII)
                    } else {
                        val REMAINSIZE = filelength - offset
                        val remainPart = ByteArray(REMAINSIZE)
                        System.arraycopy(fileBase64, offset, remainPart, 0, REMAINSIZE)
                        offset += REMAINSIZE
                        partStr = String(remainPart, StandardCharsets.US_ASCII)
                    }
                    stream?.write(" echo ${partStr} >> serverBase64 \n")
                    var executed = false // 由于shell的返回值存在回显值，需要通过不断读取输出来判断命令是否执行结束，否则可能会塞爆ADBConnection的缓冲区
                    while (!executed) {
                        val response = stream?.read()?.let {
                            String(it, StandardCharsets.US_ASCII)
                        }
                        if (!response.isNullOrBlank() &&
                            (response.endsWith("$ ") || response.endsWith("# "))) {
                            executed = true
                        }
                    }
                }
                stream?.write(" base64 -d < serverBase64 > scrcpy-server.jar && rm serverBase64\n")
                Thread.sleep(1000) // 给点时间让对面解码
            } catch (e: IOException) {
                Log.e("ZLT", "Send scrcpy-server.jar failed: ${e}")
                status.set(2)
                close(adb, sock)
                return
            }
            try {
                Log.i("ZLT", "trying to run server.jar")
                Log.i("ZLT", cmd)
                stream.write(cmd)
                Thread.sleep(100)//没什么必要
            } catch (e: IOException) {
                Log.e("ZLT", "Run scrcpy-server.jar failed")
                status.set(2)
                return
            }finally {
                //close(adb, sock) 不能close，close的话启动的服务器进程就寄了
            }
            status.set(1)
        }
    }

    fun getFileBase64(context: Context): ByteArray {
        val assetManager = context.assets
        val inputStream = assetManager.open("scrcpy-server.jar")
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        return Base64.encode(buffer, Base64.NO_WRAP)
    }

    fun close(adb: AdbConnection?, sock: Socket?){
        adb?.close()
        sock?.apply {
            if(!this.isClosed()) {
                this.shutdownOutput()
                this.close()
            }
        }
    }
}

object AdbBase64Impl : AdbBase64 {
    override fun encodeToString(data: ByteArray?): String {
        return Base64.encodeToString(data, Base64.DEFAULT)
    }
}