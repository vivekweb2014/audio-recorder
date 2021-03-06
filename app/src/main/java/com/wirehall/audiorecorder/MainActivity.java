package com.wirehall.audiorecorder;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wirehall.audiorecorder.explorer.FileListFragment;
import com.wirehall.audiorecorder.explorer.model.Recording;
import com.wirehall.audiorecorder.mp.MediaPlayerController;
import com.wirehall.audiorecorder.mr.AudioRecorderLocalService;
import com.wirehall.audiorecorder.mr.MediaRecorderState;
import com.wirehall.audiorecorder.mr.RecordingController;
import com.wirehall.audiorecorder.setting.SettingActivity;
import com.wirehall.audiorecorder.visualizer.VisualizerFragment;

public class MainActivity extends AppCompatActivity implements VisualizerFragment.VisualizerMPSession, FileListFragment.FileListFragmentListener {
    public final static String APP_PACKAGE_NAME = "com.wirehall.audiorecorder";
    public static final String KEY_PREF_RECORDING_STORAGE_PATH = "recording_storage_path";
    private static final String TAG = MainActivity.class.getName();
    private static final String PLAY_STORE_URL = "market://details?id=" + APP_PACKAGE_NAME;

    private static final int PERMISSION_REQUEST_CODE = 111;
    private static final String[] APP_PERMS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final RecordingController recordingController = RecordingController.getInstance();
    private final MediaPlayerController mediaPlayerController = MediaPlayerController.getInstance();
    private AudioRecorderLocalService audioRecorderLocalService;
    private BroadcastReceiver broadcastReceiver;
    private boolean isServiceBound = false;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            AudioRecorderLocalService.LocalBinder binder = (AudioRecorderLocalService.LocalBinder) service;
            audioRecorderLocalService = binder.getService();
            isServiceBound = true;

            // This will be invoked if actions are performed via service notification
            // So that the activity can update the UI accordingly
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    MediaRecorderState MEDIA_REC_STATE = AudioRecorderLocalService.MEDIA_REC_STATE;
                    String recordingFilePath = intent.getStringExtra(AudioRecorderLocalService.KEY_RECORDING_FILE_PATH);
                    switch (MEDIA_REC_STATE) {
                        case RECORDING:
                            recordingController.onRecordingStarted(MainActivity.this);
                            break;
                        case RESUMED:
                            recordingController.onRecordingResumed(MainActivity.this);
                            break;
                        case PAUSED:
                            recordingController.onRecordingPaused(MainActivity.this);
                            break;
                        case STOPPED:
                            recordingController.onRecordingStopped(MainActivity.this, false, recordingFilePath);
                            break;
                        case DISCARDED:
                            recordingController.onRecordingStopped(MainActivity.this, true, recordingFilePath);
                            break;
                        default:
                            break;
                    }
                }
            };
            LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(broadcastReceiver, new IntentFilter(AudioRecorderLocalService.EVENT_RECORDER_STATE_CHANGE));
            recordingController.init(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceBound = false;
            try {
                // https://stackoverflow.com/questions/2682043/how-to-check-if-receiver-is-registered-in-android
                LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            broadcastReceiver = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.main_activity);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.visualizer_fragment_container, VisualizerFragment.newInstance());
            ft.commit();
            ActivityCompat.requestPermissions(this, APP_PERMS, PERMISSION_REQUEST_CODE);
            mediaPlayerController.init(this);

            setDefaultPreferenceValues();
            AppRater.launchIfRequired(this);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setDefaultPreferenceValues() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getString(KEY_PREF_RECORDING_STORAGE_PATH, null) == null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_PREF_RECORDING_STORAGE_PATH, getBaseContext().getExternalFilesDir(null) + FileListFragment.DEFAULT_STORAGE_PATH);
            editor.apply();
        }
    }

    @Override
    protected void onStart() {
        try {
            Intent intent = new Intent(this, AudioRecorderLocalService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        super.onStart();
    }

    /**
     * @param view The method is the click handler for recorder delete button
     */
    public void deleteBtnClicked(View view) {
        try {
            recordingController.stopRecordingViaService(this, true);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * @param view The method is the click handler for recorder stop button
     */
    public void stopBtnClicked(View view) {
        try {
            recordingController.stopRecordingViaService(this, false);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public int getAudioSessionIdOfMediaPlayer() {
        return mediaPlayerController.getAudioSessionId();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isPermissionAccepted = false;
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                isPermissionAccepted = (result == PackageManager.PERMISSION_GRANTED);
                if (!isPermissionAccepted)
                    break;
            }
        }
        if (!isPermissionAccepted) {
            finish();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.list_fragment_container, FileListFragment.newInstance());
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_item_settings:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_item_about:
                AboutDialog aboutDialog = new AboutDialog(this);
                aboutDialog.show();
                return true;
            case R.id.menu_item_rate:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL)));
                if (editor != null) {
                    editor.putBoolean(AppRater.KEY_PREF_RATE_DIALOG_DONT_SHOW, true);
                    editor.apply();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void recordPauseBtnClicked(View view) {
        try {
            mediaPlayerController.stopPlaying(this);
            if (!isServiceBound) return;
            recordingController.startPauseRecording(this);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onFileItemClicked(Recording recording) {
        try {
            if (!AudioRecorderLocalService.MEDIA_REC_STATE.isStopped()) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.warn_stop_rec_to_play_audio), Toast.LENGTH_SHORT).show();
                return;
            }
            mediaPlayerController.playPauseAudio(this, recording);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onStop() {
        try {
            if (isServiceBound)
                unbindService(serviceConnection);
            isServiceBound = false;
            // https://stackoverflow.com/questions/2682043/how-to-check-if-receiver-is-registered-in-android
            LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            mediaPlayerController.releaseMediaPlayer();
            recordingController.onDestroy();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        super.onDestroy();
    }
}
