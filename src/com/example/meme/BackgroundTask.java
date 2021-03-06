package com.example.meme;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.util.Log;

public class BackgroundTask extends AsyncTask<Void, Void, Void> {
    MemeMainActivity mActivity;
    boolean isConnectionPossible;

    public BackgroundTask(MemeMainActivity activity) {
        mActivity = activity;
        isConnectionPossible = false;
    }

    @Override
    protected Void doInBackground(Void... params) {

        mActivity.runOnUiThread(new TextViewRunnable("Started."));
        // If this device was already assigned the Group Owner role, then open
        // the socket for any incoming message.
        if (mActivity.isThisDeviceGO) {
            mActivity.buttonSendReceive.callOnClick();
            mActivity.runOnUiThread(new TextViewRunnable(
                    "Role was already assigned. Send/Receive button clicked."));
        } else {
            // Discover peers until list is populated.
            while (mActivity.listPeerListAdapter.isEmpty()) {
                // If user stopped the task, return immediately.
                if (mActivity.stopMeme) {
                    Util.stopUpdate = true;
                    return null;
                }
                // If this device was assigned a role even before connecting
                // (this may happen in Wi-Fi Direct), then stop discovering
                // devices.
                if (mActivity.isThisDeviceGO) {
                    mActivity.runOnUiThread(new TextViewRunnable("Breaking."
                            + mActivity.isThisDeviceClient + " "
                            + mActivity.isThisDeviceGO));
                    break;
                }
                // Keep discovering devices and sleep for 3 seconds between
                // calls to discover.
                mActivity.buttonDiscover.callOnClick();
                mActivity.runOnUiThread(new TextViewRunnable(
                        "Discover button clicked."));
                try {
                    mActivity.runOnUiThread(new TextViewRunnable("Sleeping "
                            + Util.SLEEP_TIME_SHORT
                            + " ms till devices are discovered."));
                    Thread.sleep(Util.SLEEP_TIME_SHORT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    mActivity
                            .runOnUiThread(new TextViewRunnable(e.getMessage()));
                    return null;
                }
            }

            // If this device was assigned a role before connecting, then open
            // socket to exchange data.
            if (mActivity.isThisDeviceClient || mActivity.isThisDeviceGO) {
                // If this device was assigned a role of client, wait some
                // additional time to let the group owner open its socket.
                if (mActivity.isThisDeviceClient) {
                    mActivity.runOnUiThread(new TextViewRunnable("Sleeping "
                            + Util.SLEEP_TIME_MEDIUM
                            + " ms till the device is assigned a role."));
                    try {
                        Thread.sleep(Util.SLEEP_TIME_MEDIUM);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mActivity.buttonSendReceive.callOnClick();
                mActivity
                        .runOnUiThread(new TextViewRunnable(
                                "Role was already assigned. Send/Receive button clicked."));
            }
            // After discovering, if no role is assigned to this device yet,
            // then wait for a while.
            else if (!mActivity.listPeerListAdapter.isEmpty()) {
                int deviceCount = mActivity.listPeerListAdapter.getCount();
                boolean wasConnectedSuccessfully = false;
                int index = -1;
                while (++index < deviceCount) {
                    MobileDevice mobileDevice = mActivity.listPeerListAdapter
                            .getItem(index);
                    if (!mActivity.seenPeers.contains(mobileDevice)) {
                        final WifiP2pDevice device = mobileDevice.device;
                        mActivity.seenPeers.add(mobileDevice);
                        Log.d("MEME", "Seen Peers: " + mActivity.seenPeers);
                        isConnectionPossible = mActivity.connectToPeer(device);
                        if (isConnectionPossible) {
                            Log.d("MEME", "Successfully connected to "
                                    + device.deviceName);
                            wasConnectedSuccessfully = true;
                            break;
                        } else {
                            Log.d("MEME", "Could not connect to "
                                    + device.deviceName);
                            continue;
                        }
                    }
                    if (index == deviceCount - 1 && !wasConnectedSuccessfully) {
                        index = -1;
                        mActivity.runOnUiThread(new TextViewRunnable(
                                "All devices have been seen. Starting again."));
                        mActivity.seenPeers.clear();
                    }
                }

                // Wait till a role is assigned to the device and
                // send/receive data accordingly.
                int count = 5;
                while (!mActivity.isThisDeviceClient
                        && !mActivity.isThisDeviceGO) {
                    // If user stopped the task, return immediately.
                    if (mActivity.stopMeme) {
                        Util.stopUpdate = true;
                        return null;
                    }
                    // If no role was assigned after a specific period of time,
                    // discard the connection. We cannot wait forever.
                    count -= 1;
                    if (count < 0) {
                        mActivity.runOnUiThread(new TextViewRunnable(
                                "No role assigned. Discarding connection."));
                        mActivity.mManager.cancelConnect(mActivity.mChannel,
                                null);
                        break;
                    }
                    mActivity.runOnUiThread(new TextViewRunnable("Sleeping "
                            + Util.SLEEP_TIME_MEDIUM
                            + " ms till the device is assigned a role."));
                    try {
                        Thread.sleep(Util.SLEEP_TIME_MEDIUM);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // If this device was assigned a role of client, wait some
                // additional time to let the group owner open its socket.
                if (mActivity.isThisDeviceClient) {
                    mActivity.runOnUiThread(new TextViewRunnable("Sleeping "
                            + Util.SLEEP_TIME_MEDIUM
                            + " ms till the device is assigned a role."));
                    try {
                        Thread.sleep(Util.SLEEP_TIME_MEDIUM);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Open socket to exchange data.
                mActivity.buttonSendReceive.callOnClick();
                mActivity.runOnUiThread(new TextViewRunnable(
                        "Send/Receive button clicked."));
            }
            // If no devices were discovered and no role was assigned, then exit
            // this task.
            else {
                mActivity
                        .runOnUiThread(new TextViewRunnable(
                                "List adapter empty and no role assigned. Stopping service."));
            }
        }
        return null;
    }

    class TextViewRunnable implements Runnable {
        private String string;

        public TextViewRunnable(String string) {
            this.string = string;
        }

        @Override
        public void run() {
            mActivity.appendTextAndScroll(string + "\n");
        }

    }

}
