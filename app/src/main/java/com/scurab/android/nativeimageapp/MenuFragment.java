package com.scurab.android.nativeimageapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by JBruchanov on 15/04/2017.
 */

public class MenuFragment extends Fragment {

    private final static Class[] FRAGMENTS = new Class<?>[]{
            EffectsFragment.class,
            LargeImageFragment.class
    };

    private LinearLayoutCompat mRoot;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRoot = new LinearLayoutCompat(inflater.getContext());
        mRoot.setOrientation(LinearLayoutCompat.VERTICAL);
        return mRoot;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        for (Class fragment : FRAGMENTS) {
            Button b = new Button(view.getContext());
            b.setTag(fragment);
            b.setText(fragment.getSimpleName().replaceAll("Fragment", ""));
            b.setOnClickListener(mClickListener);
            mRoot.addView(b);
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((MainActivity)getActivity()).openFragment((Class<? extends Fragment>) v.getTag());
        }
    };
}
