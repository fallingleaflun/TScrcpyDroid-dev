package com.genymobile.scrcpy;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 负责处理
 * controlSocket的接收以及videoSocket的发送
 */
public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private static final String SOCKET_NAME = "scrcpy";

    private final LocalSocket videoSocket;
    private final FileDescriptor videoFd; // streamScreen会用到

    private final LocalSocket controlSocket;
    private final InputStream controlInputStream;
    private final OutputStream controlOutputStream;

    private final ControlMessageReader reader = new ControlMessageReader();
    private final DeviceMessageWriter writer = new DeviceMessageWriter();

    private DesktopConnection(LocalSocket videoSocket, LocalSocket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.controlSocket = controlSocket;
        if (controlSocket != null) {
            controlInputStream = controlSocket.getInputStream();
            controlOutputStream = controlSocket.getOutputStream();
        } else {
            controlInputStream = null;
            controlOutputStream = null;
        }
        videoFd = videoSocket.getFileDescriptor();
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }

    /**
     * 补充隧道的原理:
     *  - `adb forward tcp:11111 tcp:22222`会使得PC发送到11111端口的数据被转发到手机的22222端口上
     *    也即pc只需创建socket连接到11111, 而手机只需打开serverSocket监听22222
     *    PC上的adb会创建一个serverSocket监听11111, 手机上的adbd会创建socket连接到22222端口
     * 新建一个DesktopConnection实例
     * @param tunnelForward: 如果启用隧道，会新建一个LocalServerSocket接收被本地端口转发的请求. 否则建立一个LocalSocket连接到本地端口
     * @param control: 是否创建控制流
     * @param sendDummyByte: 是否检查
     * @return
     * @throws IOException
     */
    public static DesktopConnection open(boolean tunnelForward, boolean control, boolean sendDummyByte) throws IOException {
        LocalSocket videoSocket;
        LocalSocket controlSocket = null;
        if (tunnelForward) {
            LocalServerSocket localServerSocket = new LocalServerSocket(SOCKET_NAME);
            try {
                videoSocket = localServerSocket.accept();
                if (sendDummyByte) {
                    // send one byte so the client may read() to detect a connection error
                    videoSocket.getOutputStream().write(0);
                }
                if (control) {
                    try {
                        controlSocket = localServerSocket.accept();
                    } catch (IOException | RuntimeException e) {
                        videoSocket.close();
                        throw e;
                    }
                }
            } finally {
                localServerSocket.close();
            }
        } else {
            videoSocket = connect(SOCKET_NAME);
            if (control) {
                try {
                    controlSocket = connect(SOCKET_NAME);
                } catch (IOException | RuntimeException e) {
                    videoSocket.close();
                    throw e;
                }
            }
        }

        return new DesktopConnection(videoSocket, controlSocket);
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
        IO.writeFully(videoFd, buffer, 0, buffer.length);
    }

    public FileDescriptor getVideoFd() {
        return videoFd;
    }

    /**
     * 从打开的InputStream读取控制消息
     * @return 读取到的控制消息
     * @throws IOException
     */
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
