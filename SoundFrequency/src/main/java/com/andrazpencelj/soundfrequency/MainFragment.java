package com.andrazpencelj.soundfrequency;



import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Andraž on 29.10.2013.
 */
public class MainFragment extends Fragment {

    private TextView mDataPanel,mDataUnit;
    private ImageView mImageView;
    private int APP_STATE = 0;
    private int APP_MODE = 0;
    private Audio mAudio;
    private Handler mHandler;

   @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        /* omogocimo dostop do action bara */
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onStart(){
        super.onStart();
        initPress();
        initHandler();
        initSound();
    }

    @Override
    public void onPause(){
        super.onPause();
        /*ce pride do pomika aplikacije v ozadje, ustavimo nit in prekinemo animacijo */
        mAudio.stopRecording();
        mImageView.clearAnimation();
    }

    @Override
    public void onStop(){
        super.onStop();
        mAudio.release();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_menu,menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_measurement:
                if (APP_MODE == 0){
                    item.setIcon(R.drawable.hz);
                    APP_MODE = 1;
                }
                else{
                    item.setIcon(R.drawable.db);
                    APP_MODE = 0;
                }
                changeMeasurement();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void initPress(){
        /* inicializacija poslusalca pritiska na sliko */
        mImageView = (ImageView)getActivity().findViewById(R.id.speaker_image);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (APP_STATE == 0) {
                    //zacnemo zajemati zvok
                    APP_STATE = 1;
                    mAudio.startRecording(APP_MODE);
                }
                else {
                    //prenehamo zajemati zvok
                    APP_STATE = 0;
                    mAudio.stopRecording();
                }
                changePicture();
            }
        });
    }

    public void initSound(){
        /* inicializacija panela z besedilom in kreiranjem objekta Audio */
        mDataPanel = (TextView)getActivity().findViewById(R.id.data_panel);
        mDataPanel.setText(R.string.no_data);
        mDataUnit = (TextView)getActivity().findViewById(R.id.measurement_unit);
        mAudio = new Audio(mHandler);
    }

    public void initHandler(){
        /* zandler za komunikacijo z nitjo */
        mHandler = new Handler(Looper.getMainLooper()){
            public void handleMessage(Message message) {
                Bundle bundle = message.getData();
                double frequency = bundle.getDouble("frequency");
                double dB = bundle.getDouble("dB");
                Log.d("MESSAGE",""+frequency+" "+dB);
                showResult(frequency, dB);
            }
        };
    }

    public void changePicture(){
        /* zamenjamo sliko, glede na to ali zajemamo zvok ali ne */
        if(APP_STATE == 0){
            /* ustavimo animacijo */
            mImageView.clearAnimation();
            mImageView.setImageResource(R.drawable.speaker_off);
        }
        else{
            mImageView.setImageResource(R.drawable.speaker_on);
        }
    }

    public void changeMeasurement(){
        /* ustavimo prejsnje merjenje in zacnemo novo */
        mAudio.stopRecording();
        mImageView.clearAnimation();
        mAudio.startRecording(APP_MODE);
        if (APP_MODE == 0){
            mDataUnit.setText(R.string.frequency_unit);
        }
        else{
            mDataUnit.setText(R.string.db_unit);
        }

    }

    public void showResult(double frequency,double dB){
        /* izpisemo rezultat */
        if (APP_MODE == 0){
            /* izpisujemo frekvence */
            if (APP_STATE == 0){
                mDataPanel.setText(R.string.no_data);
            }
            else{
                frequency = Math.round(frequency);
                mDataPanel.setText(""+(int)frequency);
                //animateFrequency(frequency);
            }
        }
        else{
            /* izpisujemo decibele */
            if (APP_STATE == 0){
                mDataPanel.setText(R.string.no_data);
            }
            else{
                dB = Math.round(dB);
                mDataPanel.setText(""+(int)dB);
                //animatedB(dB);
            }
        }
    }

    public void animateFrequency(double frequency){
        if ((frequency < 150)&&(frequency > 0)){
            Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.speaker_animation_hard);
            mImageView.startAnimation(animation);
        }
        else if ((frequency>150)&&(frequency<500)){
            Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.speaker_animation_medium);
            mImageView.startAnimation(animation);
        }
        else if (frequency>500){
            Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.speaker_animation_soft);
            mImageView.startAnimation(animation);
        }
    }

    public void animatedB(double dB){
        if ((dB > -20)){
            Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.speaker_animation_hard);
            mImageView.startAnimation(animation);
        }
        else if((dB<-20)&&(dB>-40)){
            Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.speaker_animation_medium);
            mImageView.startAnimation(animation);
        }
        else if (dB<-40){
            Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.speaker_animation_soft);
            mImageView.startAnimation(animation);
        }

    }
}
