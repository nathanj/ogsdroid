package com.example.njones.myapplication;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

class Clock {
    private final static String TAG = "Clock";

    public int thinkingTime;
    public int periods;
    public int periodTime;
    public String system; // fischer, byo-yomi, etc.

    private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    Clock() {
        this("simple");
    }

    Clock(String system) {
        this.system = system;

        p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(1);
        p.setTextSize(20);
    }

    public void tick() {
        if (thinkingTime > 0) {
            thinkingTime--;
            return;
        }

        if (periods > 0) {
            periods--;
            thinkingTime = periodTime;
        }
    }

    public void setTime(int thinkingTime, int periods, int periodTime) {
        this.thinkingTime = thinkingTime;
        this.periods = periods;
        this.periodTime = periodTime;
    }

    public void set(JSONObject clock) {
        try {
            clock.getInt("thinking_time");
            clock.getInt("periods");
            clock.getInt("periodTime");
        } catch (JSONException e) {
        }
    }

    public void draw(Canvas canvas, String header, float x, float y) {
        if (thinkingTime < 0)
            return;
        canvas.drawText(header + ": " + toString(), x, y, p);
    }

    protected String formatTime(int seconds) {
        int days = seconds / (60 * 60 * 24);
        seconds %= (60 * 60 * 24);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        StringBuilder s = new StringBuilder();

        if (days > 1) {
            s.append(String.format("%d days", days));
        } else if (hours > 0) {
            s.append(String.format("%d:%02d:%02d", hours, minutes, seconds));
        } else if (minutes > 0) {
            s.append(String.format("%d:%02d", minutes, seconds));
        } else {
            s.append(String.format("%d", seconds));
        }
        return s.toString();
    }

    public String toString() {
        if (periods > 0) {
            return String.format("%s + %dx%s", formatTime(thinkingTime), periods, formatTime(periodTime));
        } else {
            return formatTime(thinkingTime);
        }
    }
}
