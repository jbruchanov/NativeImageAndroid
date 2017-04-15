package com.scurab.android.nativeimageapp;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

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

    public final static String IMAGE_1 = "image1.jpg";

    private HashMap<String, String> mMap = new HashMap<>();
    private static final String[] FILES = new String[]{"gradient.png", IMAGE_1/*, "200mpix.jpg"*/};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openFragment(MenuFragment.class);
        try {
            copyAssets();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        openFragment(MenuFragment.class);
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

//    private void loadImages() {
//        for (Map.Entry<String, String> entry : mMap.entrySet()) {
//            ImageView iv = new ImageView(this);
//            mRoot.addView(iv);
//            NativeImage ni = new NativeImage(NativeImage.ColorSpace.RGB);
//            int result = ni.loadImage(entry.getValue());
//            ni.applyEffect(new NativeImage.EffectBuilder().grayScale().build());
//            if (NativeImage.NO_ERR == result) {
//                iv.setImageBitmap(ni.asBitmap());
//            }
//            mImages.add(ni);
//        }
//    }
//
//    private void releaseImages() {
//        for (NativeImage image : mImages) {
//            image.dispose();
//        }
//        mImages.clear();
//    }

    public void openFragment(Class<? extends Fragment> tag) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, tag.newInstance(), tag.getName())
                    .commit();
        } catch (Throwable e) {
            showToast(e.getMessage());
            e.printStackTrace();
        }
    }

    public String getImagePath(String image) {
        return mMap.get(image);
    }

    public void showToast(CharSequence msg) {
        Toast.makeText(this, msg != null ? msg : "NullMsg", Toast.LENGTH_LONG).show();
    }
}
