package com.huawei.esdk.uc.login;

import java.util.regex.Pattern;

import com.huawei.contacts.SelfDataHandler;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.utils.StringUtil;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NetWorkSetActivity extends BaseActivity implements
        OnClickListener, OnFocusChangeListener
{

    /**
     * 端口号码EditText允许的最大输入值
     */
    private static final int MAX_INPUT_VALUE_PORT_EDITTEXT = 65535;

    private String ip_addr = SelfDataHandler.getIns().getSelfData()
            .getServerUrl();

    private String port = SelfDataHandler.getIns().getSelfData()
            .getServerPort();

    private String svn_addr = SelfDataHandler.getIns().getSelfData().getSvnIp();

    private String svn_port = SelfDataHandler.getIns().getSelfData()
            .getSvnPort();

    private String svn_account = SelfDataHandler.getIns().getSelfData()
            .getSvnAccount();

    /**annotation section is original code,next line was added  for refresh sdk, by wx303895  start*/
//    private String svn_password = SelfDataHandler.getIns().getSelfData()
//            .getSvnPassword();
    
    private String svn_password = "";
    
    private EditText ipEdit;

    private EditText portEdit;

    private EditText svnGateEdit;

    private EditText svnPortEdit;

    private EditText svnAccountEdit;

    private EditText svnPasswordEdit;

    private LinearLayout svnLayout;

    private ImageView svnCheckBox;

    private ImageView mClearIpImage;

    private ImageView mClearPortImage;

    private ImageView mCleartSvnIp;

    private ImageView mCleartSvnPort;

    private ImageView mCleartSvnAccount;

    private ImageView mCleartSvnPass;

    private static final int MAX_IP_LENGTH = 255;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.net_seting_view);

        initView();

    }

    private void initView()
    {

        TextView ipSetting = (TextView) findViewById(R.id.ip_setting);
        TextView portSetting = (TextView) findViewById(R.id.port_setting);
        TextView svnGate = (TextView) findViewById(R.id.svn_gate);
        TextView svnPort = (TextView) findViewById(R.id.svn_port);
        TextView svnAccount = (TextView) findViewById(R.id.svnaccount);
        TextView svnPassword = (TextView) findViewById(R.id.svnpassword);
        TextView svnCheckText = (TextView) findViewById(R.id.svnchecktext);

        mClearIpImage = (ImageView) findViewById(R.id.ivClear_ip);
        mClearIpImage.setOnClickListener(this);

        mClearPortImage = (ImageView) findViewById(R.id.ivClearPort);
        mClearPortImage.setOnClickListener(this);

        mCleartSvnIp = (ImageView) findViewById(R.id.ivClear_Svngate);
        mCleartSvnIp.setOnClickListener(this);

        mCleartSvnPort = (ImageView) findViewById(R.id.ivClear_svn_port);
        mCleartSvnPort.setOnClickListener(this);

        mCleartSvnAccount = (ImageView) findViewById(R.id.ivClear_svn_account);
        mCleartSvnAccount.setOnClickListener(this);

        mCleartSvnPass = (ImageView) findViewById(R.id.ivClear_svn_pass);
        mCleartSvnPass.setOnClickListener(this);

        ipSetting.setTextColor(getResources().getColor(R.color.text_gray));
        portSetting.setTextColor(getResources().getColor(R.color.text_gray));
        svnGate.setTextColor(getResources().getColor(R.color.text_gray));
        svnPort.setTextColor(getResources().getColor(R.color.text_gray));
        svnAccount.setTextColor(getResources().getColor(R.color.text_gray));
        svnPassword.setTextColor(getResources().getColor(R.color.text_gray));
        svnCheckText.setTextColor(getResources().getColor(R.color.text_gray));

        ipEdit = (EditText) findViewById(R.id.ip_edit);
        ipEdit.addTextChangedListener(new EditTextWatcher(R.id.ip_edit));
        ipEdit.setOnFocusChangeListener(this);

        portEdit = (EditText) findViewById(R.id.port_edit);
        portEdit.addTextChangedListener(new EditTextWatcher(R.id.port_edit));
        portEdit.setOnFocusChangeListener(this);

        svnGateEdit = (EditText) findViewById(R.id.svn_gate_edit);
        svnGateEdit.addTextChangedListener(new EditTextWatcher(
                R.id.svn_gate_edit));
        svnGateEdit.setOnFocusChangeListener(this);

        svnPortEdit = (EditText) findViewById(R.id.svn_port_edit);
        svnPortEdit.addTextChangedListener(new EditTextWatcher(
                R.id.svn_port_edit));
        svnPortEdit.setOnFocusChangeListener(this);

        svnAccountEdit = (EditText) findViewById(R.id.account_edit);
        svnAccountEdit.addTextChangedListener(new EditTextWatcher(
                R.id.account_edit));
        svnAccountEdit.setOnFocusChangeListener(this);

        svnPasswordEdit = (EditText) findViewById(R.id.pass_edit);
        svnPasswordEdit.setTransformationMethod(PasswordTransformationMethod
                .getInstance());
        svnPasswordEdit.addTextChangedListener(new EditTextWatcher(
                R.id.pass_edit));
        svnPasswordEdit.setOnFocusChangeListener(this);

        processEditTextWithNumber(svnGateEdit, svn_addr, MAX_IP_LENGTH);
        processEditTextWithNumber(ipEdit, ip_addr, MAX_IP_LENGTH);
        portEdit.setText(port);
        svnPortEdit.setText(svn_port);
        svnAccountEdit.setText(svn_account);
        svnPasswordEdit.setText(svn_password);

        svnLayout = (LinearLayout) findViewById(R.id.svnlayout);

        svnAccountEdit.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (null != s && null != svnAccountEdit)
                {
                    if (svn_account.equalsIgnoreCase(s.toString()))
                    {
                        svnPasswordEdit.setText(svn_password);
                    }
                    else
                    {
                        svnPasswordEdit.setText("");
                    }
                }
            }
        });

        boolean svnFlag = SelfDataHandler.getIns().getSelfData().isSvnFlag();
        svnCheckBox = (ImageView) findViewById(R.id.svn_check_box);
        svnCheckBox
                .setBackgroundResource(svnFlag ? R.drawable.btn_square_selected
                        : R.drawable.btn_square_unselected);
        if (svnFlag)
        {
            svnCheckBox.setTag(true);
            svnLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            svnCheckBox.setTag(false);
            svnLayout.setVisibility(View.GONE);
        }
        LinearLayout svnCheckLayout = (LinearLayout) findViewById(R.id.svnchecklayout);
        svnCheckLayout.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                boolean ischeck = ((Boolean) svnCheckBox.getTag())
                        .booleanValue();
                if (ischeck)
                {
                    svnLayout.setVisibility(View.GONE);
                    svnCheckBox.setTag(false);
                    svnCheckBox
                            .setBackgroundResource(R.drawable.btn_square_unselected);
                }
                else
                {
                    svnLayout.setVisibility(View.VISIBLE);
                    svnCheckBox.setTag(true);
                    svnCheckBox
                            .setBackgroundResource(R.drawable.btn_square_selected);
                }

            }
        });

        Button done_bt = (Button) findViewById(R.id.done);
        done_bt.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toSave();
            }
        });
    }

    private void toSave()
    {
        String ip = ipEdit.getText().toString().replaceAll(" ", "");
        String port = portEdit.getText().toString().replaceAll(" ", "");
        String svnIp = svnGateEdit.getText().toString().replaceAll(" ", "");
        String svnPort = svnPortEdit.getText().toString().replaceAll(" ", "");
        String svnAcc = svnAccountEdit.getText().toString().replaceAll(" ", "");
        String svnPass = svnPasswordEdit.getText().toString()
                .replaceAll(" ", "");

        if (saveInfoToXml(ip, port, svnIp, svnPort, svnAcc, svnPass))
        {
            UCAPIApp.getApp().popActivity(this);
        }
    }

    private boolean saveInfoToXml(String ip, String port, String svnIp,
            String svnPort, String svnAcc, String svnPass)
    {
        boolean ischecked = ((Boolean) svnCheckBox.getTag()).booleanValue();
        int resId = -1;
        if (TextUtils.isEmpty(ip))
        {
            resId = R.string.serverblank;
        }
        else if (TextUtils.isEmpty(port))
        {
            resId = R.string.portblank;
        }
        else if (!isIPAddress(ip) && !isDomainName(ip))
        {
            resId = R.string.net_address_format_error;
        }
        else if (isIllegalPort(port))
        {
            resId = R.string.net_port_error;
        }
        else if (ischecked)
        {
            if (TextUtils.isEmpty(svnIp))
            {
                resId = R.string.svnipblank;
            }
            else if (TextUtils.isEmpty(svnPort))
            {
                resId = R.string.svnportblank;
            }
            else if (TextUtils.isEmpty(svnAcc))
            {
                resId = R.string.svn_account_empty_prompt;
            }
            else if (TextUtils.isEmpty(svnPass))
            {
                resId = R.string.svn_pwd_empty_prompt;
            }
            else if (!isIPAddress(svnIp) && !isDomainName(svnIp))
            {
                resId = R.string.svn_address_format_error;
            }
            else if (isIllegalPort(svnPort))
            {
                resId = R.string.svn_port_error;
            }
        }
        if (-1 != resId)
        {
            Toast.makeText(getBaseContext(), getString(resId),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // 保存操作
        SelfDataHandler.getIns().getSelfData().setServerUrl(ip);
        SelfDataHandler.getIns().getSelfData().setServerPort(port);
        SelfDataHandler.getIns().getSelfData().setSvnFlag(ischecked);
        SelfDataHandler.getIns().getSelfData().setSvnAccount(svnAcc);
//        SelfDataHandler.getIns().getSelfData().setSvnPassword(svnPass);

        if (ischecked)
        {
            SelfDataHandler.getIns().getSelfData().setSvnIp(svnIp);
            SelfDataHandler.getIns().getSelfData().setSvnPort(svnPort);
        }
        SelfDataHandler.getIns().getSelfData()
                .setVersion(getString(R.string.androidversion));
        return true;
    }

    /**
     * 设置EditText控件 默认只允许输入数字类型，默认光标置于文本末尾, 默认限制输入长度为20个字符
     * @param length
     * @param editText
     */
    private void processEditTextWithNumber(EditText editText, String initText,
            int length)
    {
        if (null == editText)
        {
            return;
        }
        editText.setFilters(new InputFilter[]
        { new InputFilter.LengthFilter(length) });

        editText.setText(initText);
        Selection.setSelection(editText.getText(), editText.getText().length());
    }

    private final class EditTextWatcher implements TextWatcher
    {
        private int key;

        private EditTextWatcher(int id)
        {
            this.key = id;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count)
        {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after)
        {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            switch (key)
            {
                case R.id.ip_edit:
                    handleTextChange(s, ipEdit, mClearIpImage);
                    break;
                case R.id.port_edit:
                    handleTextChange(s, portEdit, mClearPortImage);
                    break;
                case R.id.svn_gate_edit:
                    handleTextChange(s, svnGateEdit, mCleartSvnIp);
                    break;
                case R.id.svn_port_edit:
                    handleTextChange(s, svnPortEdit, mCleartSvnPort);
                    break;
                case R.id.account_edit:
                    handleTextChange(s, svnAccountEdit, mCleartSvnAccount);
                    if (svn_account.equalsIgnoreCase(s.toString()))
                    {
                        svnPasswordEdit.setText(svn_password);
                    }
                    else
                    {
                        svnPasswordEdit.setText("");
                    }
                    break;
                case R.id.pass_edit:
                    handleTextChange(s, svnPasswordEdit, mCleartSvnPass);
                    break;
                default:
                    break;
            }
        }

        private void handleTextChange(Editable s, EditText et, ImageView iv)
        {
            if ((null != s) && (null != s.toString())
                    && (null != mClearIpImage))
            {
                if ("".equals(et.getText().toString().trim()) || !et.hasFocus())
                {
                    iv.setVisibility(View.GONE);
                }
                else
                {
                    iv.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        int key = v.getId();
        switch (key)
        {
            case R.id.ivClear_ip:
                ipEdit.setText("");
                break;

            case R.id.ivClearPort:
                portEdit.setText("");
                break;

            case R.id.ivClear_Svngate:
                svnGateEdit.setText("");
                break;

            case R.id.ivClear_svn_port:
                svnPortEdit.setText("");
                break;

            case R.id.ivClear_svn_account:
                svnAccountEdit.setText("");
                svn_account = "";
                svn_password = "";
                svnPasswordEdit.setText("");
                break;

            case R.id.ivClear_svn_pass:
                svnPasswordEdit.setText("");
                svn_password = "";
                break;

            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus)
    {
        EditText et = null;
        if (v instanceof EditText)
        {
            et = (EditText) v;
            et.setSelection(et.getText().toString().length());
        }

        if (null == et)
        {
            return;
        }

        if (hasFocus && !TextUtils.isEmpty(et.getText().toString()))
        {
            handleHasFocus(et);
        }
        else
        {
            handleLostFocus(et);
        }
    }

    private void handleHasFocus(EditText et)
    {
        if (null == et)
        {
            return;
        }
        switch (et.getId())
        {
            case R.id.ip_edit:
                mClearIpImage.setVisibility(View.VISIBLE);
                break;
            case R.id.port_edit:
                mClearPortImage.setVisibility(View.VISIBLE);
                break;
            case R.id.svn_gate_edit:
                mCleartSvnIp.setVisibility(View.VISIBLE);
                break;
            case R.id.svn_port_edit:
                mCleartSvnPort.setVisibility(View.VISIBLE);
                break;
            case R.id.account_edit:
                mCleartSvnAccount.setVisibility(View.VISIBLE);
                break;
            case R.id.pass_edit:
                mCleartSvnPass.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void handleLostFocus(EditText et)
    {
        if (null == et)
        {
            return;
        }
        switch (et.getId())
        {
            case R.id.ip_edit:
                mClearIpImage.setVisibility(View.GONE);
                break;
            case R.id.port_edit:
                mClearPortImage.setVisibility(View.GONE);
                break;
            case R.id.svn_gate_edit:
                mCleartSvnIp.setVisibility(View.GONE);
                break;
            case R.id.svn_port_edit:
                mCleartSvnPort.setVisibility(View.GONE);
                break;
            case R.id.account_edit:
                mCleartSvnAccount.setVisibility(View.GONE);
                break;
            case R.id.pass_edit:
                mCleartSvnPass.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    /**
     * 
     * 判断端口的输入是否为 0 ~ 65535
     * @param s
     */
    private boolean isIllegalPort(String s)
    {
        if (null == s)
        {
            return true;
        }
        int value = Integer.parseInt(s);
        if (s.charAt(0) == '0' || value > MAX_INPUT_VALUE_PORT_EDITTEXT)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 判断是否是IP地址
     * @param s
     * @return
     */
    private boolean isIPAddress(String s)
    {
        Pattern p = Pattern
                .compile("^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}"
                        + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$");
        if (p.matcher(s).matches())
        {
            return true;
        }
        return false;
    }

    /**
     * 判断是否是域名
     * @param s
     * @return
     */
    private boolean isDomainName(String s)
    {
        if (TextUtils.isEmpty(s) || s.matches("[0-9//.]+"))
        {
            return false;
        }

        Pattern p = Pattern
                .compile("[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+\\.?");
        if (p.matcher(s).matches())
        {
            return true;
        }
        return false;
    }

	@Override
	public void initializeData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initializeComposition() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearData() {
		// TODO Auto-generated method stub
		
	}

}
