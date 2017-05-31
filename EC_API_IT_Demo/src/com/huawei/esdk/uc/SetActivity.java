package com.huawei.esdk.uc;

import com.huawei.contacts.SelfDataHandler;
import com.huawei.esdk.uc.function.VoipFunc;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class SetActivity extends Activity
{

    private RadioGroup radioGroup;

    private EditText edCallBackNum;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_set);

        initView();

    }

    private void initView()
    {
        Button done_bt = (Button) findViewById(R.id.done);
        done_bt.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toSave();
                finish();
            }
        });

        radioGroup = (RadioGroup) findViewById(R.id.group_type);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
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

        edCallBackNum = (EditText) findViewById(R.id.callbackNum);
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
