package com.scurab.android.nativeimageapp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.scurab.andriod.nativeimage.NativeImage;

/**
 * Created by JBruchanov on 15/04/2017.
 */

public class EffectsFragment extends Fragment {

    private final static String[] EFFECTS = new String[]{
            "--",
            "grayScale",
            "crop",
            "brightness",
            "contrast",
            "gamma",
            "inverse",
            "flipv",
            "fliph",
            "naiveResize"
    };

    NativeImage mImage;
    ImageView mImageView;
    private Spinner mSpinner;
    private ProgressDialog mDialog;
    private Bitmap mBitmap;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_effects, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImage = new NativeImage();
        mImageView = (ImageView) view.findViewById(R.id.image);
        mSpinner = (Spinner)view.findViewById(R.id.selector);
        mImage.loadImage(getImagePath(MainActivity.IMAGE_1));
        mImageView.setImageBitmap(mImage.asBitmap());

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, EFFECTS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        view.findViewById(R.id.apply_effect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onApplySelected(EFFECTS[mSpinner.getSelectedItemPosition()]);
            }
        });

        view.findViewById(R.id.reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mImage.loadImage(getImagePath(MainActivity.IMAGE_1));
                    releaseBitmap();
                    mBitmap = mImage.asScaledBitmap(getResources().getDisplayMetrics().widthPixels, 0);
                    mImageView.setImageBitmap(mBitmap);
                } catch (OutOfMemoryError outOfMemoryError) {
                    Toast.makeText(getContext(), outOfMemoryError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void releaseBitmap() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    private void onApplySelected(String effect) {
        final NativeImage.EffectBuilder effectBuilder = new NativeImage.EffectBuilder();
        final NativeImage.MetaData metaData = mImage.getMetaData();
        switch (effect) {
            default:
            case "--":
                return;
            case "grayScale":
                effectBuilder.grayScale();
                break;
            case "crop":
                if (metaData.width > 2 && metaData.height > 2) {
                    effectBuilder.crop(0, 0, metaData.width / 2, metaData.height / 2);
                }
                break;
            case "brightness":
                effectBuilder.brightness(10);
                break;
            case "contrast":
                effectBuilder.brightness(10);
                break;
            case "gamma":
                effectBuilder.gamma(0.25f);
                break;
            case "inverse":
                effectBuilder.inverse();
                break;
            case "flipv":
                effectBuilder.flipVertical();
                break;
            case "fliph":
                effectBuilder.flipHorizontal();
                break;
            case "naiveResize":
                if (metaData.width > 2 && metaData.height > 2) {
                    effectBuilder.naiveDownscale(metaData.width / 2, metaData.height / 2);
                }
                break;
        }
        mDialog = ProgressDialog.show(getContext(), effect, null, true, false);
        releaseBitmap();
        //noinspection unchecked
        new AsyncTask<Object, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object[] params) {
                try {
                    mImage.applyEffect(effectBuilder.build());
                    return mImage.asScaledBitmap(getResources().getDisplayMetrics().widthPixels, 0);
                } catch (Throwable e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap o) {
                try {
                    mBitmap = o;
                    mDialog.dismiss();
                    mDialog = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mImageView != null) {
                    mImageView.setImageBitmap(o);
                }
            }
        }.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mImage != null) {
            mImage.dispose();
            mImage = null;
        }
    }

    public String getImagePath(String image) {
        return ((MainActivity) getActivity()).getImagePath(image);
    }
}
