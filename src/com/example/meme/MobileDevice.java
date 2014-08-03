package com.example.meme;

import android.net.wifi.p2p.WifiP2pDevice;

public class MobileDevice {
    public WifiP2pDevice device;

    public MobileDevice(WifiP2pDevice device) {
        this.device = device;
    }

    @Override
    public boolean equals(Object o) {
        MobileDevice device = (MobileDevice) o;
        return (device.toString().equals(toString()));
    }

    public String toString() {
        return device.deviceName + ": " + device.deviceAddress;
    }
}
