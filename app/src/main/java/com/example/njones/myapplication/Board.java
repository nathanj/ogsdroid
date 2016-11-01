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
    private static int EMPTY             = 0;
    private static int BLACK             = 1;
    private static int WHITE             = 2;
    private static int COLOR             = (BLACK|WHITE);
    private static int MARKED            = 0x80;
    private static int WHITE_TERRITORY   = 0x40;
    private static int BLACK_TERRITORY   = 0x20;
    private static int NEUTRAL_TERRITORY = 0x10;
    private static int TERRITORY         = (BLACK_TERRITORY|WHITE_TERRITORY);

    public int board[][];
    private int rows = 13, cols = 13;

    Board() {
        board = new int[rows][cols];
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

    private void drawTerritory(Canvas c, int i, int j, int v) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        float spacing = dims / (Math.max(cols, rows) + 1);

        float midx = (j + 1) * spacing;
        float midy = (i + 1) * spacing;

        float x = midx - spacing / 4;
        float fx = midx + spacing / 4;
        float y = midy - spacing / 4;
        float fy = midy + spacing / 4;
        RectF r = new RectF();
        r.set(x, y, fx, fy);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (v == WHITE_TERRITORY)
            p.setARGB(255, 255, 255, 255);
        else
            p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(r, p);
    }

    private void drawStone(Canvas c, int i, int j, int v, boolean last) {
        int dims = Math.min(c.getWidth(), c.getHeight());
        float spacing = dims / (Math.max(cols, rows) + 1);

        float midx = (j + 1) * spacing;
        float midy = (i + 1) * spacing;

        float x = midx - spacing / 2;
        float fx = midx + spacing / 2;
        float y = midy - spacing / 2;
        float fy = midy + spacing / 2;
        RectF r = new RectF();
        r.set(x, y, fx, fy);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if ((v & COLOR) == WHITE)
            p.setARGB(255, 255, 255, 255);
        else
            p.setARGB(255, 0, 0, 0);
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(r, p);
        if ((v & COLOR) == WHITE)
            p.setARGB(255, 30, 30, 30);
        else
            p.setARGB(255, 30, 30, 30);
        p.setStrokeWidth(1);
        p.setStyle(Paint.Style.STROKE);
        c.drawOval(r, p);
        if ((v & MARKED) == MARKED) {
            p.setARGB(255, 255, 30, 30);
            p.setStrokeWidth(3);
            p.setStyle(Paint.Style.STROKE);
            c.drawOval(r, p);
        }

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
                if ((board[i][j] & COLOR) > 0) {
                    drawStone(c, i, j, board[i][j], i == lastY && j == lastX);
                }
                if ((board[i][j] & TERRITORY) > 0) {
                    drawTerritory(c, i, j, board[i][j]);
                }
            }
        }
    }

    public void draw(Canvas c) {
        drawGrid(c);
        drawStones(c);
    }

    void unmarkGroup(int x, int y, int color) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        // already unmarked, nothing to do
        if ((board[y][x] & MARKED) != MARKED)
            return;
        if (board[y][x] == color) {
            board[y][x] &= ~MARKED;
            unmarkGroup(x - 1, y, color);
            unmarkGroup(x + 1, y, color);
            unmarkGroup(x, y - 1, color);
            unmarkGroup(x, y + 1, color);
        }
    }

    void markGroup(int x, int y, int color) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        // already marked, nothing to do
        if ((board[y][x] & MARKED) == MARKED)
            return;
        if (board[y][x] == color) {
            board[y][x] |= MARKED;
            markGroup(x - 1, y, color);
            markGroup(x + 1, y, color);
            markGroup(x, y - 1, color);
            markGroup(x, y + 1, color);
        }
    }

    void int setTerritory(int x, int y, int territory) {
        if (y < 0 || y >= rows)
            return 0;
        if (x < 0 || x >= cols)
            return 0;
        // marked empty space, convert to territory and keep traversing
        if ((board[y][x] & (MARKED|EMPTY)) == (MARKED|EMPTY)) {
            board[y][x] == territory;
            setTerritory(x - 1, y, territory);
            setTerritory(x + 1, y, territory);
            setTerritory(x, y - 1, territory);
            setTerritory(x, y + 1, territory);
        }
    }

    void int determineTerritory(int x, int y) {
        if (y < 0 || y >= rows)
            return 0;
        if (x < 0 || x >= cols)
            return 0;
        // removed stone, treat as empty
        if ((board[y][x] & MARKED) == MARKED)
            return 0;
        // stone, return that color
        if (board[y][x] != 0)
            return board[y][x];
        // empty space, mark and keep traversing
        board[y][x] |= MARKED;
        return
            determineTerritory(x - 1, y) |
            determineTerritory(x + 1, y) |
            determineTerritory(x, y - 1) |
            determineTerritory(x, y + 1);
    }

    void markTerritory() {
        // remove all previous territory markers
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[j][i] &= ~WHITE_TERRITORY;
                board[j][i] &= ~BLACK_TERRITORY;
                board[j][i] &= ~NEUTRAL_TERRITORY;
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[j][i] == EMPTY) {
                    int mask = determineTerritory(x, y);
                    if (mask == WHITE)
                        setTerritory(x, y, WHITE_TERRITORY);
                    else if (mask == BLACK)
                        setTerritory(x, y, BLACK_TERRITORY);
                    else
                        setTerritory(x, y, NEUTRAL_TERRITORY);
                }
            }
        }
    }

    public String stoneRemovalAtTouch(int width, int height, float x, float y) {
        Log.w("myApp", String.format("w=%d h=%d x=%f y=%f", width, height, x, y));
        int dims = Math.min(width, height);
        float spacing = dims / (Math.max(cols, rows) + 1);

        int sx = (int) ((x - spacing / 2) / spacing);
        int sy = (int) ((y - spacing / 2) / spacing);
        if (sx < 0 || sx >= cols)
            return "";
        if (sx < 0 || sy >= rows)
            return "";
        int color = board[sy][sx];
        if ((color & MARKED) == MARKED)
            unmarkGroup(sx, sy, color);
        else
            markGroup(sx, sy, color);
        markTerritory();
    }

    public String addStoneAtTouch(int width, int height, float x, float y) {
        Log.w("myApp", String.format("w=%d h=%d x=%f y=%f", width, height, x, y));
        int dims = Math.min(width, height);
        float spacing = dims / (Math.max(cols, rows) + 1);

        int sx = (int) ((x - spacing / 2) / spacing);
        int sy = (int) ((y - spacing / 2) / spacing);
        if (sx < 0 || sx >= cols)
            return "";
        if (sx < 0 || sy >= rows)
            return "";
        board[sy][sx] = 1;
        Log.w("myApp", String.format("created stone at %d/%d", sx, sy));
        String moveStr = "";
        int c1 = (int) 'a' + sx;
        int c2 = (int) 'a' + sy;
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
        board[(int) c2][(int) c1] = lastV;
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
        // already visited
        if ((board[y][x] & MARKED) > 0)
            return false;
        // mark stone as visited
        board[y][x] |= MARKED;
        return
                hasLiberty(x - 1, y, color) ||
                        hasLiberty(x + 1, y, color) ||
                        hasLiberty(x, y - 1, color) ||
                        hasLiberty(x, y + 1, color);
    }

    private void captureStones(int x, int y, int color) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        if (board[y][x] == color) {
            board[y][x] = 0;
            captureStones(x - 1, y, color);
            captureStones(x + 1, y, color);
            captureStones(x, y - 1, color);
            captureStones(x, y + 1, color);
        }
    }

    private void captureGroup(int x, int y, int color) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        if (board[y][x] == color) {
            boolean has = hasLiberty(x, y, color);
            Log.w("capture", "x = " + x + ", y = " + y + ", hasLiberty = " + has);
            // remove marks
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    board[j][i] &= ~MARKED;
                }
            }
            if (!has) {
                Log.w("capture", "capturing stones");
                captureStones(x, y, color);
            }
        }

    }

    private int lastY, lastX, lastV = 1;
}
