package com.example.meme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

public class Util {
    public static final String TAG = Util.class.getSimpleName();
    public final static String D1 = "fa:a9:d0:02:21:08";
    public final static String D2 = "fa:a9:d0:03:e0:1b";
    public final static String D4 = "fa:a9:d0:1c:03:16";
    static String thisDeviceAddress;

    static Map<String, Double> myTimers = new HashMap<String, Double>();
    static {
        myTimers.put(D1, Double.POSITIVE_INFINITY);
        myTimers.put(D2, Double.POSITIVE_INFINITY);
        myTimers.put(D4, Double.POSITIVE_INFINITY);
    }

    static Map<String, String> deviceNameAddressMap = new HashMap<String, String>();
    static {
        deviceNameAddressMap.put(D1, "D1");
        deviceNameAddressMap.put(D2, "D2");
        deviceNameAddressMap.put(D4, "D4");
    }

    static final long startTime = System.currentTimeMillis();

    public static int getCurrentTimeInstant() {
        long currentTime = System.currentTimeMillis();
        int currentTimeInstant = (int) (currentTime - startTime) / 1000;
        return currentTimeInstant;
    }

    static boolean stopUpdate = false;

    public static void updateTimersLocally() {
        stopUpdate = false;
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Updating timers locally.");
                while (true) {
                    if (stopUpdate) {
                        Log.d(TAG, "Stopped updating timers locally.");
                        break;
                    }
                    for (String device : myTimers.keySet()) {
                        if (device == thisDeviceAddress) {
                            myTimers.put(device, 0.0);
                        } else {
                            double timer = myTimers.get(device);
                            if (timer != Double.POSITIVE_INFINITY
                                    && timer != 0.0) {
                                myTimers.put(device, timer + 1.0);
                            }
                        }
                    }
                    Log.d(TAG, "Timers updated: " + myTimers);
                    if (stopUpdate) {
                        Log.d(TAG, "Stopped updating timers locally.");
                        break;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    public static Collection<MobileDevice> deviceToMobileDevice(
            Collection<WifiP2pDevice> list) {
        List<MobileDevice> mobileDevices = new ArrayList<MobileDevice>();
        for (WifiP2pDevice device : list) {
            MobileDevice mobileDevice = new MobileDevice(device);
            mobileDevices.add(mobileDevice);
        }
        return mobileDevices;
    }

}
