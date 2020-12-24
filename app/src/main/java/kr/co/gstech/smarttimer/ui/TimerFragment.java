package kr.co.gstech.smarttimer.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.androchef.happytimer.countdowntimer.DynamicCountDownView;
import com.androchef.happytimer.countdowntimer.HappyTimer;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import kr.co.gstech.smarttimer.R;
import kr.co.gstech.smarttimer.bluetooth.BTCommunicator;
import kr.co.gstech.smarttimer.constants.MSG;
import kr.co.gstech.smarttimer.vo.AngleVO;

/**
 * A simple {@link Fragment} subclass.
 */
public class TimerFragment extends MainFragment {
    private static final String TAG = "TimerFragment";
    private DynamicCountDownView mDynamicCountDownView;
    private TextView mDebugTxt;
    private MaterialButton mConnectBtn;
    private MaterialButton mSettingBtn;

    private BTCommunicator mBTCommunicator;
    private HappyTimer.State mTimerState;
    private Timer mPauseTimer = null;

    private Drawable mPlayImage;
    private Drawable mPauseImage;
    private Drawable mStopImage;
    private ImageView mTimerStateImageView;

    private int mBreakTimeAngle;
    private int mBreakTimeSecond;
    private int mLearningGoalTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
//            initialize();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mBreakTimeAngle = Integer.parseInt(sharedPref.getString("angle", "30"));
        mBreakTimeSecond = Integer.parseInt(sharedPref.getString("break_time", "3"));
        mLearningGoalTime = Integer.parseInt(sharedPref.getString("learning_goal_time", "1"));
        initialize();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseObject();
    }

    private void releaseObject() {
        cancelPauseTimer();
        if (mDynamicCountDownView != null) {
            mDynamicCountDownView.stopTimer();
        }
        if (mBTCommunicator != null) {
            mBTCommunicator.disconnectDevice();
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initialize() {
        mTimerState = HappyTimer.State.STOPPED;
        FragmentActivity activity = getActivity();
        assert activity != null;
        mDynamicCountDownView = activity.findViewById(R.id.dynamicCountDownView);
        mDynamicCountDownView.initTimer(mLearningGoalTime * 3600, HappyTimer.Type.COUNT_UP);
        mDynamicCountDownView.setTimerType(HappyTimer.Type.COUNT_UP);
        mDynamicCountDownView.setStateChangeListener(new HappyTimer.OnStateChangeListener() {
            @Override
            public void onStateChange(HappyTimer.State state, int i, int i1) {
                mTimerState = state;
                System.out.println("Timer State : " + state);
                if (state == HappyTimer.State.RESET) {
                    releaseObject();
                    initialize();
                }
            }
        });

        mDebugTxt = activity.findViewById(R.id.debugTxt);
        ImageButton playBtn = activity.findViewById(R.id.playBtn);
        ImageButton pauseBtn = activity.findViewById(R.id.pauseBtn);
        ImageButton stopBtn = activity.findViewById(R.id.stopBtn);
        mConnectBtn = activity.findViewById(R.id.connectBtn);
        mSettingBtn = activity.findViewById(R.id.settingBtn);
        mConnectBtn.setEnabled(true);
        mSettingBtn.setEnabled(true);
        mConnectBtn.setText(R.string.bt_connect);

        BtnOnClickListener btnOnClickListener = new BtnOnClickListener();
        playBtn.setOnClickListener(btnOnClickListener);
        pauseBtn.setOnClickListener(btnOnClickListener);
        stopBtn.setOnClickListener(btnOnClickListener);
        mConnectBtn.setOnClickListener(btnOnClickListener);
        mSettingBtn.setOnClickListener(btnOnClickListener);

        mPlayImage = getResources().getDrawable(R.drawable.ic_outline_play_circle_24, null);
        mPauseImage = getResources().getDrawable(R.drawable.ic_outline_pause_circle_outline_24, null);
        mStopImage = getResources().getDrawable(R.drawable.ic_outline_stop_circle_24, null);
        mTimerStateImageView = activity.findViewById(R.id.timerStateImage);
        mTimerStateImageView.setImageDrawable(mStopImage);

        mBTCommunicator = new BTCommunicator(new BtHandler());
    }

    public class BtHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG.BT_CONNECTED:
                    mConnectBtn.setText(getString(R.string.bt_disconnect));
                    mSettingBtn.setEnabled(false);
                case MSG.BT_CONNECTING_STATUS:
                    Toast.makeText(getContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
                case MSG.BT_MESSAGE_READ:
                    AngleVO angleVO = (AngleVO) msg.obj;
                    String currentState;
                    if (Math.abs(angleVO.getX()) > mBreakTimeAngle) {   //Study..
                        onStateStudy();
                        currentState = "Study...";
                    } else {                                            //Take a break.
                        onStateTakeBreak();
                        currentState = "Take a break (after " + mBreakTimeSecond + " +  second).";
                    }
                    showDebugMsg(angleVO, currentState);
                    break;
            }
        }
    }

    private void onStateStudy() {
        if (mTimerState == HappyTimer.State.STOPPED) {
            mDynamicCountDownView.startTimer();
            mTimerStateImageView.setImageDrawable(mPlayImage);
        } else if (mTimerState == HappyTimer.State.PAUSED) {
            cancelPauseTimer();
            mDynamicCountDownView.resumeTimer();
            mTimerStateImageView.setImageDrawable(mPlayImage);
        }
    }

    private void onStateTakeBreak() {
        if (mTimerState == HappyTimer.State.RUNNING || mTimerState == HappyTimer.State.RESUMED) {
            if (mPauseTimer == null) {
                startPauseTimer();
            }
        }
    }

    private void startPauseTimer() {
        mPauseTimer = new Timer();
        mPauseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "CALL startPauseTimer");
                        mDynamicCountDownView.pauseTimer();
                        mPauseTimer = null;
                        mTimerStateImageView.setImageDrawable(mPauseImage);
                    }
                });
            }
        }, mBreakTimeSecond * 1000);
    }

    private void cancelPauseTimer() {
        if (mPauseTimer != null) {
            Log.i(TAG, "CALL cancelPauseTimer");
            mPauseTimer.cancel();
        }
    }

    class BtnOnClickListener implements Button.OnClickListener {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.playBtn:
                    mDynamicCountDownView.startTimer();
                    break;
                case R.id.pauseBtn:
                    mDynamicCountDownView.pauseTimer();
                    break;
                case R.id.stopBtn:
                    mDynamicCountDownView.stopTimer();
                    break;
                case R.id.connectBtn:
                    if (mSettingBtn.isEnabled()) {
                        listPairedDevices();
                    } else {
                        releaseObject();
                        initialize();
                    }
                    break;
                case R.id.settingBtn:
                    mListener.onFragmentInteraction();
                    break;
            }
        }
    }

    void listPairedDevices() {
        if (mBTCommunicator.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = mBTCommunicator.getBondedDevices();

            if (pairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(getString(R.string.bt_title));

                List<String> listPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : pairedDevices) {
                    listPairedDevices.add(device.getName());
                }
                final CharSequence[] items = listPairedDevices.toArray(new CharSequence[listPairedDevices.size()]);
                listPairedDevices.toArray(new CharSequence[listPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        mBTCommunicator.connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getContext(), getString(R.string.bt_not_found), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getContext(), getString(R.string.bt_disabled), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("DefaultLocale")
    private void showDebugMsg(AngleVO angleVO, String currentState) {
        StringBuilder sb = new StringBuilder();
        sb.append("Raw Data = ");
        for (byte b : angleVO.getRawData()) {
            sb.append(String.format("%02x ", b));
        }
        sb.append(String.format("\nAngle = %4.3f\t\t%4.3f\t\t%4.3f\n", angleVO.getX(), angleVO.getY(), angleVO.getZ()));
        sb.append("\n");
        sb.append(currentState);
//        System.out.println(sb.toString());
        mDebugTxt.setText(sb.toString());
    }

}