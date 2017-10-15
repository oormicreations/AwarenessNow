package in.oormi.awarenessnow;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    PendingIntent pendingIntent;
    BroadcastReceiver broadcastReceiver;
    AlarmManager alarmManager;

    public boolean noNightRem = false;
    public int remFreq = 20;
    public int randomDelayRange = 5;
    public boolean demoMode = false;
    private ArrayList<String> remList = new ArrayList<>();
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
//        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
//        PreferenceManager.setDefaultValues(this, R.xml.pref_headers, false);
//        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        getReminders();
        setupBroadcastReceiver();

        final ToggleButton toggleRem = (ToggleButton) findViewById(R.id.toggleButtonSwan);
        toggleRem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    dispRem();  //initial rem without a delay
                    startTimer();
                    setAnimation();
                } else {
                    stopTimer();
                    toggleRem.clearAnimation();
                }
            }
        });
    }

    private void getReminders() {
        String[] defaultRems = getResources().getStringArray(R.array.rems);
        remList.clear();
        for (int i = 0; i < defaultRems.length; i++) {
            remList.add(defaultRems[i]);
        }

    }

    private void setAnimation() {
        final ToggleButton toggleRem = (ToggleButton) findViewById(R.id.toggleButtonSwan);
/*
        AlphaAnimation fade_in = new AlphaAnimation(0.2f, 1.0f);
        fade_in.setDuration(1000);
        fade_in.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationStart(Animation arg0)
            {
            }
            public void onAnimationRepeat(Animation arg0)
            {
            }

            public void onAnimationEnd(Animation arg0)
            {
                toggleRem.setVisibility(View.VISIBLE);
                Animation animation = AnimationUtils.loadAnimation(MainActivity.this, 
                        R.anim.remstart);
                toggleRem.startAnimation(animation);
            }
        });
        toggleRem.startAnimation(fade_in);
        */
        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.remstart);
        toggleRem.startAnimation(animation);

    }

    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.infomenu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menuShare);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider)  MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=in.oormi.awarenessnow");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this app!");
        setShareIntent(shareIntent);
        return true;
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuInfo:
                Intent i = new Intent(this, ResourceShow.class);
                startActivity(i);
                break;

            case R.id.menuSettings:
                break;

            case R.id.menuEdit:
                editReminders();
                break;

            case R.id.menuDemo:
                break;

            case R.id.menuShare:
                break;
        }
        return true;
    }

    private void editReminders() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        final LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);


        final ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        layout.setGravity(Gravity.CLIP_VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        final TextView tv = new TextView(this);
        tv.setText(getString(R.string.editRemTitle));
        tv.setPadding(40, 40, 40, 40);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(20);

        final ArrayList<EditText> etRemList = new ArrayList<>();
        for (int i=0;i<remList.size();i++) {
            final EditText etRem = new EditText(this);
            etRem.setHint(R.string.hintRem);
            etRem.setText(remList.get(i));
            layout.addView(etRem);
            etRemList.add(etRem);
        }

        alertDialogBuilder.setView(scrollView);
        alertDialogBuilder.setCustomTitle(tv);

        alertDialogBuilder.setNegativeButton(R.string.editCancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.setPositiveButton(R.string.editApply,
                new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                remList.clear();
                for (int i =0; i < etRemList.size(); i++){
                    String s = etRemList.get(i).getText().toString();
                    if (!s.isEmpty())remList.add(s);
                }
            }
        });

        alertDialogBuilder.setNeutralButton(R.string.editAdd,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });

        AlertDialog edRemDialog = alertDialogBuilder.create();
        try {
            edRemDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

        edRemDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final EditText etRem = new EditText(MainActivity.this);
                etRem.setHint(R.string.hintRem);
                layout.addView(etRem);
                etRemList.add(etRem);
                etRem.requestFocus();
            }
        });
    }

    private void setupBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {

                boolean enabled = false;
                if (noNightRem) {
                    //get the current time
                    Calendar calendar = Calendar.getInstance();
                    //calendar.set(Calendar.HOUR_OF_DAY, 17);
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    boolean day = (hour<22) && (hour>5);
                    if (day){enabled = true;}
                }
                else {enabled = true;}

                if (enabled) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP, "AwareAppTag");
                    wl.acquire();
                    dispRem();
                    stopTimer();
                    startTimer();//starts with a new random delay
                    wl.release();
                }
                else {//keep looping anyway
                    stopTimer();
                    startTimer();//starts with a new random delay
                }

            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("in.oormi.awake") );
        pendingIntent = PendingIntent.getBroadcast( this, 0, new Intent("in.oormi.awake"), 0 );
        alarmManager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void startTimer() {
        Random rand = new Random();
        int rdelay = 0;
        if (randomDelayRange >0) {
            rdelay = rand.nextInt(randomDelayRange);
        }

        int delay = 1000 * (remFreq + rdelay);
        String toastinfo = String.valueOf(delay/60000) + getString(R.string.nxtremmintoast);
        if(demoMode){
            delay = 30000;
            toastinfo = "30 sec (Demo Mode)";
        }
        //Toast.makeText(this, getString(R.string.nxtremtoast) + toastinfo, Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, pendingIntent);
        }
        else{
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, pendingIntent);
        }
    }

    public void stopTimer() {
        if(alarmManager !=null){
            if(pendingIntent !=null){
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    private void dispRem(){
        if (remList.size()<1) return;

        Random rand = new Random();
        int value = rand.nextInt(remList.size());

        final TextView tvRem = (TextView)findViewById(R.id.textViewRem);
        tvRem.setText(remList.get(value));

        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.remzoom);
        tvRem.startAnimation(animation);

        SharedPreferences getAlarms = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String alarms = getAlarms.getString("notifications_new_message_ringtone",
                "content://settings/system/notification_sound");
        boolean toneenable = getAlarms.getBoolean("notifications_new_message", true);
        if (toneenable) {
            Uri uri = Uri.parse(alarms);
            playSound(this, uri);
        }

        boolean vibeenable = getAlarms.getBoolean("notifications_new_message_vibrate", false);
        if (vibeenable) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        }

        boolean ttsenable = getAlarms.getBoolean("notifications_new_message_tts", true);
        if(ttsenable){
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                @Override
                public void onInit(int status) {
                    if(status == TextToSpeech.SUCCESS){
                        int result=tts.setLanguage(Locale.UK);
                        if(result==TextToSpeech.LANG_MISSING_DATA ||
                                result==TextToSpeech.LANG_NOT_SUPPORTED){
                            Log.e("TTS Error: ", "This Language is not supported");
                        }
                        else{
                            tts.speak(tvRem.getText(), TextToSpeech.QUEUE_FLUSH, null,
                                    TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                        }
                    }
                    else
                        Log.e("TTS Error: ", "Initialization Failed!");
                }
            });

        }
    }


    private void playSound(Context context, Uri alert) {
        MediaPlayer mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, alert);
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            System.out.println("Error playing tone");
        }
    }

    @Override
    protected void onDestroy() {
        if(alarmManager !=null){
            if(pendingIntent !=null){
                alarmManager.cancel(pendingIntent);
            }
        }
        if(broadcastReceiver !=null){
            unregisterReceiver(broadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                moveTaskToBack(true);
                return true;
        }
        return false;
    }

}

