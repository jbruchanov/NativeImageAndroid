package com.scurab.andriod.nativeimage;

import android.util.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JBruchanov on 25/10/2016.
 */

class ShellHelper {
    private static final String CMD_MEMORY = "cat /proc/meminfo";
    private final static Pattern MEM_PATTERN = Pattern.compile("(\\d*).?kb");

    public static Pair<Long, Long> getDeviceMemory() {
        String result = executeSafe(CMD_MEMORY);
        StringTokenizer stringTokenizer = new StringTokenizer(result, "\n");
        long total = 0;
        long free = 0;
        while (stringTokenizer.hasMoreTokens()) {
            String s = stringTokenizer.nextToken().toLowerCase();
            if (s.startsWith("memtotal:")) {
                total = parseSize(s) * 1024;
            } else if (s.startsWith("memfree:")) {
                free = parseSize(s) * 1024;
            }

            if (total != 0 && free != 0) {
                break;
            }
        }
        return new Pair<>(total, free);
    }

    private static long parseSize(String value) {
        final Matcher matcher = MEM_PATTERN.matcher(value);
        if (matcher.find()) {
            String v = matcher.group(1);
            try {
                return Long.parseLong(v);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public static String executeSafe(String cmd, String... cmds) {
        String result = null;
        try {
            result = execute(cmd, cmds);
        } catch (Throwable e) {
            StringWriter stack = new StringWriter();
            e.printStackTrace(new PrintWriter(stack));
            result = e.getMessage() + "\n" + stack.toString();
        }

        return result;
    }

    public static String execute(String cmd, String... cmds) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(cmd);
        DataOutputStream dos = new DataOutputStream(p.getOutputStream());
        for (String c : cmds) {
            dos.writeBytes(c + "\n");
        }
        dos.flush();
        dos.close();
        p.waitFor();
        InputStream inputStream = p.getInputStream();
        return toString(inputStream);
    }

    private static String toString(InputStream is) throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int read = 0;
        StringBuffer sb = new StringBuffer();
        while ((read = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read));
        }
        return sb.toString();
    }
}
