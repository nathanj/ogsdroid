package com.example.njones.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by njones on 10/26/16.
 *
 * Represents the go board.
 */

class Board {
    public int board[][];
    private int rows = 19, cols = 19;

    Board() {
        board = new int[19][19];
        board[10][10] = 1;
        board[9][9] = 1;
        board[5][3] = 1;
    }

    private void drawGrid(Canvas c) {
        int width = c.getWidth();
        int height = c.getHeight();
        int horizSpacing = width/(cols+1);
        int vertSpacing = height/(rows+1);

        Paint p = new Paint();
        p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(1);

        // Vertical lines
        for (int i = 0; i < cols; i++) {
            int x = (i+1) * horizSpacing;
            int y = vertSpacing;
            int fx = x;
            int fy = height - vertSpacing;
            c.drawLine(x, y, fx, fy, p);

        }
        // Horizontal lines
        for (int j = 0; j < rows; j++) {
            int x = horizSpacing;
            int y = (j+1) * vertSpacing;
            int fx = width - horizSpacing;
            int fy = y;
            c.drawLine(x, y, fx, fy, p);
        }
    }

    private void drawStone(Canvas c) {
        int width = c.getWidth();
        int height = c.getHeight();
        int horizSpacing = width/(cols+1);
        int vertSpacing = height/(rows+1);

        int midx = j * horizSpacing;
        int midy = i * vertSpacing;

        int x = midx - horizSpacing;
        int fx = midx + horizSpacing;
        int y = midy - vertSpacing;
        int fy = midy + vertSpacing;

        Paint p = new Paint();
        p.setARGB(255, 255, 255, 255);
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(x, fx, y, fy, p);
        p.setARGB(255, 30, 30, 30);
        p.setStrokeWidth(1);
        p.setStyle(Paint.Style.STROKE);
        c.drawOval(x, fx, y, fy, p);
    }

    private void drawStones(Canvas c) {
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                if (board[i][j] == 1) {
                    drawStone(c, i, j);
                }
            }
    }

    public void draw(Canvas c, Bitmap b) {
        drawGrid(c);
        drawStones(c);
    }
}
