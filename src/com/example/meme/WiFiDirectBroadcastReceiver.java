package com.example.meme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = WiFiDirectBroadcastReceiver.class
            .getSimpleName();
    private WifiP2pManager mManager;
    private Channel mChannel;
    private MemeMainActivity mActivity;
    private static boolean isConnected = false;

    public WiFiDirectBroadcastReceiver(WifiP2pManager mManager,
            Channel mChannel, MemeMainActivity activity) {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(TAG, "Wifi P2P is enabled.");
            } else {
                Log.i(TAG, "Wifi P2P is disabled.");
                WifiManager wifiManager = (WifiManager) mActivity
                        .getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(false);
                wifiManager.setWifiEnabled(true);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mManager != null) {
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }
            Log.i(TAG, "Peers changed.");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                .equals(action)) {
            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                Log.d(TAG, "Network connected.");
                if (!isConnected) {
                    mManager.requestConnectionInfo(mChannel, mActivity);
                    mManager.requestGroupInfo(mChannel, mActivity);
                    isConnected = true;
                }
            } else {
                isConnected = false;
                Log.i(TAG, "Network is not connected.");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {
            Log.i(TAG, "This device changed.");
            MemeMainActivity.thisDevice = (WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Util.myTimers.put(MemeMainActivity.thisDevice.deviceAddress, 0.0);
            Util.thisDeviceAddress = MemeMainActivity.thisDevice.deviceAddress;

            mActivity.setTitle("MEME ("
                    + MemeMainActivity.thisDevice.deviceName + ": "
                    + MemeMainActivity.thisDevice.deviceAddress + ")");
        }
    }
}
