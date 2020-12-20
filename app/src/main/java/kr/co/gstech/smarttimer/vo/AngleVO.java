package kr.co.gstech.smarttimer.vo;

public class AngleVO {
    private byte[] rawData = new byte[11];
    private double x;
    private double y;
    private double z;

    public AngleVO(byte[] rawData, double x, double y, double z) {
        this.rawData = rawData;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
