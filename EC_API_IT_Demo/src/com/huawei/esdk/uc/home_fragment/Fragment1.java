package com.huawei.esdk.uc.home_fragment;

/**
 * Created by lance on 10/15/16.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.huawei.contacts.SelfDataHandler;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.VoipFunc;

/**
 * Created by Carson_Ho on 16/5/23.
 */
public class Fragment1 extends Fragment

{

    private RadioGroup radioGroup;

    private EditText edCallBackNum;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_set, null);
        initView(view);
        return view;
    }

    private void initView(View view)
    {
        Button done_bt = (Button) view.findViewById(R.id.done);
        done_bt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toSave();
                //finish();  TODO??
            }
        });

        radioGroup = (RadioGroup) view.findViewById(R.id.group_type);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                switch (checkedId)
                {
                    case R.id.ctd:

                        SelfDataHandler.getIns().getSelfData()
                                .setCallType(VoipFunc.CTD);
                        break;

                    case R.id.voip:

                        SelfDataHandler.getIns().getSelfData()
                                .setCallType(VoipFunc.VOIP);
                        break;

                    case R.id.video:

                        SelfDataHandler.getIns().getSelfData()
                                .setCallType(VoipFunc.VIDEO);
                        break;

                    default:
                        break;
                }
            }
        });

        setCallType();

        edCallBackNum = (EditText) view.findViewById(R.id.callbackNum);
        edCallBackNum.setText(SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb());
    }

    private void toSave()
    {
        String callBackNum = edCallBackNum.getText().toString().trim();
        if (("").equals(callBackNum))
        {
            SelfDataHandler.getIns().getSelfData().setCallbackNumber("");
        }
        else
        {
            SelfDataHandler.getIns().getSelfData()
                    .setCallbackNumber(callBackNum);
        }
    }

    private void setCallType()
    {
        switch (SelfDataHandler.getIns().getSelfData().getCallType())
        {
            case VoipFunc.CTD:

                radioGroup.check(R.id.ctd);
                break;

            case VoipFunc.VOIP:

                radioGroup.check(R.id.voip);
                break;

            case VoipFunc.VIDEO:

                radioGroup.check(R.id.video);
                break;

            default:

                radioGroup.check(R.id.voip);
                break;
        }
    }
}
