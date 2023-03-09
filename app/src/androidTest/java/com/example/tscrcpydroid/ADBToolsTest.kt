package com.example.tscrcpydroid

import android.content.Context
import android.content.res.AssetManager
import android.util.Base64
import android.util.Log
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.util.ConnectionTools.startRemoteServer
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.Socket
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ADBToolsTest {
    //val localIP = "192.168.180.101"
    //val targetIP = "192.168.180.102"
    //val targetPort = 13432

    val localIP = "192.168.1.142"
    val targetIP = "192.168.1.239"
    val targetPort = 13432


    @Test
    fun testADBTest() {
        //测试adblib是否可用
        val context: Context = ApplicationProvider.getApplicationContext()//利用这个在测试类中获取context
        val socket = Socket(targetIP, 5555)
        var crypto: AdbCrypto? = null
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

        val connection = AdbConnection.create(socket, crypto)
        connection.connect(10000L, TimeUnit.MILLISECONDS, false)
        val stream = connection.open("shell:")
        val response = stream.read()
        val resstr = String(response)
        Log.i("ZLT", resstr)
        connection.close()
    }

    @Test
    fun startRemoteServerTest() {
        val context: Context = ApplicationProvider.getApplicationContext()//利用这个在测试类中获取context
        val assetManager = context.assets
        val inputStream = assetManager.open("scrcpy-server.jar")
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        val fileBase64 = Base64.encode(buffer, Base64.NO_WRAP)
        val result = startRemoteServer(
            context = context,
            localIP = localIP,
            targetIP = targetIP,
            targetPort = 13432,
            fileBase64 = fileBase64,
            bitRate = BitRate(1024 * 1024),
            size = 1280,
            timeOut = 100000
        )
        Assert.assertTrue(result)
        while (true){
        }
        //已经测试通过，可以启动server.jar
    }

    @Test
    fun writeSomethingTest() {
        //测试过发现write的第一个字符会变成乱码，所以需要加个空格
        //测试过发现read得到的返回值有一堆乱码以及之前输入的值，issue如是说https://github.com/tananaev/adblib/issues/17
        val context: Context = ApplicationProvider.getApplicationContext()//利用这个在测试类中获取context
        val socket = Socket(targetIP, 5555)
        var crypto: AdbCrypto? = null
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

        val connection = AdbConnection.create(socket, crypto)
        connection.connect(10000L, TimeUnit.MILLISECONDS, false)
        var stream = connection.open("shell:")
        Log.i("ZLT", String(stream.read()))
        Log.i("ZLT", "test 1")
        execCommand(stream, " cd /data/local\n")
        Log.i("ZLT", "test 2")
        execCommand(stream, " ls\n")
        Log.i("ZLT", "test 3")
        execCommand(stream, " cd ../\n")
        Log.i("ZLT", "test 4")
        while (!stream.isClosed) {
            Log.i("ZLT", String(stream.read()))
        }
        connection.close()
    }

    fun execCommand(stream: AdbStream, cmd :String){
        stream.write(cmd)
        Thread.sleep(1000L)
        Log.i("ZLT", String(stream.read(), Charsets.US_ASCII))
    }
}

object AdbBase64Impl : AdbBase64 {
    override fun encodeToString(data: ByteArray?): String {
        return Base64.encodeToString(data, Base64.DEFAULT)
    }
}