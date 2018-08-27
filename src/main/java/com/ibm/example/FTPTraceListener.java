package com.ibm.example;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class FTPTraceListener implements MyFTPClient.FTPCommunicationListener {

    static private SimpleDateFormat formatter;

    String getNowString() {
        if (formatter == null) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            TimeZone gmtTime = TimeZone.getTimeZone("GMT");
            formatter.setTimeZone(gmtTime);
        }
        return formatter.format(new Date());
    }

    private boolean active = true;
    private List<String> lines = new ArrayList<>();

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void log(String msg) {
        append("[LOG] " + getNowString() + " " + msg);
    }

    private void append(String msg) {
        if (!active) {
            return;
        }
        lines.add(msg);
        System.out.println("   " + msg);
    }

    @Override
    public void sent(String s) {
        append("[FTP] " + getNowString() + " -> " + s);
    }

    @Override
    public void received(String s) {
        append("[FTP] " + getNowString() + " <- " + s);
    }
}
