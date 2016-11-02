package com.example.njones.myapplication;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

/**
 * Created by njones on 10/26/16.
 * <p>
 * Represents the go board.
 */

class Board {
    private static int EMPTY = 0x00;
    private static int BLACK = 0x01;
    private static int WHITE = 0x02;
    private static int COLOR = (BLACK | WHITE);
    private static int MARKED = 0x10;
    private static int REMOVED = 0x20;
    private static int NEUTRAL_TERRITORY = 0x100;
    private static int BLACK_TERRITORY = 0x200;
    private static int WHITE_TERRITORY = 0x400;
    private static int TERRITORY = (BLACK_TERRITORY | WHITE_TERRITORY);

    public int board[][];
    private int rows = 9, cols = 9;

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
        if (v == WHITE_TERRITORY || v == (REMOVED | BLACK))
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
        boolean removed = (v & REMOVED) == REMOVED;
        int alpha = 255;

        if (removed) {
            alpha = 100;
        }

        float midx = (j + 1) * spacing;
        float midy = (i + 1) * spacing;

        float x = midx - spacing / 2 + 1;
        float fx = midx + spacing / 2 - 1;
        float y = midy - spacing / 2 + 1;
        float fy = midy + spacing / 2 - 1;
        RectF r = new RectF();
        r.set(x, y, fx, fy);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if ((v & COLOR) == WHITE)
            p.setARGB(alpha, 255, 255, 255);
        else
            p.setARGB(alpha, 0, 0, 0);
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(r, p);
        if ((v & COLOR) == WHITE)
            p.setARGB(alpha, 150, 150, 150);
        else
            p.setARGB(alpha, 30, 30, 30);
        p.setStrokeWidth(1);
        p.setStyle(Paint.Style.STROKE);
        c.drawOval(r, p);

        if (last) {
            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            if ((v & COLOR) == WHITE)
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
                if ((board[i][j] & TERRITORY) > 0 || (board[i][j] & REMOVED) > 0) {
                    drawTerritory(c, i, j, board[i][j]);
                }
            }
        }
    }

    public void draw(Canvas c) {
        drawGrid(c);
        drawStones(c);
    }

    void unremoveGroup(int x, int y, int color) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        // already unremoved, nothing to do
        if ((board[y][x] & REMOVED) != REMOVED)
            return;
        if (board[y][x] == color) {
            board[y][x] &= ~REMOVED;
            unremoveGroup(x - 1, y, color);
            unremoveGroup(x + 1, y, color);
            unremoveGroup(x, y - 1, color);
            unremoveGroup(x, y + 1, color);
        }
    }

    String removeGroup(int x, int y, int color) {
        //Log.w("remov", "checking marking group at x=" + x + " y=" + y + " color=" + color);
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        // already removed, nothing to do
        if ((board[y][x] & REMOVED) == REMOVED)
            return;
        if (board[y][x] == color) {
            //Log.w("remov", "really marking group at x=" + x + " y=" + y + " color=" + color);

            board[y][x] |= REMOVED;
            removeGroup(x - 1, y, color);
            removeGroup(x + 1, y, color);
            removeGroup(x, y - 1, color);
            removeGroup(x, y + 1, color);
        }
    }

    void setTerritory(int x, int y, int territory) {
        if (y < 0 || y >= rows)
            return;
        if (x < 0 || x >= cols)
            return;
        // marked empty space, convert to territory and keep traversing
        if ((board[y][x] & (MARKED | COLOR)) == (MARKED | EMPTY)) {
            board[y][x] = territory;
            setTerritory(x - 1, y, territory);
            setTerritory(x + 1, y, territory);
            setTerritory(x, y - 1, territory);
            setTerritory(x, y + 1, territory);
        }
        // marked removed stone, remove mark and keep traversing
        if ((board[y][x] & (MARKED | REMOVED)) == (MARKED | REMOVED)) {
            board[y][x] &= ~MARKED;
            setTerritory(x - 1, y, territory);
            setTerritory(x + 1, y, territory);
            setTerritory(x, y - 1, territory);
            setTerritory(x, y + 1, territory);
        }
    }

    int determineTerritory(int x, int y) {
        if (y < 0 || y >= rows)
            return 0;
        if (x < 0 || x >= cols)
            return 0;
        // already marked
        if ((board[y][x] & MARKED) == MARKED)
            return 0;
        // stone, return that color
        if ((board[y][x] & REMOVED) != REMOVED && (board[y][x] & COLOR) > 0) {
            Log.w("trace", String.format("returning y=%d x=%d color=%x", y, x, board[y][x]));
            return board[y][x];
        }
        // removed stone and empty space, mark and keep traversing
        board[y][x] |= MARKED;
        return
                determineTerritory(x - 1, y) |
                        determineTerritory(x + 1, y) |
                        determineTerritory(x, y - 1) |
                        determineTerritory(x, y + 1);
    }

    void markTerritory() {
        Log.w("removal", "marking terrotiry");
        // remove all previous territory markers
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[j][i] &= ~WHITE_TERRITORY;
                board[j][i] &= ~BLACK_TERRITORY;
                board[j][i] &= ~NEUTRAL_TERRITORY;
            }
        }
        traceBoard("before determine territory");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[j][i] == EMPTY) {
                    int mask = determineTerritory(i, j);
                    Log.w("removal", String.format("r=%d c=%d mask=%x", j, i, mask));
                    traceBoard("before setting territory");
                    if (mask == WHITE)
                        setTerritory(i, j, WHITE_TERRITORY);
                    else if (mask == BLACK)
                        setTerritory(i, j, BLACK_TERRITORY);
                    else
                        setTerritory(i, j, NEUTRAL_TERRITORY);
                    traceBoard("in territory");
                }
            }
        }
    }

    void traceBoard(String header) {
        for (int r = 0; r < rows; r++) {
            Log.w("trace", String.format("%30s: %3x %3x %3x %3x %3x %3x %3x %3x %3x",
                    header,
                    board[r][0],
                    board[r][1],
                    board[r][2],
                    board[r][3],
                    board[r][4],
                    board[r][5],
                    board[r][6],
                    board[r][7],
                    board[r][8]
            ));
        }
    }

    public void stoneRemoval(String coords, boolean removed) {
        for (int i = 0; i < coords.length(); i++) {
            int sx = (int) coords.charAt(i) - (int) 'a';
            int sy = (int) coords.charAt(i+1) - (int) 'a';
            if (removed)
                board[sy][sx] |= REMOVED;
            else
                board[sy][sx] &= ~REMOVED;
        }
        markTerritory();
    }

    String toStringCoords(int x, int y) {
        StringBuilder s(2);
        s.append((char) (x + (int) 'a'));
        s.append((char) (y + (int) 'a'));
        return s.toString();
    }

    String markedToCoords() {
        StringBuilder s;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if ((board[j][i] & MARKED) == MARKED) {
                    s.append(toStringCoords(i, j));
                }
            }
        }
        return s.toString();
    }

    void removeMarked() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[j][i] &= ~MARKED;
            }
        }
    }

    public String stoneRemovalAtTouch(int width, int height, float x, float y) {
        Log.w("removal", String.format("w=%d h=%d x=%f y=%f", width, height, x, y));
        int dims = Math.min(width, height);
        float spacing = dims / (Math.max(cols, rows) + 1);

        int sx = (int) ((x - spacing / 2) / spacing);
        int sy = (int) ((y - spacing / 2) / spacing);
        if (sx < 0 || sx >= cols)
            return "";
        if (sx < 0 || sy >= rows)
            return "";
        int color = board[sy][sx];

        removeGroup(sx, sy, color);
        String coords = markedToCoords();
        removeMarked();
        return coords;




        //traceBoard("before mark");
        //if ((color & REMOVED) == REMOVED)
        //    unremoveGroup(sx, sy, color);
        //else
        //    removeGroup(sx, sy, color);
        //traceBoard("after mark");
        //markTerritory();
        //traceBoard("after territyroy");
        return "";
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

    private int lastY, lastX, lastV = 2;
}
