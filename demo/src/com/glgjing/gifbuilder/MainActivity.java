package com.glgjing.gifbuilder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;


public class MainActivity extends Activity {

  private static final int REQUEST_SELECT_VIDEO = 1;
  private String filePath;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.select_video).setOnClickListener(clickListener);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SELECT_VIDEO) {
      if (resultCode == RESULT_OK) {
        Uri videoUri = data.getData();
        filePath = getRealFilePath(videoUri);
      }
    }
  }

  public String getRealFilePath(Uri uri ) {
    String path = uri.getPath();
    String[] pathArray = path.split(":");
    String fileName = pathArray[pathArray.length - 1];
    return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
  }

  private View.OnClickListener clickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent();
      intent.setType("video/*");
      intent.setAction(Intent.ACTION_GET_CONTENT);
      startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_SELECT_VIDEO);
    }
  };
}
