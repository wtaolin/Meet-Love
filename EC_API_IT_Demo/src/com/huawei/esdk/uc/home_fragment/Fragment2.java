package com.huawei.esdk.uc.home_fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huawei.esdk.uc.FakeVideoActivity;
import com.huawei.esdk.uc.MainActivityOld;
import com.huawei.esdk.uc.R;

/**
 * Created by lance on 10/15/16.
 */
public class Fragment2 extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, null);
        initView(view);
        return view;
    }
    public void initView(View view)
    {
        View btn = view.findViewById(R.id.tap_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), FakeVideoActivity.class);
                startActivity(i);
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent i = new Intent(getActivity(), MainActivityOld.class);
                startActivity(i);
                return false;
            }
        });

    }
}
