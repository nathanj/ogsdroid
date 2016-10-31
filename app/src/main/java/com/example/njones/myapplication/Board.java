package com.example.njones.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

/**
 * Created by njones on 10/26/16.
 * <p>
 * Represents the go board.
 */

class Board {
    public int board[][];
    private int rows = 9, cols = 9;

    Board() {
        board = new int[rows][cols];
//        board[0][0] = 1;
//        board[12][12] = 2;
//        board[5][3] = 1;
//        board[5][4] = 1;
//        board[5][5] = 1;
//        board[6][3] = 2;
//        board[6][4] = 2;
//        board[6][5] = 2;
    }

    private void drawGrid(Canvas c) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        float spacing = dims / (Math.max(cols, rows) + 1);

        Paint p = new Paint();
        p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(1);

        // Vertical lines
        for (int i = 0; i < cols; i++) {
            float x = (i + 1) * spacing;
            float y = spacing;
            float fx = x;
            float fy = rows * spacing;
            c.drawLine(x, y, fx, fy, p);

        }
        // Horizontal lines
        for (int j = 0; j < rows; j++) {
            float x = spacing;
            float y = (j + 1) * spacing;
            float fx = cols * spacing;
            float fy = y;
            c.drawLine(x, y, fx, fy, p);
        }
    }

    private void drawStone(Canvas c, int i, int j, int v, boolean last) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        float spacing = dims / (Math.max(cols, rows) + 1);

        Log.w("myApp", String.format("c w=%d h=%d", c.getWidth(), c.getHeight()));

        float midx = (j + 1) * spacing;
        float midy = (i + 1) * spacing;

        float x = midx - spacing / 2;
        float fx = midx + spacing / 2;
        float y = midy - spacing / 2;
        float fy = midy + spacing / 2;
        RectF r = new RectF();
        r.set(x, y, fx, fy);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (v == 1)
            p.setARGB(255, 255, 255, 255);
        else
            p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(r, p);
        if (v == 1)
            p.setARGB(255, 30, 30, 30);
        else
            p.setARGB(255, 30, 30, 30);
        p.setStrokeWidth(1);
        p.setStyle(Paint.Style.STROKE);
        c.drawOval(r, p);

        if (last) {
            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            if (v == 1)
                p.setARGB(255, 0, 0, 0);
            else
                p.setARGB(255, 255, 255, 255);
            p.setStrokeWidth(2);
            p.setStyle(Paint.Style.STROKE);
            float x2 = midx - spacing / 4;
            float fx2 = midx + spacing / 4;
            float y2 = midy - spacing / 4;
            float fy2 = midy + spacing / 4;
            r.set(x2, y2, fx2, fy2);
            c.drawOval(r, p);
        }
    }

    private void drawStones(Canvas c) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] != 0) {
                    drawStone(c, i, j, board[i][j], i == lastY && j == lastX);
                }
            }
        }
    }

    public void draw(Canvas c) {
        drawGrid(c);
        drawStones(c);
    }

    public String addStoneAtTouch(int width, int height, float x, float y) {
        Log.w("myApp", String.format("w=%d h=%d x=%f y=%f", width, height, x, y));
        int dims = Math.min(width, height);
        float spacing = dims / (Math.max(cols, rows) + 1);

        int sx = (int) ((x - spacing / 2) / spacing);
        int sy = (int) ((y - spacing / 2) / spacing);
        if (sx >= cols)
            return "";
        if (sy >= rows)
            return "";
        board[sy][sx] = 1;
        Log.w("myApp", String.format("created stone at %d/%d", sx, sy));
        String moveStr = "";
        int c1 = (int)'a' + sx;
        int c2 = (int)'a' + sy;
        Log.w("myApp", String.format("c1=%d c2=%d", c1, c2));
        moveStr = moveStr + (char) c1;
        moveStr = moveStr + (char) c2;
        Log.w("myApp", "moveStr = " + moveStr);
        return moveStr;
    }

    private int oppositeColor(int color) {
        if (color == 1)
            return 2;
        else
            return 1;
    }

    public void addStone(int x, int y) {
        lastV = oppositeColor(lastV);

        board[y][x] = lastV;
        lastY = y;
        lastX = x;

        captureGroup(x - 1, y, oppositeColor(lastV));
        captureGroup(x + 1, y, oppositeColor(lastV));
        captureGroup(x, y - 1, oppositeColor(lastV));
        captureGroup(x, y + 1, oppositeColor(lastV));
    }

    public void addStone(String coords) {
	    char c1 = coords.charAt(0);
	    char c2 = coords.charAt(1);
        lastV = oppositeColor(lastV);
	    board[(int)c2][(int)c1] = lastV;
	    Log.w("myApp", "added stone at " + coords);
    }

    private boolean hasLiberty(int x, int y, int color) {
        // edges
        if (y < 0 || y >= rows)
            return false;
        if (x < 0 || x >= cols)
            return false;
        // open space
        if (board[y][x] == 0)
            return true;
        // opposite color stone
        if (board[y][x] != color)
            return false;
        return
            hasLiberty(x - 1, y) ||
            hasLiberty(x + 1, y) ||
            hasLiberty(x, y - 1) ||
            hasLiberty(x, y + 1);
    }

    private void captureStones(int x, int y, int color) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        if (board[y][x] == color) {
            board[y][x] = 0;
            captureStones(x - 1, y);
            captureStones(x + 1, y);
            captureStones(x, y - 1);
            captureStones(x, y + 1);
        }
    }

    private void captureGroup(int x, int y, int color) {
        if (!hasLiberty(x, y, color)) {
            captureStones(x, y, color);
        }
    }

    private int lastY, lastX, lastV = 1;
}
