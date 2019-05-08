package org.home.bluetoothoscilloscope;

import android.bluetooth.BluetoothSocket;

/**
 * Created by prima on 01.04.2016.
 */
interface CommunicatorService {
    Communicator createCommunicatorThread(BluetoothSocket socket);
}
