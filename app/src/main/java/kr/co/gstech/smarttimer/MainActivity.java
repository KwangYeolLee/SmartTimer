package kr.co.gstech.smarttimer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.rbddevs.splashy.Splashy;

import java.util.Timer;
import java.util.TimerTask;

import kr.co.gstech.smarttimer.ui.MainFragment;
import kr.co.gstech.smarttimer.ui.SettingsFragment;
import kr.co.gstech.smarttimer.ui.TimerFragment;

public class MainActivity extends AppCompatActivity
        implements MainFragment.OnFragmentInteractionListener {
    private static final String TAG = "MainActivity";
    private final int SPLASH_DELAY = 3000;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            setSplashy();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            replaceFragment(R.layout.fragment_timer);
                        }
                    });
                }
            }, SPLASH_DELAY);
        }
    }

    private void setSplashy() {
        new Splashy(this)
            .setLogo(R.drawable.web_hi_res_512)
            .setTitle(R.string.app_name)
            .setTitleColor("#FFFFFF")
            .setSubTitle("The time never stops")
            .setProgressColor(R.color.white)
            .setBackgroundColor("#000000")
            .setDuration(SPLASH_DELAY)
            .show();
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof MainFragment) {
            MainFragment mainFragment = (MainFragment) fragment;
            mainFragment.setOnFragmentInteractionListener(this);
        }
    }

    private void replaceFragment(int layoutId) {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, getFragment(layoutId));
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commitAllowingStateLoss();
    }

    @SuppressLint("NonConstantResourceId")
    private MainFragment getFragment(int layoutId) {
        MainFragment mainFragment =  null;
        if (layoutId == R.layout.fragment_timer) {
            mainFragment = new TimerFragment();
        }
        return mainFragment;
    }

    @Override
    public void onFragmentInteraction() {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, new SettingsFragment());
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}