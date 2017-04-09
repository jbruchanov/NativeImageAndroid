package com.scurab.android.nativeimageapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.scurab.andriod.nativeimage.NativeImage;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private HashMap<String, String> mMap = new HashMap<>();
    private List<NativeImage> mImages = new ArrayList<>();
    private static final String[] FILES = new String[]{"gradient.png", "image1.jpg"};

    private LinearLayout mRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRoot = (LinearLayout) findViewById(R.id.root);
        try {
            copyAssets();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void copyAssets() throws IOException {
        for (String file : FILES) {
            File output = new File(getFilesDir(), file);
            if (!(output.exists() && output.length() > 0)) {
                InputStream is = getAssets().open(file);
                IOUtils.copy(is, new FileOutputStream(output));
            }
            mMap.put(file, output.getAbsolutePath());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseImages();
    }

    private void loadImages() {
        for (Map.Entry<String, String> entry : mMap.entrySet()) {
            ImageView iv = new ImageView(this);
            mRoot.addView(iv);
            NativeImage ni = new NativeImage(NativeImage.ColorSpace.RGB);
            int result = ni.loadImage(entry.getValue());
            if (NativeImage.NO_ERR == result) {
                iv.setImageBitmap(ni.asBitmap());
            }
            mImages.add(ni);
        }
    }

    private void releaseImages() {
        for (NativeImage image : mImages) {
            image.dispose();
        }
        mImages.clear();
    }
}
