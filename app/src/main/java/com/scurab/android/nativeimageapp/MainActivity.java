package com.scurab.android.nativeimageapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public final static String IMAGE_1 = "image1.jpg";
    public final static String LARGE_IMAGE_200MPIX = "200mpix.jpg";
    public final static String LARGE_IMAGE_100MPIX = "100mpix.jpg";

    private HashMap<String, String> mMap = new HashMap<>();
    private static final String[] FILES = new String[]{"gradient.png", IMAGE_1, LARGE_IMAGE_100MPIX, LARGE_IMAGE_200MPIX};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            copyAssets();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        openFragment(MenuFragment.class, false);
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

    public void openFragment(Class<? extends Fragment> tag) {
        openFragment(tag, true);
    }

    public void openFragment(Class<? extends Fragment> tag, boolean addToBackStack) {
        try {
            final FragmentTransaction t = getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, tag.newInstance(), tag.getName());
            if (addToBackStack) {
                t.addToBackStack(null);
            }
            t.commit();
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
