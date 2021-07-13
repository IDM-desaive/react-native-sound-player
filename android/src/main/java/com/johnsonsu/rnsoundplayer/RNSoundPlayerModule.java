package com.johnsonsu.rnsoundplayer;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;

import java.io.File;

import java.io.IOException;

import javax.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;

import okhttp3.OkHttpClient;


public class RNSoundPlayerModule extends ReactContextBaseJavaModule {

  public final static String EVENT_FINISHED_PLAYING = "FinishedPlaying";
  public final static String EVENT_FINISHED_LOADING = "FinishedLoading";
  public final static String EVENT_FINISHED_LOADING_FILE = "FinishedLoadingFile";
  public final static String EVENT_FINISHED_LOADING_URL = "FinishedLoadingURL";

  private final ReactApplicationContext reactContext;
  private MediaPlayer mediaPlayer;
  private float volume;

  private OkHttpClient okHttpClient;

  public RNSoundPlayerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.volume = 1.0f;
  }

  @Override
  public String getName() {
    return "RNSoundPlayer";
  }

  @ReactMethod
  public void playSoundFile(String name, String type) throws IOException {
    mountSoundFile(name, type);
    this.resume();
  }

  @ReactMethod
  public void loadSoundFile(String name, String type) throws IOException {
    mountSoundFile(name, type);
  }

  @ReactMethod
  public void playUrl(String url) throws IOException {
    prepareUrl(url);
    this.resume();
  }

  @ReactMethod
  public void loadUrl(String url) throws IOException {
    prepareUrl(url);
  }

  @ReactMethod
  public void pause() throws IllegalStateException {
    if (this.mediaPlayer != null) {
      this.mediaPlayer.pause();
    }
  }

  @ReactMethod
  public void resume() throws IOException, IllegalStateException {
    if (this.mediaPlayer != null) {
      this.setVolume(this.volume);
      this.mediaPlayer.start();
    }
  }

  @ReactMethod
  public void stop() throws IllegalStateException {
    if (this.mediaPlayer != null) {
      this.mediaPlayer.stop();
    }
  }

  @ReactMethod
  public void seek(float seconds) throws IllegalStateException {
    if (this.mediaPlayer != null) {
      this.mediaPlayer.seekTo((int)seconds * 1000);
    }
  }

  @ReactMethod
  public void setVolume(float volume) throws IOException {
    this.volume = volume;
    if (this.mediaPlayer != null) {
      this.mediaPlayer.setVolume(volume, volume);
    }
  }

  @ReactMethod
  public void getInfo(
          Promise promise) {
    if (this.mediaPlayer == null) {
      promise.resolve(null);
      return;
    }
    WritableMap map = Arguments.createMap();
    map.putDouble("currentTime", this.mediaPlayer.getCurrentPosition() / 1000.0);
    map.putDouble("duration", this.mediaPlayer.getDuration() / 1000.0);
    promise.resolve(map);
  }

  private void sendEvent(ReactApplicationContext reactContext,
          String eventName,
          @Nullable WritableMap params) {
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  private void mountSoundFile(String name, String type) throws IOException {
    if (this.mediaPlayer == null) {
      int soundResID = getReactApplicationContext().getResources().getIdentifier(name, "raw", getReactApplicationContext().getPackageName());

      if (soundResID > 0) {
        this.mediaPlayer = MediaPlayer.create(getCurrentActivity(), soundResID);
      } else {
        this.mediaPlayer = MediaPlayer.create(getCurrentActivity(), this.getUriFromFile(name, type));
      }

      this.mediaPlayer.setOnCompletionListener(
              new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                  WritableMap params = Arguments.createMap();
                  params.putBoolean("success", true);
                  sendEvent(getReactApplicationContext(), EVENT_FINISHED_PLAYING, params);
                }
              });
    } else {
      Uri uri;
      int soundResID = getReactApplicationContext().getResources().getIdentifier(name, "raw", getReactApplicationContext().getPackageName());

      if (soundResID > 0) {
        uri = Uri.parse("android.resource://" + getReactApplicationContext().getPackageName() + "/raw/" + name);
      } else {
        uri = this.getUriFromFile(name, type);
      }

      this.mediaPlayer.reset();
      this.mediaPlayer.setDataSource(getCurrentActivity(), uri);
      this.mediaPlayer.prepare();
    }

    WritableMap params = Arguments.createMap();
    params.putBoolean("success", true);
    sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING, params);
    WritableMap onFinishedLoadingFileParams = Arguments.createMap();
    onFinishedLoadingFileParams.putBoolean("success", true);
    onFinishedLoadingFileParams.putString("name", name);
    onFinishedLoadingFileParams.putString("type", type);
    sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING_FILE, onFinishedLoadingFileParams);
  }

  private Uri getUriFromFile(String name, String type) {
    String folder = getReactApplicationContext().getFilesDir().getAbsolutePath();
    String file = name + "." + type;

    // http://blog.weston-fl.com/android-mediaplayer-prepare-throws-status0x1-error1-2147483648
    // this helps avoid a common error state when mounting the file
    File ref = new File(folder + "/" + file);

    if (ref.exists()) {
      ref.setReadable(true, false);
    }

    return Uri.parse("file://" + folder + "/" + file);
  }

  private void setMediaPlayerListeners(final String url) {
    if (this.mediaPlayer == null) {
      return;
    }

    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
      @Override
      public boolean onError(MediaPlayer mp, int what, int extra) {
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
        mp.reset();

        WritableMap params = Arguments.createMap();
        params.putBoolean("success", false);
        params.putString("url", url);

        WritableMap extraMap = Arguments.createMap();
        extraMap.putInt("what", extra);
        extraMap.putString("whatString", whatString);
        extraMap.putInt("extra", extra);
        extraMap.putString("extraString", extraString);
        params.putMap("extra", extraMap);

        sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING_URL, params);

        return true;
      }
    });
    this.mediaPlayer.setOnCompletionListener(
            new OnCompletionListener() {
              @Override
              public void onCompletion(MediaPlayer arg0) {
                WritableMap params = Arguments.createMap();
                params.putBoolean("success", true);
                sendEvent(getReactApplicationContext(), EVENT_FINISHED_PLAYING, params);
              }
            });
    this.mediaPlayer.setOnPreparedListener(
            new OnPreparedListener() {
              @Override
              public void onPrepared(MediaPlayer mediaPlayer) {
                WritableMap onFinishedLoadingURLParams = Arguments.createMap();
                onFinishedLoadingURLParams.putBoolean("success", true);
                onFinishedLoadingURLParams.putString("url", url);
                sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING_URL, onFinishedLoadingURLParams);
              }
            }
    );
  }

  private void prepareUrl(final String url) throws IOException {
    Uri uri = Uri.parse(url);
    if (this.mediaPlayer == null) {
      this.mediaPlayer = new MediaPlayer();
      this.setMediaPlayerListeners(url);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC).build());
      } else {
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      }
    } else {
      this.mediaPlayer.reset();
    }
    setDataSource(uri);
    this.mediaPlayer.prepareAsync();

    WritableMap params = Arguments.createMap();
    params.putBoolean("success", true);
    sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING, params);
  }

  private void setDataSource(final Uri uri) throws IOException {
    mediaPlayer.setDataSource(getCurrentActivity(), uri);
  }
}
