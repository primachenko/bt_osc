package org.home.bluetoothoscilloscope;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;

/**
 * Created by prima on 02.04.2016.
 */
class DrawThread extends Thread {
    private float[] coord;
    private boolean running = false;
    private SurfaceHolder surfaceHolder;

    public DrawThread(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        Canvas canvas;
        Paint p = new Paint();
        p.setColor(Color.RED);
        while (running) {

            canvas = null;
            try {
                coord = MainActivity.getPoints();
                canvas = surfaceHolder.lockCanvas(null);
                if (canvas == null)
                    continue;
                //отрисовка
                canvas.drawColor(Color.BLACK);
                p.setColor(Color.BLUE);
                p.setAlpha(64);
                p.setStrokeWidth(1);
                for(int i = 0; i<=20; i++) {
                    canvas.drawLine(0, i*canvas.getHeight()/20, canvas.getWidth(), i*canvas.getHeight()/20, p);
                    canvas.drawLine(i*canvas.getWidth()/20, 0, i*canvas.getWidth()/20, canvas.getHeight(), p);
                }
                canvas.drawLine(0, canvas.getHeight()-1, canvas.getWidth(), canvas.getHeight()-1, p);
                canvas.drawLine(canvas.getWidth()-1, 0, canvas.getWidth()-1, canvas.getHeight(), p);
                p.setColor(Color.BLUE);
                p.setStrokeWidth(2);
                p.setAlpha(128);
                p.setTextSize(20);
                if(coord[0]==0) canvas.drawText("Ожидание подключения", 20,20,p);
                canvas.drawLine(0, 0, 0, canvas.getHeight(), p);
                canvas.drawLine(0, canvas.getHeight()/2, canvas.getWidth(), canvas.getHeight()/2, p);
                p.setColor(Color.BLUE);
                p.setStrokeWidth(2);
                p.setAlpha(255);
                for (int i= 0; i< coord.length-2&&i<canvas.getWidth();i++) {
                    //if (coord[i] == 0) break;
                    //canvas.drawPoint(i,(canvas.getHeight()/2)-(float) (coord[i]*canvas.getHeight()*0.4), p);
                    canvas.drawLine(i,(canvas.getHeight()/2)-(float) (coord[i]*canvas.getHeight()*0.5),i+1,(canvas.getHeight()/2)-(float) (coord[i+1]*canvas.getHeight()*0.5),p);
                }
                try{
                    wait();
                } catch (Exception e){};
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

}

