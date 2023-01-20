package com.johnsonsu.rnsoundplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;

import java.io.IOException;

public class RNMediaPlayer implements IRNMediaPlayer {
    private final MediaPlayer mediaPlayer;
    private final Context context;
    private final ReactApplicationContext reactApplicationContext;
    private IRNOnErrorListener onErrorListener;
    private IRNOnPreparedListener onPreparedListener;
    private IRNOnCompletionListener onCompletionListener;

    public RNMediaPlayer(Context context, ReactApplicationContext reactApplicationContext) {
        mediaPlayer = new MediaPlayer();
        this.context = context;
        this.reactApplicationContext = reactApplicationContext;
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (onErrorListener != null) {
                    String whatString = "Media Error Unknown";
                    if (MediaPlayer.MEDIA_ERROR_SERVER_DIED == what) {
                        whatString = "Media Error Server Died";
                    }
                    String extraString = "Unknown";
                    switch (extra) {
                        case MediaPlayer.MEDIA_ERROR_IO:
                            extraString = "Media Error IO";
                            break;
                        case MediaPlayer.MEDIA_ERROR_MALFORMED:
                            extraString = "Media Error Malformed";
                            break;
                        case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                            extraString = "Media Error Unsupported";
                            break;
                        case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                            extraString = "Media Error Timed Out";
                            break;
                        case -2147483648:
                            extraString = "Low-level System Error";
                            break;
                    }
                    return onErrorListener.onError(
                            RNMediaPlayer.this,
                            what,
                            extra,
                            whatString,
                            extraString);
                }
                return false;
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (onPreparedListener != null) {
                    onPreparedListener.onPrepared(RNMediaPlayer.this);
                }
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (onCompletionListener != null) {
                    onCompletionListener.onCompletion(RNMediaPlayer.this);
                }
            }
        });
    }

    @Override
    public void play() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public void stop() {
        mediaPlayer.stop();
    }

    @Override
    public void setUri(Uri uri) throws IOException {
        mediaPlayer.setDataSource(context, uri);
    }

    @Override
    public void prepare() throws IOException {
        mediaPlayer.prepareAsync();
    }

    @Override
    public void reset() {
        mediaPlayer.reset();
    }

    @Override
    public void seekTo(int msec) {
        mediaPlayer.seekTo(msec);
    }

    @Override
    public void setVolume(float volume) {
        mediaPlayer.setVolume(volume, volume);
    }

    @Override
    public long getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public void setOnCompletionListener(IRNOnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    @Override
    public void setOnErrorListener(IRNOnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    @Override
    public void setOnPreparedListener(IRNOnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    @Override
    public void setRawResourceId(int resourceId) throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(resourceId);
        if (afd == null) return;
        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
    }

    @Override
    public void getDeviceVolume(Promise promise) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float pct = (float) currentVolume / (float) maxVolume;
        promise.resolve(pct);
    }

    @Override
    public void isDeviceMuted(Promise promise) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            promise.resolve(audioManager.isStreamMute(AudioManager.STREAM_MUSIC));
        } else {
            promise.resolve(false);
        }
    }
}
