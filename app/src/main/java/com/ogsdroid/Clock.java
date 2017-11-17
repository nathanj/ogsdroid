package com.ogsdroid;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.json.JSONException;
import org.json.JSONObject;

class Clock {
    private final static String TAG = "Clock";

    public long thinkingTime;
    public long periods;
    public long periodTime;
    public String system; // fischer, byo-yomi, etc.

    private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    Clock() {
        this("simple");
    }

    Clock(String system) {
        this.system = system;

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

    public void setTime(long thinkingTime, long periods, long periodTime) {
        this.thinkingTime = thinkingTime;
        this.periods = periods;
        this.periodTime = periodTime;
    }

    public void set(JSONObject clock) {
        try {
            setTime(clock.getInt("thinking_time"),
                    clock.getInt("periods"),
                    clock.getInt("periodTime"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void draw(Canvas canvas, boolean black, String username, float sx, float sy, float w, float h, boolean mymove) {
        if (mymove) {
            if (urgent()) {
                p.setARGB(255, 255, 0, 0);
                p.setStrokeWidth(1);
                canvas.drawRect(sx, sy, sx + w, sy + h, p);
            } else {
                p.setARGB(255, 0, 128, 0);
                p.setStrokeWidth(1);
                canvas.drawRect(sx, sy, sx + w, sy + h, p);
            }
        }
        if (black) {
            p.setARGB(255, 0, 0, 0);
            p.setStrokeWidth(1);
        } else {
            p.setARGB(255, 255, 255, 255);
            p.setStrokeWidth(1);
        }
        canvas.drawRect(sx + 5, sy + 5, sx + w - 5, sy + h - 5, p);
        if (black) {
            p.setARGB(255, 255, 255, 255);
            p.setStrokeWidth(1);
        } else {
            p.setARGB(255, 0, 0, 0);
            p.setStrokeWidth(1);
        }
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(h / 8);
        p.setTypeface(Typeface.MONOSPACE);
        canvas.drawText(username, sx + w / 2, sy + h / 3, p);
        canvas.drawText(toString(), sx + w / 2, sy + h * 2 / 3, p);
    }

    protected String formatTime(long seconds) {
        long days = seconds / (24 * 60 * 60);
        seconds %= (24 * 60 * 60);
        long hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder s = new StringBuilder();

        if (days > 0) {
            s.append(String.format("%dd", days));
            if (hours > 0)
                s.append(String.format("%dh", hours));
        } else if (hours > 0) {
            s.append(String.format("%dh", hours));
            if (minutes > 0)
                s.append(String.format("%dm", minutes));
        } else if (minutes > 0) {
            s.append(String.format("%dm%02ds", minutes, seconds));
        } else {
            s.append(String.format("%ds", seconds));
        }
        return s.toString();
    }

    public String toString() {
        if (thinkingTime <= 0 && periods == 0)
            return "";
        if (periods > 0) {
            return String.format("%s + %dx%s", formatTime(thinkingTime), periods, formatTime(periodTime));
        } else {
            return formatTime(thinkingTime);
        }
    }

    public boolean urgent() {
        return (thinkingTime < 10);
    }
}
