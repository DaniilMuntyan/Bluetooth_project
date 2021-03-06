package com.example.bluetooth_project.connectionStuff.clientPart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.bluetooth_project.ALL.InputAndOutput;
import com.example.bluetooth_project.ALL.PublicStaticObjects;

import java.io.IOException;
import java.util.UUID;

public class ConnectRunnable implements Runnable {

    private final BluetoothSocket socket;
    private final BluetoothDevice device;
    private final BluetoothAdapter bluetoothAdapter;
    private final UUID MY_UUID = UUID.fromString(PublicStaticObjects.getMyUuid());

    public ConnectRunnable(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        this.device = device;
        this.bluetoothAdapter = PublicStaticObjects.getBluetoothAdapter();

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e("in connectRunnable: ", "Socket's create() method failed", e);
        }
        socket = tmp;
        PublicStaticObjects.setSocket(socket);
    }

    @Override
    public void run() {

        bluetoothAdapter.cancelDiscovery();

        if(connectDevices()) {
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(socket);
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {
        try {
            InputAndOutput.setInputStream(socket.getInputStream());
            InputAndOutput.setOutputStream(socket.getOutputStream());
            PublicStaticObjects.setIsConnected(true);
            PublicStaticObjects.getMainActivity().runOnUiThread(
                    () -> PublicStaticObjects.getMainActivity().getArrayAdapter().clear());
            PublicStaticObjects.getMainActivity().getDevices().clear();
            try {
//                InputAndOutput.getOutputStream().write(new byte[]{2, 2, 8, 0, 0, 0, 0, 0, 0});
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connectDevices() {
        try {

            PublicStaticObjects.getMainActivity().runOnUiThread(() ->
                    PublicStaticObjects.showToast("Ожидайте соединения...")
            );

            socket.connect();

            PublicStaticObjects.setIsConnected(true);
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                socket.close();
            } catch (IOException closeException) {
                closeException.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public void cancel() {
        try {
            socket.close();
            PublicStaticObjects.setIsConnected(false);
        } catch (IOException e) {
            Log.e("lol", "Could not close the client socket", e);
        }
    }
}
