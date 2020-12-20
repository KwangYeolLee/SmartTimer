package kr.co.gstech.smarttimer.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.UUID;

import kr.co.gstech.smarttimer.constants.MSG;
import kr.co.gstech.smarttimer.utils.StLibrary;
import kr.co.gstech.smarttimer.vo.AngleVO;

public class BTCommunicator {
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Handler mBluetoothHandler;

    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private final Set<BluetoothDevice> mPairedDevices;

    private ConnectedBluetoothThread mThreadConnectedBluetooth;

    public BTCommunicator(Handler handler) {
        mBluetoothHandler = handler;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPairedDevices = mBluetoothAdapter.getBondedDevices();
    }

    public void disconnectDevice() {
        if (mThreadConnectedBluetooth != null) {
            mThreadConnectedBluetooth.cancel();
        }
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return mPairedDevices;
    }

    public void connectSelectedDevice(String selectedDeviceName) {
        for (BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            BluetoothSocket bluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(bluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(
                    MSG.BT_CONNECTED, "블루투스 장치가 연결 되었습니다.").sendToTarget();
        } catch (IOException e) {
            mBluetoothHandler.obtainMessage(
                    MSG.BT_CONNECTING_STATUS, "블루투스 연결 중 오류가 발생했습니다.").sendToTarget();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                mBluetoothHandler.obtainMessage(
                        MSG.BT_CONNECTING_STATUS, "소켓 연결 중 오류가 발생했습니다.").sendToTarget();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[4096];
            int bytes;

            while (!isInterrupted()) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        StLibrary.decimalToHex(buffer, bytes);
                        parcelPacket(buffer, bytes);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                mBluetoothHandler.obtainMessage(
                        MSG.BT_CONNECTING_STATUS, "데이터 전송 중 오류가 발생했습니다.").sendToTarget();
            }
        }

        public void cancel() {
            try {
                interrupt();
                mmSocket.close();
                mBluetoothHandler.obtainMessage(
                        MSG.BT_CONNECTING_STATUS, "블루투스 장치가 해제 되었습니다.").sendToTarget();
            } catch (IOException e) {
                mBluetoothHandler.obtainMessage(
                        MSG.BT_CONNECTING_STATUS, "소켓 해제 중 오류가 발생했습니다.").sendToTarget();
            }
        }
    }

    private void parcelPacket(byte[] buffer, int bytes) {
        byte[] acc = new byte[11];
        byte[] gyro = new byte[11];
        byte[] angle = new byte[11];

        int totalBytes = bytes;
        while (totalBytes >= 11) {
            if (buffer[0] != 0x55) {
                totalBytes--;
                System.arraycopy(buffer, 1, buffer, 0, totalBytes - 1);
                continue;
            }
            switch (buffer[1]) {
                case 0x51:
                    System.arraycopy(buffer, 0, acc, 0, 11);
                    break;
                case 0x52:
                    System.arraycopy(buffer, 0, gyro, 0, 11);
                    break;
                case 0x53:
                    System.arraycopy(buffer, 0, angle, 0, 11);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(angle);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    byteBuffer.get();
                    byteBuffer.get();
                    short roll = byteBuffer.getShort();
                    short pitch = byteBuffer.getShort();
                    short yaw = byteBuffer.getShort();
                    double x = roll / 32768.0 * 180;
                    double y = pitch / 32768.0 * 180;
                    double z = yaw / 32768.0 * 180;
                    AngleVO angleVO = new AngleVO(angle, x, y, z);
                    mBluetoothHandler.obtainMessage(MSG.BT_MESSAGE_READ, angleVO).sendToTarget();
                    break;
            }
            totalBytes -= 11;
            if (totalBytes < 11) {
                return;
            }
            System.arraycopy(buffer, 11, buffer, 0, totalBytes - 1);
        }
    }

}
