package com.example.meme;

public class TimersModel {
    private int timeInstant;
    private String mtArray;
    private String rtArray;
    private String deltatTArray;

    public int getTimeInstant() {
        return timeInstant;
    }

    public void setTimeInstant(int timeInstant) {
        this.timeInstant = timeInstant;
    }

    public String getMtArray() {
        return mtArray;
    }

    public void setMtArray(String mtArray) {
        this.mtArray = mtArray;
    }

    public String getRtArray() {
        return rtArray;
    }

    public void setRtArray(String rtArray) {
        this.rtArray = rtArray;
    }

    public String getDeltatTArray() {
        return deltatTArray.split("\\{")[1].split("\\}")[0];
    }

    public void setDeltatTArray(String deltatTArray) {
        this.deltatTArray = deltatTArray;
    }

    public String toString() {
        return timeInstant + "";
    }

    public String toBigString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(timeInstant + "," + mtArray + "," + rtArray + ","
                + deltatTArray + "\n");
        buffer.append(MemeMainActivity.predict(this, false));
        return buffer.toString();
    }

    public String toBiggerString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Prediction made at t = " + Util.getCurrentTimeInstant()
                + "\n");
        buffer.append(timeInstant + "," + mtArray + "," + rtArray + ","
                + deltatTArray + "\n");
        buffer.append(Util.lastContactTimes + "\n");
        buffer.append(MemeMainActivity.predict(this, true));
        return buffer.toString();
    }

    public String getDevicesMovingCloser(boolean writeToLog) {
        StringBuffer buffer = new StringBuffer();
        int count = 0;

        // Get the deltaT array from the latest timers entry and for each
        // positive value in this array, infer that the corresponding device
        // is moving closer to this device.
        String delta[] = getDeltatTArray().split(",");
        for (String s : delta) {
            String split[] = s.split("=");
            try {
                double value = Double.parseDouble(split[1]);
                if (value >= 0) {
                    count += 1;
                    String deviceName = Util.deviceNameAddressMap.get(split[0]
                            .trim());
                    if (writeToLog) {
                        buffer.append(deviceName + ": " + split[0]
                                + ", Last Updated: "
                                + Util.lastContactTimes.get(split[0].trim())
                                + "\n");
                    } else {
                        buffer.append(deviceName + ": " + split[0] + "\n");
                    }
                }
            } catch (NumberFormatException e) {
                count -= 1;
                continue;
            }

        }
        buffer.append("Total devices moving closer: " + count);
        return buffer.toString();
    }
}
