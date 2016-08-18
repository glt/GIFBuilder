package com.glgjing.gifbuilder;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.Voice;
import android.widget.Toast;

import com.glgjing.gifencoder.GIFEncoder;

import java.io.File;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    new AsyncTask<Void, Void, Void>() {

      @Override
      protected Void doInBackground(Void... params) {
        String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScreenRecord" + "/test.gif";
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.a0);
        GIFEncoder encoder = new GIFEncoder();
        encoder.init(bitmap);
        encoder.start(rootDir);
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a1));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a2));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a3));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a4));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a5));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a6));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a7));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a8));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a9));
        encoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.a10));
        encoder.finish();
        return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        Toast.makeText(getApplicationContext(), "complete", Toast.LENGTH_LONG).show();
      }
    }.execute();
  }
}
