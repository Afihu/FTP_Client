package com.ftpclient.ftp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FTPResponse {
    private final int code;
    private final List<String> lines;

    private FTPResponse(int code, List<String> lines) {
        this.code = code;
        this.lines = Collections.unmodifiableList(lines);
    }

    /**
     * Parse a raw FTP reply (possibly multi‐line) into an FTPResponse.
     */
    public static FTPResponse parse(String rawReply) {
        String[] split = rawReply.split("\\r?\\n");
        List<String> list = new ArrayList<>();
        for (String line : split) {
            if (!line.isEmpty()) {
                list.add(line);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Empty FTP response");
        }
        // The numeric code is always the first 3 chars of the last line
        String last = list.get(list.size() - 1);
        int c;
        try {
            c = Integer.parseInt(last.substring(0, 3));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid FTP response code in: " + last, e);
        }
        return new FTPResponse(c, list);
    }

    /** The 3‐digit FTP response code. */
    public int getCode() {
        return code;
    }

    /** All lines of the reply, including any multiline “header”. */
    public List<String> getLines() {
        return lines;
    }

    /** The textual message (everything after the code on each line). */
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.length() > 4) {
                sb.append(line.substring(4));
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    /** 1xx Positive Preliminary reply. */
    public boolean isPositivePreliminary() {
        return code >= 100 && code < 200;
    }
    /** 2xx Positive Completion reply. */
    public boolean isPositiveCompletion() {
        return code >= 200 && code < 300;
    }
    /** 3xx Positive Intermediate reply. */
    public boolean isPositiveIntermediate() {
        return code >= 300 && code < 400;
    }
    /** 4xx Transient Negative Completion reply. */
    public boolean isTransientNegative() {
        return code >= 400 && code < 500;
    }
    /** 5xx Permanent Negative Completion reply. */
    public boolean isPermanentNegative() {
        return code >= 500 && code < 600;
    }

    @Override
    public String toString() {
        return String.join(System.lineSeparator(), lines);
    }
}