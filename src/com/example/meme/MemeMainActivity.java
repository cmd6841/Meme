package com.example.meme;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MemeMainActivity extends Activity implements OnClickListener,
        ConnectionInfoListener, GroupInfoListener {
    public static final String TAG = MemeMainActivity.class.getSimpleName();
    WifiP2pManager mManager;
    Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    Button buttonDiscover, buttonSendReceive;
    ListView listPeerList;
    ArrayAdapter<MobileDevice> listPeerListAdapter;
    List<MobileDevice> peers = new ArrayList<MobileDevice>();
    List<MobileDevice> seenPeers = new ArrayList<MobileDevice>();
    public static WifiP2pDevice thisDevice;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meme_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        buttonDiscover = (Button) findViewById(R.id.button_discover);
        buttonDiscover.setOnClickListener(this);

        buttonSendReceive = (Button) findViewById(R.id.button_send_receive);
        buttonSendReceive.setOnClickListener(this);

        listPeerList = (ListView) findViewById(R.id.listView_peerlist);
        listPeerListAdapter = new ArrayAdapter<MobileDevice>(this,
                android.R.layout.simple_list_item_1, peers);
        listPeerList.setAdapter(listPeerListAdapter);
        listPeerList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                final MobileDevice item = (MobileDevice) parent
                        .getItemAtPosition(position);
                connectToPeer(item.device);
            }
        });
        textView = (TextView) findViewById(R.id.textView_updates);
        textView.setMovementMethod(new ScrollingMovementMethod());
        dataSource = new TimersDAO(this);

        memeDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MEME");
        if (!memeDir.exists()) {
            memeDir.mkdirs();
        }
        logFile = new File(memeDir.getAbsolutePath(), Util.getNewFileName());

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fileWriter = new FileWriter(logFile);
            logWriter = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Util.updateTimersLocally();
        start();
    }

    File memeDir;
    File logFile;
    FileWriter fileWriter;
    BufferedWriter logWriter;
    TimersDAO dataSource;
    BackgroundTask backgroundTask;
    private int timesRun = 0;
    boolean stopMeme = false;

    private void start() {
        // Start a new thread that runs MEME in background indefinitely until it
        // is stopped by the user.
        new Thread(new Runnable() {

            @Override
            public void run() {

                while (true) {
                    // Stop the background thread execution and this thread
                    // execution.
                    if (stopMeme) {
                        Util.stopUpdate = true;
                        runOnUiThread(new TextViewRunnable("MEME stopped."));
                        if (!backgroundTask.isCancelled())
                            backgroundTask.cancel(true);
                        closeFiles();
                        break;
                    }
                    runOnUiThread(new TextViewRunnable(
                            "Service task run count: " + (++timesRun)));
                    // Start MEME in background.
                    backgroundTask = new BackgroundTask(MemeMainActivity.this);
                    backgroundTask.execute();

                    // Wait while the background task is running.
                    Status status = backgroundTask.getStatus();
                    while (status != Status.FINISHED) {
                        status = backgroundTask.getStatus();
                    }

                    // Stop the background thread execution and this thread
                    // execution.
                    if (stopMeme) {
                        Util.stopUpdate = true;
                        runOnUiThread(new TextViewRunnable("MEME stopped."));
                        if (!backgroundTask.isCancelled())
                            backgroundTask.cancel(true);
                        closeFiles();
                        break;
                    }

                    // Sleep time before next start.
                    runOnUiThread(new TextViewRunnable("Sleeping for "
                            + Util.SLEEP_TIME_LONG + " ms before new run."));
                    try {
                        Thread.sleep(Util.SLEEP_TIME_LONG);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        Log.d(TAG, "MEME start thread stopped!");
    }

    class TextViewRunnable implements Runnable {
        private String string;

        public TextViewRunnable(String string) {
            this.string = string;
        }

        @Override
        public void run() {
            appendTextAndScroll(string + "\n");
            listPeerListAdapter.notifyDataSetChanged();
        }

    }

    InetAddress groupOwnerAddress;
    boolean isThisDeviceGO = false;
    boolean isThisDeviceClient = false;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.i(TAG, "Connection info received.");
        appendTextAndScroll("Connection info received.\n");
        groupOwnerAddress = info.groupOwnerAddress;

        if (info.groupFormed && info.isGroupOwner) {
            appendTextAndScroll("This device is the groupowner.\n");
            Log.i(TAG, "This device is the groupowner.");
            isThisDeviceGO = true;
        } else if (info.groupFormed) {
            appendTextAndScroll("This device is a client.\n");
            Log.i(TAG, "This device is a client.");
            isThisDeviceClient = true;
        } else {
            appendTextAndScroll("This device is not connected.\n");
            Log.d(TAG, "This device is not connected.");
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null) {
            if (group.isGroupOwner()) {
                appendTextAndScroll(group.getNetworkName() + " created.\n");
                Log.d(TAG, group.getNetworkName() + " created.");
            } else {
                appendTextAndScroll(group.getNetworkName() + "created by "
                        + group.getOwner().deviceName + " joined.\n");
                Log.d(TAG,
                        group.getNetworkName() + "created by "
                                + group.getOwner().deviceName + " joined.");
            }
        }
    }

    List<String> connectionsList = new ArrayList<String>();
    Map<String, Double> receivedTimers;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        // Discover devices.
        case R.id.button_discover:
            mManager.discoverPeers(mChannel, new ActionListener() {

                @Override
                public void onSuccess() {
                    Log.i(TAG, "Peer discovery successful.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.i(TAG, "Peer discovery failed. Reason: " + reason);
                }
            });
            break;

        // Open the client or server socket for exchanging the messages.
        case R.id.button_send_receive:
            if (isThisDeviceGO) {
                new ServerAsyncTask(this).execute();
            } else if (isThisDeviceClient) {
                new ClientAsyncTask(this).execute();
            } else {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MemeMainActivity.this,
                                "Device not yet assigned any role.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }
            break;

        default:
            break;
        }
    }

    public PeerListListener peerListListener = new PeerListListener() {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            peers.clear();
            peers.addAll(Util.deviceToMobileDevice(peerList.getDeviceList()));
            listPeerListAdapter.notifyDataSetChanged();
            if (peers.size() == 0) {
                Log.d(TAG, "No devices found");
                return;
            }
        }
    };

    public boolean connectToPeer(final WifiP2pDevice peer) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peer.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (peer.status == WifiP2pDevice.CONNECTED
                || peer.status == WifiP2pDevice.UNAVAILABLE
                || peer.status == WifiP2pDevice.FAILED) {
            Log.d("MEME", "Connection not possible - Peer status: "
                    + peer.status);
            return false;
        } else {
            Log.d("MEME", "Connection is possible - Peer status: "
                    + peer.status);
            mManager.connect(mChannel, config, new ActionListener() {

                @Override
                public void onSuccess() {
                    appendTextAndScroll("Connection to " + peer.deviceName
                            + " successful.\n");
                    Log.i(TAG, "Connection to " + peer.deviceName
                            + " successful.");
                    if (peer.isGroupOwner()) {
                        Toast.makeText(MemeMainActivity.this,
                                "Connected to GO: " + peer.deviceName,
                                Toast.LENGTH_SHORT).show();
                        appendTextAndScroll("Connected to GO: "
                                + peer.deviceName + "\n");
                    } else {
                        Toast.makeText(MemeMainActivity.this,
                                "Connected to " + peer.deviceName,
                                Toast.LENGTH_SHORT).show();
                        appendTextAndScroll("Connected to " + peer.deviceName
                                + "\n");
                    }
                }

                @Override
                public void onFailure(int reason) {
                    appendTextAndScroll("connect() failed. Reason: " + reason
                            + "\n");
                    Toast.makeText(MemeMainActivity.this,
                            "Connect failed. Reason: " + reason,
                            Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.meme_main, menu);
        return true;
    }

    static boolean isDatabaseUpdated = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
        // Set the flag to stop MEME.
        case R.id.item_stop:
            stopMeme = true;
            return true;
            // Show the timers database in a new activity.
        case R.id.item_show_timers:
            Intent intent = new Intent(this, TimersActivity.class);
            startActivity(intent);
            return true;
            // Apply the estimation algorithm to show which devices are moving
            // closer to this device.
        case R.id.item_predict:
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setPositiveButton("Close", null);

            if (!isDatabaseUpdated) {
                dialog.setTitle("Error!");
                dialog.setMessage("Current database not updated. No new timers present in database!");
            } else {
                // Get the latest entry of timers in the database.
                TimersModel timersModel = dataSource.getLatestEntry();
                dialog.setTitle("Time Instant: " + timersModel.getTimeInstant());
                dialog.setMessage(predict(timersModel));
            }
            dialog.show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }

    }

    public static String predict(TimersModel timersModel) {
        StringBuffer buffer = new StringBuffer();
        if (timersModel != null) {
            buffer.append("Devices moving closer to " + thisDevice.deviceName
                    + " at t = " + timersModel.getTimeInstant() + ":\n");
            buffer.append(timersModel.getDevicesMovingCloser());
        } else {
            buffer.append("No timers present in database!");
        }
        return buffer.toString();
    }

    public void writeToLogFile(String content) {
        try {
            logWriter.write(content);
            logWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_meme_main,
                    container, false);
            return rootView;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        Util.stopUpdate = true;
        closeFiles();
    }

    public void closeFiles() {
        try {
            if (logWriter != null) {
                logWriter.flush();
                logWriter.close();
                logWriter = null;
            }
            // MediaScannerConnection.scanFile(this,
            // new String[] { logFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendTextAndScroll(String text) {
        if (textView != null) {
            textView.append(text + "\n");
            final Layout layout = textView.getLayout();
            if (layout != null) {
                int scrollDelta = layout
                        .getLineBottom(textView.getLineCount() - 1)
                        - textView.getScrollY() - textView.getHeight();
                if (scrollDelta > 0)
                    textView.scrollBy(0, scrollDelta);
            }
        }
    }
}
