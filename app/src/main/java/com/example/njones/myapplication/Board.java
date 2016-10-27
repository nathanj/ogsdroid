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

    Board() {
        board = new int[19][19];
        board[10][10] = 1;
        board[9][9] = 1;
        board[5][3] = 1;
    }

    public void draw(Canvas c, Bitmap b) {


        Paint p2 = new Paint();
        p2.setARGB(255, 0, 0, 0);
        p2.setStrokeWidth(2);

        for (int i = 0; i < 19; i++)
        {
            c.drawLine(15 + c.getWidth()/19 * i, 15, 15 + c.getWidth()/19 * i, 700, p2);
            c.drawLine(15, 15 + c.getWidth()/19 * i, 700, 15 + c.getWidth()/19 * i, p2);
        }

        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                if (board[i][j] == 1) {
                    Rect r3 = new Rect();
                    int x = i * c.getWidth()/19 + 15 - c.getWidth()/19/2;
                    int y = j * c.getWidth()/19 + 15 - c.getWidth()/19/2;
                    r3.set(x, y, x + c.getWidth()/19, y + c.getWidth()/19);
                    c.drawBitmap(b, null, r3, null);
                }
            }
        }



    }
}
