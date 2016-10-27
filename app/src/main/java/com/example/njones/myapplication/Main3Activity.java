package com.example.njones.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class Main3Activity extends AppCompatActivity implements SurfaceHolder.Callback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

    }

    public void surfaceCreated(SurfaceHolder holder) {
        Board board = new Board();

        Canvas c = holder.lockCanvas();

        Bitmap bmpIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.board);

        Paint p = new Paint();
        p.setARGB(128, 128, 128, 128);
        c.drawLine(10, 10, 80, 80, p);
        //c.drawBitmap(bmpIcon, 50, 50, null);
        Rect r = new Rect();
        r.set(0, 0, bmpIcon.getWidth(), bmpIcon.getHeight());
        Rect r2 = new Rect();
        r2.set(0, 0, c.getWidth(), c.getHeight());
        c.drawBitmap(bmpIcon, null, r2, null);

        Bitmap b2 = BitmapFactory.decodeResource(getResources(),
                R.drawable.inner);

        Bitmap b = BitmapFactory.decodeResource(getResources(),
                R.drawable.stone_1);

        board.draw(c, b);

//        c.drawLine(15, 15, 215, 15, p2);
//        c.drawLine(15, 45, 215, 45, p2);
//        c.drawLine(15, 75, 215, 75, p2);
//        c.drawLine(15, 15, 15, 215, p2);
//        c.drawLine(45, 15, 45, 215, p2);
//        c.drawLine(75, 15, 75, 215, p2);


        holder.unlockCanvasAndPost(c);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
