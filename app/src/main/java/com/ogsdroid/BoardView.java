package com.ogsdroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ogs.OGSGameConnection;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class BoardView extends View {
    private static final String TAG = "BoardView";
    public Board board;
    public Clock clockWhite, clockBlack;
    public String phase;
    public OGSGameConnection gameConnection;
    public Timer timer;
    public boolean blacksMove;
    public String zoom;
    public boolean zoomed = false;
    private Bitmap background;
    private Rect r, r2;
    private Matrix m = new Matrix();
    private float mx, my;

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    private void init() {
        background = BitmapFactory.decodeResource(getResources(),
                R.drawable.board);
        r = new Rect();
        r2 = new Rect();

        final BoardView bv = this;

        clockWhite = new Clock();
        clockBlack = new Clock();
        phase = "";

        timer = new Timer("OGS board clock timer");
        timer.schedule(
                new TimerTask() {
                    public void run() {
                        if (phase.equals("play")) {
                            if (blacksMove)
                                clockBlack.tick();
                            else
                                clockWhite.tick();
                            bv.postInvalidate();
                        }
                    }
                },
                1000, 1000);


    }

    public void setClockWhite(JSONObject clock) {
        clockWhite.set(clock);
    }

    public void setClockBlack(JSONObject clock) {
        clockBlack.set(clock);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        canvas.concat(m);

        r.set(0, 0, background.getWidth(), background.getHeight());
        r2.set(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(background, null, r2, null);

        int dimension = Math.min(canvas.getWidth(), canvas.getHeight());

        board.draw(canvas, dimension);

        canvas.restore();

        clockWhite.draw(canvas, false, "White", 0, dimension, canvas.getWidth() / 2, canvas.getHeight() - dimension);
        clockBlack.draw(canvas, true, "Black", canvas.getWidth() / 2, dimension, canvas.getWidth() / 2, canvas.getHeight() - dimension);

    }

    public void unZoom() {
        mx = 0;
        my = 0;
        zoomed = false;
        m.reset();
        invalidate();
    }

    private boolean shouldZoom() {
        if (zoom.equals("1") && board.cols >= 9)
            return true;
        if (zoom.equals("2") && board.cols >= 13)
            return true;
        if (zoom.equals("3") && board.cols >= 19)
            return true;
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (phase.equals("play")) {
            if (shouldZoom()) {
                if (zoomed) {
                    if ((event.getAction() & MotionEvent.ACTION_UP) > 0) {
                        Log.d(TAG, "placing stone at " + event.toString());
                        float x = event.getX();
                        float y = event.getY();
                        float[] pts = {x, y};

                        Matrix inverse = new Matrix();
                        m.invert(inverse);
                        inverse.mapPoints(pts);

                        Log.d(TAG, String.format("got touch at %f,%f maps to %f,%f", x, y, pts[0], pts[1]));

                        String moveStr = board.addStoneAtTouch(getWidth(),
                                getHeight(), pts[0], pts[1]);
                        Log.d(TAG, "moveStr = " + moveStr);
                        if (gameConnection != null)
                            gameConnection.makeMove(moveStr);

                        unZoom();
                    }
                } else {
                    if ((event.getAction() & MotionEvent.ACTION_MOVE) > 0) {
                        mx = event.getX();
                        my = event.getY();

                        m.reset();
                        m.postTranslate(-mx / 2, -my / 2);
                        m.postScale(2, 2);

                        invalidate();
                    } else if ((event.getAction() & MotionEvent.ACTION_UP) > 0) {
                        zoomed = true;
                    }
                }
            } else {
                if ((event.getAction() & MotionEvent.ACTION_UP) > 0) {
                    String moveStr = board.addStoneAtTouch(getWidth(),
                            getHeight(), event.getX(), event.getY());
                    Log.d(TAG, "moveStr = " + moveStr);
                    if (gameConnection != null)
                        gameConnection.makeMove(moveStr);
                }
            }
        } else if (phase.equals("stone removal")) {
            if ((event.getAction() & MotionEvent.ACTION_UP) > 0) {
                String coords = board.stoneRemovalAtTouch(getWidth(),
                        getHeight(), event.getX(), event.getY());
                Log.d(TAG, "coords=" + coords);
                if (gameConnection != null)
                    gameConnection.removeStones(coords, true);
            }
        } else {
            Log.d(TAG, "unknown phase " + phase);
        }
        return true;
    }

}
