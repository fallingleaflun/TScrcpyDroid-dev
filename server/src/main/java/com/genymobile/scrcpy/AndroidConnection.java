package com.genymobile.scrcpy;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 原来的代码是用LocalSocket进行端口转发的，但是控制端是手机的时候我不懂怎么实现，只能直接用socket了
 * 补充隧道的原理:
 * - `adb forward tcp:11111 tcp:22222`会使得PC发送到11111端口的数据被转发到手机的22222端口上
 * 也即pc只需创建socket连接到11111, 而手机只需打开serverSocket监听22222
 * PC上的adb会创建一个serverSocket监听11111, 手机上的adbd会创建socket连接到22222端口
 */
public class AndroidConnection implements Closeable {
    private static final int DEVICE_NAME_FIELD_LENGTH = 64;
    private final Socket videoSocket;
    //private final FileDescriptor videoFd; // Socket的getFileDescriptor方法被设置成hide了，不能用linux风格的socket
    private final OutputStream videoOutputStream;

    private final Socket controlSocket;
    private final InputStream controlInputStream;
    private final OutputStream controlOutputStream;

    private final ControlMessageReader reader = new ControlMessageReader();
    private final DeviceMessageWriter writer = new DeviceMessageWriter();

    private AndroidConnection(Socket videoSocket, Socket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.controlSocket = controlSocket;
        if (controlSocket != null) {
            controlInputStream = controlSocket.getInputStream();
            controlOutputStream = controlSocket.getOutputStream();
        } else {
            controlInputStream = null;
            controlOutputStream = null;
        }
        videoOutputStream = videoSocket.getOutputStream();
    }

    private static Socket listenAndAccept(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = null;
        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket.close();
        }
        return socket;
    }

    /**
     * 该方法会阻塞至收到一个请求
     *
     * @param ip:      所接受的目标ip，其实是Server端的ip，连接者连接的需要需要和这个一致
     * @param port:    本地端口
     * @param control: 是否控制，如果控制就会多开1个socket
     */
    public static AndroidConnection open(String ip, int port, boolean control) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        try {
            Socket videoSocket = serverSocket.accept();
            if (videoSocket.getInetAddress().toString().equals(ip)) {
                try {
                    if (control) {
                        Socket controlSocket = serverSocket.accept();
                        if (controlSocket.getInetAddress().toString().equals(ip)) {
                            return new AndroidConnection(videoSocket, controlSocket);
                        } else {
                            Ln.e("Unknown ip accepted, I will close it");
                            controlSocket.close();
                            videoSocket.close();
                        }
                    } else {
                        return new AndroidConnection(videoSocket, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    videoSocket.close();
                }
            } else {
                Ln.e("Unknown ip accepted, I will close it");
                videoSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serverSocket.close();//accept完serverSocket就可以close了
        }
        return null;
    }

    public void close() throws IOException {
        videoSocket.shutdownInput();
        videoSocket.shutdownOutput();
        videoSocket.close();
        if (controlSocket != null) {
            controlSocket.shutdownInput();
            controlSocket.shutdownOutput();
            controlSocket.close();
        }
    }

    public void sendDeviceMeta(String deviceName, int width, int height) throws IOException {
        byte[] buffer = new byte[DEVICE_NAME_FIELD_LENGTH + 4];

        byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
        int len = StringUtils.getUtf8TruncationIndex(deviceNameBytes, DEVICE_NAME_FIELD_LENGTH - 1);
        System.arraycopy(deviceNameBytes, 0, buffer, 0, len);
        // byte[] are always 0-initialized in java, no need to set '\0' explicitly

        buffer[DEVICE_NAME_FIELD_LENGTH] = (byte) (width >> 8);
        buffer[DEVICE_NAME_FIELD_LENGTH + 1] = (byte) width;
        buffer[DEVICE_NAME_FIELD_LENGTH + 2] = (byte) (height >> 8);
        buffer[DEVICE_NAME_FIELD_LENGTH + 3] = (byte) height;
        //IO.writeFully(videoFd, buffer, 0, buffer.length);
        TSIO.writeFully(videoOutputStream, buffer, 0, buffer.length);
    }

    public OutputStream getVideoOutputStream() {
        return videoOutputStream;
    }

    public ControlMessage receiveControlMessage() throws IOException {
        ControlMessage msg = reader.next();
        while (msg == null) {
            reader.readFrom(controlInputStream);
            msg = reader.next();
        }
        return msg;
    }

    public void sendDeviceMessage(DeviceMessage msg) throws IOException {
        writer.writeTo(msg, controlOutputStream);
    }
}
