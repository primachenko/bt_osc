package org.home.bluetoothoscilloscope;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prima on 02.04.2016.
 */
public class GenerateActivity extends ListActivity implements SeekBar.OnSeekBarChangeListener {

    public final static String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";

    private class WriteTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... args) {
            try {
                clientThread.getCommunicator().write(args[0]);
            } catch (Exception e) {
                Log.d("MainActivity", e.getClass().getSimpleName() + " " + e.getLocalizedMessage());
            }
            return null;
        }
    }



    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver discoverDevicesReceiver;
    private BroadcastReceiver discoveryFinishedReceiver;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<BluetoothDevice>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private TextView textData;
    private EditText textMessage;
    private ProgressDialog progressDialog;
    private ServerThread serverThread;
    private ClientThread clientThread;
    private volatile boolean stop_flag = true;
    private volatile String ACTIVE="";
    volatile int sleep = 15;
    volatile double freq = 1;
    private final CommunicatorService communicatorService = new CommunicatorService() {
        @Override
        public Communicator createCommunicatorThread(BluetoothSocket socket) {
            return new CommunicatorImpl(socket, new CommunicatorImpl.CommunicationListener() {
                @Override
                public void onMessage(final String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textData.setText(textData.getText().toString() + "\n" + message);
                        }
                    });
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);
        try{
            do{

            } while(sleep != 15);

        } catch (InterruptedException e) {Log.d("BT loader", "Fatal Error");
        final SeekBar seekbar1 = (SeekBar)findViewById(R.id.seekBar);
        seekbar1.setOnSeekBarChangeListener(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        while(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
            try{
                Thread.sleep(2000);
            }catch (InterruptedException e) {break;}
        }

        //textData = (TextView) findViewById(R.id.data_text);
     //   textMessage = (EditText) findViewById(R.id.message_text);

        listAdapter = new ArrayAdapter<BluetoothDevice>(getBaseContext(), android.R.layout.simple_list_item_1, discoveredDevices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                final BluetoothDevice device = getItem(position);
                ((TextView) view.findViewById(android.R.id.text1)).setText(device.getName());
                return view;
            }
        };
        setListAdapter(listAdapter);

    }

    public void makeDiscoverable(View view) {
        Intent i = new Intent(
                BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(i);
    }

    public void discoverDevices(View view) {

        discoveredDevices.clear();
        listAdapter.notifyDataSetChanged();

        if (discoverDevicesReceiver == null) {
            discoverDevicesReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                        if (!discoveredDevices.contains(device)) {
                            discoveredDevices.add(device);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
            };
        }

        if (discoveryFinishedReceiver == null) {
            discoveryFinishedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    getListView().setEnabled(true);
                    if (progressDialog != null)
                        progressDialog.dismiss();
                    Toast.makeText(getBaseContext(), "Поиск закончен. Выберите устройство для отправки ообщения.", Toast.LENGTH_LONG).show();
                    unregisterReceiver(discoveryFinishedReceiver);
                }
            };
        }

        registerReceiver(discoverDevicesReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        getListView().setEnabled(false);

        progressDialog = ProgressDialog.show(this, "Поиск устройств", "Подождите...");

        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        bluetoothAdapter.cancelDiscovery();

        if (discoverDevicesReceiver != null) {
            try {
                unregisterReceiver(discoverDevicesReceiver);
            } catch (Exception e) {
                Log.d("MainActivity", "Не удалось отключить ресивер " + discoverDevicesReceiver);
            }
        }

        if (clientThread != null) {
            clientThread.cancel();
        }
        if (serverThread != null) serverThread.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        serverThread = new ServerThread(communicatorService);
        serverThread.start();

        discoveredDevices.clear();
        listAdapter.notifyDataSetChanged();
    }

    public void onListItemClick(ListView parent, View v,
                                int position, long id) {
        if (clientThread != null) {
            clientThread.cancel();
        }

        BluetoothDevice deviceSelected = discoveredDevices.get(position);

        clientThread = new ClientThread(deviceSelected, communicatorService);
        clientThread.start();

        Toast.makeText(this, "Вы подключились к устройству \"" + discoveredDevices.get(position).getName() + "\"", Toast.LENGTH_SHORT).show();
    }


    public void startGen(){
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                stop_flag = false;
                double x = 0.001;
                double y=0;
                while(!stop_flag) {
                    if (x > 30) stop_flag = true;
                    if(!ACTIVE.equals("")) {
                        if (clientThread != null) {
                            switch (ACTIVE) {
                                case "SIN":
                                    y = new BigDecimal(Math.sin(x)).setScale(5, RoundingMode.UP).doubleValue();
                                    break;
                                case "COS":
                                    y = new BigDecimal(Math.cos(x)).setScale(5, RoundingMode.UP).doubleValue();
                                    break;
                                case "SET":
                                    y = new BigDecimal(Math.sin(x)/x).setScale(5, RoundingMode.UP).doubleValue();
                                    break;
                            }
                            x = x + freq/2000;
                            if (!stop_flag) {
                                new WriteTask().execute(String.valueOf(y));
                            }
                        } else {
                            stop_flag = true;
                        }
                        try {
                            Thread.sleep(sleep);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });

    }

    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.sin:
                stop_flag = true;
                ACTIVE = "SIN";
                startGen();
                //выполняем необходимое действие
                break;
            case R.id.cos:
                stop_flag = true;
                ACTIVE = "COS";
                startGen();
                //выполняем необходимое действие
                break;
            case R.id.set:
                stop_flag = true;
                ACTIVE = "SET";
                startGen();
                //выполняем необходимое действие
                break;
        }
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        freq = seekBar.getProgress();
        Toast.makeText(this, ""+freq, Toast.LENGTH_LONG).show();
    }
    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        // super.onBackPressed();
        //openQuitDialog();
    }


}
/**/
