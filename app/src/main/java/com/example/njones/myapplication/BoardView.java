package com.example.njones.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.ogs.OGS;
import com.ogs.OGSGameConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class BoardView extends View {
    private static final String TAG = "BoardView";
    public Board board;
    private Bitmap background;
    private Rect r, r2;
    public Clock clockWhite, clockBlack;

    public String phase;
    public OGSGameConnection gameConnection;

    public Timer timer;
    public boolean blacksMove;

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Board getBoard() {
        return board;
    }

    private void init() {
        background = BitmapFactory.decodeResource(getResources(),
                R.drawable.board);
        r = new Rect();
        r2 = new Rect();

        final BoardView bv = this;

        clockWhite = new Clock();
        clockBlack = new Clock();

        timer = new Timer("OGS board clock timer");
        timer.schedule(
                new TimerTask() {
                    public void run() {
                        if (blacksMove)
                            clockBlack.tick();
                        else
                            clockWhite.tick();
                        bv.postInvalidate();
                    }
                },
                1000, 1000);
    }

    public void initBoard(int rows, int cols) {
        board = new Board(rows, cols);
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

        r.set(0, 0, background.getWidth(), background.getHeight());
        r2.set(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(background, null, r2, null);

        board.draw(canvas);
        clockWhite.draw(canvas, 500, 500);
        clockBlack.draw(canvas, 300, 300);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (phase.equals("play")) {
            if ((event.getAction() & MotionEvent.ACTION_POINTER_DOWN) > 0) {
                String moveStr = board.addStoneAtTouch(getWidth(),
                        getHeight(), event.getX(), event.getY());
                Log.w(TAG, moveStr);
                if (gameConnection != null)
                    gameConnection.makeMove(moveStr);
            }
        } else if (phase.equals("stone removal")) {
            if ((event.getAction() & MotionEvent.ACTION_POINTER_DOWN) > 0) {
                String coords = board.stoneRemovalAtTouch(getWidth(),
                        getHeight(), event.getX(), event.getY());
                Log.w(TAG, "coords=" + coords);
                if (gameConnection != null)
                    gameConnection.removeStones(coords, true);
            }
        } else {
            Log.w(TAG, "unknown phase " + phase);
        }
        return true;
    }

}
