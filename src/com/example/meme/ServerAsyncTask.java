package com.example.meme;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import android.media.MediaScannerConnection;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.AsyncTask;
import android.widget.Toast;

public class ServerAsyncTask extends AsyncTask<Void, Void, String> {

    private MemeMainActivity mActivity;

    public ServerAsyncTask(MemeMainActivity activity) {
        mActivity = activity;
    }

    private void updateTimers() {
        Util.stopUpdate = true;
        mActivity.dataSource.storeTimers(Util.getCurrentTimeInstant(),
                Util.myTimers, mActivity.receivedTimers);

        for (String device : Util.myTimers.keySet()) {
            double myTimer = Util.myTimers.get(device);
            double receivedTimer = mActivity.receivedTimers.get(device);
            if (receivedTimer != Double.POSITIVE_INFINITY) {
                if (myTimer == Double.POSITIVE_INFINITY
                        || (receivedTimer + Util.TIME_GRADIENT < myTimer)) {
                    Util.myTimers.put(device, receivedTimer
                            + Util.TIME_GRADIENT);
                }
            }
        }
        Util.updateTimersLocally();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String doInBackground(Void... params) {
        ServerSocket socket = null;
        Socket client = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        mActivity.receivedTimers = null;
        try {
            socket = new ServerSocket(Util.SERVER_SOCKET);
            client = socket.accept();
            oos = new ObjectOutputStream(client.getOutputStream());
            ois = new ObjectInputStream(client.getInputStream());
            while (mActivity.receivedTimers == null) {
                mActivity.receivedTimers = (Map<String, Double>) ois
                        .readObject();
            }
            oos.writeObject(Util.myTimers);
            oos.flush();
            updateTimers();
            return mActivity.receivedTimers.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return e.getMessage();
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    ois.close();
                    oos.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPostExecute(final String result) {
        super.onPostExecute(result);
        TimersModel latestTimers = mActivity.dataSource.getLatestEntry();
        mActivity.writeToLogFile(latestTimers.toBigString());
        MediaScannerConnection.scanFile(mActivity,
                new String[] { mActivity.logFile.getAbsolutePath() }, null,
                null);
        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
                mActivity.appendTextAndScroll(Util.myTimers + "\n");
                try {
                    Thread.sleep(Util.SLEEP_TIME_SHORT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mActivity.mManager.removeGroup(mActivity.mChannel,
                        new ActionListener() {

                            @Override
                            public void onSuccess() {
                                mActivity.runOnUiThread(new TextViewRunnable(
                                        "Group removed."));
                                mActivity.isThisDeviceGO = false;
                            }

                            @Override
                            public void onFailure(int reason) {
                                mActivity.runOnUiThread(new TextViewRunnable(
                                        "Group removal failed. Reason: "
                                                + reason));
                            }
                        });
            }
        });
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
