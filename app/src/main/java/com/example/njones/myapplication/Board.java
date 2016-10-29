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
    private int rows = 13, cols = 13;

    Board() {
        board = new int[rows][cols];
        board[0][0] = 1;
        board[12][12] = 2;
        board[5][3] = 1;
        board[5][4] = 1;
        board[5][5] = 1;
        board[6][3] = 2;
        board[6][4] = 2;
        board[6][5] = 2;
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

    private void drawStone(Canvas c, int i, int j, int v) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        float spacing = dims / (Math.max(cols, rows) + 1);

        Log.w("myApp", String.format("c w=%d h=%d", c.getWidth(), c.getHeight()));

        float midx = (j+1) * spacing;
        float midy = (i+1) * spacing;

        float x = midx - spacing / 2;
        float fx = midx + spacing / 2;
        float y = midy - spacing / 2;
        float fy = midy + spacing / 2;
        RectF r = new RectF();
        r.set(x, y, fx, fy);

        if (v == 1) {
            Paint p = new Paint();
            p.setARGB(255, 255, 255, 255);
            p.setStrokeWidth(0);
            p.setStyle(Paint.Style.FILL);
            c.drawOval(r, p);
            p.setARGB(255, 30, 30, 30);
            p.setStrokeWidth(1);
            p.setStyle(Paint.Style.STROKE);
            c.drawOval(r, p);
        } else {
            Paint p = new Paint();
            p.setARGB(255, 0, 0, 0);
            p.setStrokeWidth(0);
            p.setStyle(Paint.Style.FILL);
            c.drawOval(r, p);
            p.setARGB(255, 30, 30, 30);
            p.setStrokeWidth(1);
            p.setStyle(Paint.Style.STROKE);
            c.drawOval(r, p);
        }
    }

    private void drawStones(Canvas c) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] != 0) {
                    drawStone(c, i, j, board[i][j]);
                }
            }
        }
    }

    public void draw(Canvas c) {
        drawGrid(c);
        drawStones(c);
    }

    public void addStone(int width, int height, float x, float y) {
        Log.w("myApp", String.format("w=%d h=%d x=%f y=%f", width, height, x, y));
        int dims = Math.min(width, height);
        float spacing = dims / (Math.max(cols, rows) + 1);

        int sx = (int) ((x - spacing / 2) / spacing);
        int sy = (int) ((y - spacing / 2) / spacing);
        if (sx >= cols)
            return;
        if (sy >= rows)
            return;
        board[sy][sx] = 1;
        Log.w("myApp", String.format("created stone at %d/%d", sx, sy));
    }
}
