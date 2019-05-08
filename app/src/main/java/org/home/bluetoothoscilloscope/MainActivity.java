package org.home.bluetoothoscilloscope;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;


public class MainActivity extends Activity {
    public static float[] points = new float[1200];
    public final static String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";
    private BluetoothAdapter bluetoothAdapter;
    private ServerThread serverThread;
    private int count;
    private boolean stopflag = false;
    private boolean stopflag1 = true;


    private final CommunicatorService communicatorService = new CommunicatorService() {
        @Override
        public Communicator createCommunicatorThread(BluetoothSocket socket) {
            return new CommunicatorImpl(socket, new CommunicatorImpl.CommunicationListener() {
                @Override
                public void onMessage(final String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String TMP = message;
                            try {
                                setPoints(Float.valueOf(TMP));
                            } catch (NumberFormatException e){Log.d("IP SP","NFE");setPoints(0);}
                            Log.d("SetPoints", "Set value "+TMP);
                            TMP = "";
                            try{
                                notifyAll();
                               wait();//ждет
                            } catch(Exception e) {}
                        }
                    });
                }
            });
        }
    };

    synchronized static float[] getPoints(){
        return points;
    }

    synchronized private void setPoints(float f) {
        float[] temp = new float[points.length];
        for(int i=0; i<points.length-2;i++)
            temp[i+1]=points[i];
        temp[0] = f;
        points = temp;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        for(int i=0; i<points.length-1;i++)
            points[i]=0;
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new DrawView(this));
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter.isEnabled()==false&&!stopflag) {
            bluetoothAdapter.enable();
            stopflag = true;
        }
        if(stopflag1) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivity(i);
            stopflag1 = false;
        }
    }

   @Override
    public void onPause() {
        super.onPause();
        if (serverThread != null) serverThread.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        serverThread = new ServerThread(communicatorService);
        serverThread.start();
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        // super.onBackPressed();
        //openQuitDialog();
    }

    private void onGen() {
        Intent intent = new Intent(this, GenerateActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                count++;
                if(count > 7) {
                    Toast.makeText(this, "Режим сервера", Toast.LENGTH_LONG).show();
                    onGen();
                }
                return false;
        }
        return false;
    }
}
