package com.andrazpencelj.soundfrequency;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by Andraž on 2.11.2013.
 */
public class Audio implements Runnable  {

    private AudioRecord mAudioRecord = null;
    private Thread mThread = null;
    private Handler mHandler = null;
    private int BUFFER_SIZE;
    private boolean RECORD;
    private int DATA_TYPE = 0;

    public Audio(Handler handler){
        /* konstruktor */
        try{
            mHandler = handler;
            BUFFER_SIZE = 4*mAudioRecord.getMinBufferSize(MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,BUFFER_SIZE);
        }
        catch (Exception e){
            Log.d("ERROR",e.toString());
        }
    }

    @Override
    public void run() {
        DoubleFFT_1D FFT = new DoubleFFT_1D(BUFFER_SIZE);
        short [] realAudioData = new short [BUFFER_SIZE];
        double [] doubleAudioData;
        double [] magnitude;
        double frequency = 0;
        double dB = 0;
        mAudioRecord.startRecording();
        /* nastavimo prioriteto tako, da nit tece v ozadju */
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while (RECORD){
            /* preberemo podatke iz mikrofona */
            mAudioRecord.read(realAudioData,0,BUFFER_SIZE);
            /* pretvorimo dobljene podatke iz tipa short v double */
            doubleAudioData = convertShortToDoubleArray(realAudioData);
            /* opravimo FFT nad podatki */
            FFT.realForward(doubleAudioData);
            /* izracunamo magnitude iz podatkov dobljenih po FFT */
            magnitude = magnitude(doubleAudioData);
            if (DATA_TYPE == 0){
                 /* izracunamo frekvenco */
                int peakIndeks = peakIndeks(magnitude);
                frequency = calculateFrequecny(peakIndeks);
            }
            else{
                /* izracunamo decibele */
                dB = calculateDecibels(avrageSignalPower(realAudioData));
            }
            /* posljemo podatke UI niti */
            sendMessageToUI(frequency,dB);
        }
        mAudioRecord.stop();
    }

    public void sendMessageToUI(double frequency,double dB){
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putDouble("frequency",frequency);
        bundle.putDouble("dB",dB);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    public void startRecording(int type){
        /* zacnemo z zajemanjem podatkov in iskanjem frekvence */
        /* type = 0 frekvenca, type = 1 decibeli */
        DATA_TYPE = type;
        RECORD = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void stopRecording(){
        /* prenehamo z zajemanjem podatkov */
        RECORD = false;
    }

    private double [] convertShortToDoubleArray(short [] originalData){
        /* pretvorba tabele s tipom short v tabelo s tipom double */
        double [] newData = new double[2*originalData.length];
        for (int i=0;i<originalData.length;i++){
            newData[i] = (double)originalData[i];
        }
        return  newData;
    }

    private double [] magnitude(double [] FFTdata){
        /* izracumamo magnitude signala in vrnemo tabelo */
        /* magnituda = koren(Re*Re+Im*Im) */
        double [] magnitude = new double[FFTdata.length/2];
        for (int i=0;i<FFTdata.length/2;i++){
            magnitude[i] = Math.sqrt(FFTdata[2*i]*FFTdata[2*i]+FFTdata[2*i+1]*FFTdata[2*i+1]);
        }
        return  magnitude;
    }

    private int peakIndeks(double [] FFTdata){
        /* v tabeli poiscemo najvecjo magnitudo in vrnemo njen indeks */
        int index = -1;
        double maxMagnitude = Double.NEGATIVE_INFINITY;
        for (int i=0;i<FFTdata.length/2;i++){
            if (maxMagnitude<Math.abs(FFTdata[i])){
                index = i;
                maxMagnitude = Math.abs(FFTdata[i]);
            }
        }
        return index;
    }

    private double calculateFrequecny(int index){
        /* izracunamo frekvenco */
        double frequency = index * (44100.0 / BUFFER_SIZE);
        return frequency;
    }

    private double calculateDecibels(double avgSignalPower){
        /* izracunamo decibele zaznanega signala */
        /* magnitudo delimo z 32768, kot neko referencno vrednost */
        double dB = 20.0 * Math.log10(avgSignalPower/Short.MAX_VALUE);
        return  dB;
    }

    private double avrageSignalPower(short [] amplitudes){
        /* izracunamo RMS (root mean square) */
        double avgSignalPower = 0;
        float avgAmplitudes = 0;
        for (int i=0;i<amplitudes.length;i++){
            avgAmplitudes += amplitudes[i]*amplitudes[i];
        }
        avgSignalPower = (double)(avgAmplitudes/amplitudes.length);
        return Math.sqrt(avgSignalPower);
    }

    public void release(){
        mAudioRecord.release();
    }
}
