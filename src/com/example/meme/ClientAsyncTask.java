package com.example.meme;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.widget.Toast;

public class ClientAsyncTask extends AsyncTask<Void, Void, String> {
    private MemeMainActivity mActivity;

    public ClientAsyncTask(MemeMainActivity activity) {
        mActivity = activity;
    }

    private void updateTimers() {
        Util.stopUpdate = true;
        mActivity.dataSource.storeTimers(Util.getCurrentTimeInstant(),
                Util.myTimers, mActivity.receivedTimers);
        for (String device : Util.myTimers.keySet()) {
            double myTimer = Util.myTimers.get(device);
            double receivedTimer = mActivity.receivedTimers.get(device);
            if (receivedTimer != Double.POSITIVE_INFINITY && myTimer != 0.0) {
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
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        mActivity.receivedTimers = null;
        try {
            mActivity.runOnUiThread(new TextViewRunnable(
                    "Sleeping for a while to let server up.\n"));
            Thread.sleep(Util.SLEEP_TIME_SHORT);
            socket = new Socket(mActivity.groupOwnerAddress.getHostAddress(),
                    Util.SERVER_SOCKET);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject(Util.myTimers);
            oos.flush();
            while (mActivity.receivedTimers == null) {
                mActivity.receivedTimers = (Map<String, Double>) ois
                        .readObject();
            }
            updateTimers();
            return mActivity.receivedTimers.toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (InterruptedException e) {
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
                mActivity.isThisDeviceClient = false;
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
