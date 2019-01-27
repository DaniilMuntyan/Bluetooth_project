package com.example.bluetooth_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.example.bluetooth_project.ALL.InputAndOutput;
import com.example.bluetooth_project.ALL.PublicStaticObjects;
import com.example.bluetooth_project.connectionStuff.Listener;
import com.example.bluetooth_project.connectionStuff.clientPart.ConnectRunnable;
import com.example.bluetooth_project.connectionStuff.serverPart.AcceptRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int ACTION_REQUEST_MULTIPLE_PERMISSION = 1;
    private final static int MAX_TIME_DISCOVER_SECONDS = 300;
    private static final int DISCOVERY_REQUEST = 228;

    private Button buttonTurnOn;
    private Button buttonDiscovery;
    private Button buttonDiscoverable;
    private Button buttonSend;
    private ListView listView;
    private EditText editText;

    private ArrayAdapter<String> arrayAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    private AcceptRunnable acceptRunnable;
    private ConnectRunnable connectRunnable;
    private Thread threadAccept, threadConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // filling PublicStaticObjects
        PublicStaticObjects.setBluetoothAdapter(bluetoothAdapter);
        PublicStaticObjects.setMainActivity(this);

        // bluetooth doesn't exist I guess
        if(bluetoothAdapter == null) {
            try {
                throw new Exception("bluetooth not found");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // this hack is needed on Samsung J7
        checkPermission();

        // xml elements
        buttonTurnOn = findViewById(R.id.buttonTurnOn);
        buttonDiscovery = findViewById(R.id.buttonDiscovery);
        buttonDiscoverable = findViewById(R.id.buttonDiscoverable);
        buttonSend = findViewById(R.id.buttonSend);
        listView = findViewById(R.id.list);
        editText = findViewById(R.id.editText);

        buttonTurnOn.setOnClickListener(this);
        buttonDiscovery.setOnClickListener(this);
        buttonDiscoverable.setOnClickListener(this);
        buttonSend.setOnClickListener(this);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        //TODO (Выровнять по центру)

        listView.setAdapter(arrayAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        listView.setOnItemClickListener((adapterView, view, i, l) -> listViewAction(i) );

        if(bluetoothAdapter.isEnabled()) {
            runServer();
        }

        new Thread(new Listener()).start();
    }

    private void runServer() {
        if(acceptRunnable == null) {
            acceptRunnable = new AcceptRunnable();
        }
        threadAccept = new Thread(acceptRunnable);
        threadAccept.start();
    }

    private void stopServer() {
        acceptRunnable.cancel();
        threadAccept.interrupt();
    }

    private Timer timer = new Timer();

    private TimerTask newTimerTaskDecreaseCounter() {
        return new TimerTask() {
            int secondsLeft = MAX_TIME_DISCOVER_SECONDS;
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                runOnUiThread(() ->
                    buttonDiscoverable.setText(getResources().getString(R.string.discoverable) +
                                                " (" + --secondsLeft + ")"));
                if(secondsLeft == 0) {
                    runOnUiThread(() -> {
                        buttonDiscoverable.setText(getResources().getString(R.string.discoverable));
                        timer.cancel();
                        timer.purge();
                    });
                }
            }
        };
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addElementToList(device.getName() + " " + device.getAddress());
                devices.add(device);
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // TODO
            }
        }
    };

    private void addElementToList(String element) {
        if(arrayAdapter.getPosition(element) == -1) {
            arrayAdapter.add(element);
        }
    }

    private void askToEnableBluetooth(BluetoothAdapter bluetoothAdapter) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_ENABLE_BT);
        if(!bluetoothAdapter.isEnabled()) {
            PublicStaticObjects.showToast("Вы должны включить Bluetooth");
        }
        else {
            PublicStaticObjects.showToast("Bluetooth уже включен");
        }
    }

    private void checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int pCheck;
            pCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            pCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            pCheck += this.checkSelfPermission("Manifest.permission.BLUETOOTH_ADMIN");
            pCheck += this.checkSelfPermission("Manifest.permission.BLUETOOTH");
            if (pCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH}, ACTION_REQUEST_MULTIPLE_PERMISSION);
            }
        }
    }

    // actions:

    private void buttonDiscoverableAction() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, MAX_TIME_DISCOVER_SECONDS);
        startActivityForResult(discoverableIntent, DISCOVERY_REQUEST);
    }

    private void buttonDiscoveryAction() {
        if(bluetoothAdapter.isEnabled()) {
            arrayAdapter.clear();
            devices.clear();
            // if is already discovering discover again
            if(bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        }
        else {
            PublicStaticObjects.showToast("Вы должны включить Bluetooth");
        }
    }

    private void buttonTurnOnAction() {

        // checking if bluetooth is enabled
        if(!bluetoothAdapter.isEnabled()) {
            askToEnableBluetooth(bluetoothAdapter);
        }
    }

    private void buttonSendAction() {
        if(PublicStaticObjects.getIsConnected()) {
            String toSend = editText.getText().toString();
            if(editText.getText().toString().length() != 6) {
                PublicStaticObjects.showToast("Серийник должен быть 6-тизначным");
            }
            else {
                try {
                    byte[] buffer = new byte[6 + 3];
                    buffer[0] = 2;
                    buffer[1] = 2;
                    buffer[2] = 8;
                    System.arraycopy(toSend.getBytes(), 0, buffer, 3, 6);
                    InputAndOutput.getOutputStream().write(buffer);
                    InputAndOutput.getOutputStream().flush();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void listViewAction(int i) {
        connectRunnable = new ConnectRunnable(devices.get(i));
        threadConnect = new Thread(connectRunnable);
        threadConnect.start();
    }

    // override methods:

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        acceptRunnable.cancel();
        connectRunnable.cancel();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonDiscovery:
                buttonDiscoveryAction();
                break;
            case R.id.buttonTurnOn:
                buttonTurnOnAction();
                break;
            case R.id.buttonDiscoverable:
                buttonDiscoverableAction();
                break;
            case R.id.buttonSend:
                buttonSendAction();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 228) {
            if(resultCode == 300) {
                timer.scheduleAtFixedRate(newTimerTaskDecreaseCounter(), 0, 1000);
            } else {
                PublicStaticObjects.showToast("Разрешение не получено");
            }
        }
        if(requestCode == REQUEST_ENABLE_BT) {
            // resultCode == -1 - OK
            // resultCode ==  0 - NOT OK
            if(resultCode == -1) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> runServer());
                    }
                }, 2000);
            }
        }
    }

    // getters and setters:

    public EditText getEditText() {
        return editText;
    }

    public Button getButtonSend() {
        return buttonSend;
    }

    public ArrayAdapter<String> getArrayAdapter() {
        return arrayAdapter;
    }
}
