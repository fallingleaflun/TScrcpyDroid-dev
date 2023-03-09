package com.genymobile.scrcpy;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import com.example.tscrcpydroid.BuildConfig;

import java.io.DataOutputStream;
import java.io.FileDescriptor;
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

    public static void writeFully(OutputStream outputStream, ByteBuffer from) throws IOException {
        byte[] bytes = new byte[from.remaining()];
        from.get(bytes, 0, bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }

    public static void writeFully(OutputStream outputStream, byte[] buffer, int offset, int len) throws IOException {
        outputStream.write(buffer, offset, len);
        outputStream.flush();
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
