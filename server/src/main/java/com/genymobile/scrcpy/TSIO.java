package com.genymobile.scrcpy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;

public final class TSIO {
    //用不了linuxAPI，要用位于上一层的NIO处理
    private TSIO() {
        // not instantiable
    }

    public static void writeFully(OutputStream outputStream, ByteBuffer from) {
        byte[] bytes = new byte[from.remaining()];
        from.get(bytes, 0, bytes.length);
        try {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            Ln.e("writeFully fail");
            throw new RuntimeException(e);
        }
    }

    public static void writeFully(OutputStream outputStream, byte[] buffer, int offset, int len) {
        try {
            outputStream.write(buffer, offset, len);
            outputStream.flush();
        } catch (IOException e) {
            Ln.e("writeFully fail");
            throw new RuntimeException(e);
        }
    }

    public static String toString(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine()).append('\n');
        }
        return builder.toString();
    }
}
