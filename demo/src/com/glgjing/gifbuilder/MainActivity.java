package com.glgjing.gifbuilder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.glgjing.gifencoder.BitmapExtractor;
import com.glgjing.gifencoder.GIFEncoder;

import java.util.List;


public class MainActivity extends Activity {

  private static final int REQUEST_SELECT_VIDEO = 1;
  private String filePath;
  private enum State {INIT, READY, BUILDING, COMPLETE}
  private State state = State.INIT;

  private TextView selectVideo;
  private TextView tip;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    selectVideo = (TextView) findViewById(R.id.select_video);
    selectVideo.setOnClickListener(clickListener);
    tip = (TextView) findViewById(R.id.tip);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SELECT_VIDEO) {
      if (resultCode == RESULT_OK) {
        Uri videoUri = data.getData();
        filePath = getRealFilePath(videoUri);
        state = State.READY;
        selectVideo.setText(R.string.create_gif);
        tip.setText(R.string.building_init);
      }
    }
  }

  public String getRealFilePath(Uri uri ) {
    String path = uri.getPath();
    String[] pathArray = path.split(":");
    String fileName = pathArray[pathArray.length - 1];
    return /*Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + */fileName;
  }

  private View.OnClickListener clickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (state == State.INIT || state == State.COMPLETE) {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_SELECT_VIDEO);

      } else if (state == State.READY) {
        new AsyncTask<Void, Void, Void>() {

          @Override
          protected void onPreExecute() {
            state = State.BUILDING;
            tip.setText(R.string.building_gif);
          }

          @Override
          protected Void doInBackground(Void... params) {

            BitmapExtractor extractor = new BitmapExtractor();
            extractor.setFPS(4);
            extractor.setScope(0, 5);
            extractor.setSize(540, 960);
            List<Bitmap> bitmaps = extractor.createBitmaps(filePath);

            String fileName = String.valueOf(System.currentTimeMillis()) + ".gif";
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
            GIFEncoder encoder = new GIFEncoder();
            encoder.init(bitmaps.get(0));
            encoder.start(filePath);
            for (int i = 1; i <bitmaps.size(); i++) {
              encoder.addFrame(bitmaps.get(i));
            }
            encoder.finish();
            return null;
          }

          @Override
          protected void onPostExecute(Void aVoid) {
            state = State.COMPLETE;
            tip.setText(R.string.building_complete);
            selectVideo.setText(R.string.select_video);
            Toast.makeText(getApplicationContext(), "存储路径" + filePath, Toast.LENGTH_LONG).show();
          }
        }.execute();
      }
    }
  };
}
