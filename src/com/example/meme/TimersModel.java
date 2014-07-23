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

}
