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
        board[5][3] = 1;
        board[5][4] = 1;
        board[5][5] = 1;
        board[6][3] = 2;
        board[6][4] = 2;
        board[6][5] = 2;
    }

    private void drawGrid(Canvas c) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        float horizSpacing = dims / (cols + 1);
        float vertSpacing = dims / (rows + 1);

        Paint p = new Paint();
        p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(1);

        // Vertical lines
        for (int i = 0; i < cols; i++) {
            float x = (i + 1) * horizSpacing;
            float y = vertSpacing;
            float fx = x;
            Log.w("myApp", String.format("fx=%f", fx));
            float fy = rows * vertSpacing;
            c.drawLine(x, y, fx, fy, p);

        }
        // Horizontal lines
        for (int j = 0; j < rows; j++) {
            float x = horizSpacing;
            float y = (j + 1) * vertSpacing;
            float fx = cols * horizSpacing;
            Log.w("myApp",
                    String.format("fx=%f width=%d horizSpacing=%f",
                            fx, dims, horizSpacing));
            float fy = y;
            c.drawLine(x, y, fx, fy, p);
        }
    }

    private void drawStone(Canvas c, int i, int j, int v) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        int horizSpacing = dims / (cols + 1);
        int vertSpacing = dims / (rows + 1);

        int midx = j * horizSpacing;
        int midy = i * vertSpacing;

        int x = midx - horizSpacing / 2;
        int fx = midx + horizSpacing / 2;
        int y = midy - vertSpacing / 2;
        int fy = midy + vertSpacing / 2;
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
}
