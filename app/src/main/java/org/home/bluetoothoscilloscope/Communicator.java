package org.home.bluetoothoscilloscope;

/**
 * Created by prima on 01.04.2016.
 */
interface Communicator {
    void startCommunication();
    void write(String message);
    void stopCommunication();
}
