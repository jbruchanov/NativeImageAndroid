package com.scurab.android.nativeimageapp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.scurab.andriod.nativeimage.NativeImage;

/**
 * Created by JBruchanov on 15/04/2017.
 */

public class LargeImageFragment extends Fragment {

    private ImageView mImageView;
    private NativeImage mImage;
    private TextView mLabel;
    private Bitmap mBitmap;
    private ProgressDialog mDialog;
    private SeekBar mOffsetX;
    private SeekBar mOffsetY;
    private ImageView mPreview;
    
    private String mImageFile;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mImageFile = getResources().getDisplayMetrics().widthPixels < 800 ? MainActivity.LARGE_IMAGE_100MPIX : mImageFile;
        return inflater.inflate(R.layout.fragment_large_image, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImageView = (ImageView) view.findViewById(R.id.image);
        mPreview = (ImageView) view.findViewById(R.id.preview);
        mLabel = (TextView) view.findViewById(R.id.label);
        mOffsetX = (SeekBar) view.findViewById(R.id.offsetX);
        mOffsetY = (SeekBar) view.findViewById(R.id.offsetY);
        final SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onUpdatePreview();
            }
        };
        mOffsetX.setOnSeekBarChangeListener(listener);
        mOffsetY.setOnSeekBarChangeListener(listener);
    }

    private void onUpdatePreview() {
        if (mImage != null && mBitmap != null) {
            final NativeImage.MetaData metaData = mImage.getMetaData();
            int offsetX = (int) ((metaData.width - mBitmap.getWidth()) / 100f * mOffsetX.getProgress());
            int offsetY = (int) ((metaData.height - mBitmap.getHeight()) / 100f * mOffsetY.getProgress());
            mImage.setPixels(mBitmap, offsetX, offsetY, mBitmap.getWidth(), mBitmap.getHeight());
            Log.d("Preview", String.format("Offset x:%s y:%s", offsetX, offsetY));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadImage();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
            mImageView.setImageBitmap(null);
        }
        if (mImage != null) {
            mImage.dispose();
            mImage = null;
        }
    }

    private void loadImage() {
        if (mImage == null) {
            mImage = new NativeImage();
            mDialog = ProgressDialog.show(getActivity(), "Loading", null, true, false);
            final String path = getImagePath(mImageFile);
            new AsyncTask<Void, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... params) {
                    mImage.loadImage(path);
                    final NativeImage.MetaData metaData = mImage.getMetaData();
                    mBitmap = Bitmap.createBitmap(metaData.width / 10, metaData.height / 10, Bitmap.Config.ARGB_8888);
                    mImage.setPixels(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight());
                    return mBitmap;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    mImageView.setImageBitmap(bitmap);
                    mPreview.setImageBitmap(mImage.asScaledBitmap(getResources().getDisplayMetrics().widthPixels / 2, 0));
                    final NativeImage.MetaData metaData = mImage.getMetaData();
                    mLabel.setText(String.format("Allocated:%.2f megs Image:%sx%s", metaData.getAllocatedBytes() / 1024f / 1024f, metaData.width, metaData.height));
                    try {
                        mDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }
    }

    public String getImagePath(String image) {
        return ((MainActivity) getActivity()).getImagePath(image);
    }
}
