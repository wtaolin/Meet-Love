package com.huawei.esdk.uc.login;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.contacts.MyOtherInfo;
import com.huawei.contacts.MyOtherInfo.OtherLoginType;
import com.huawei.esdk.uc.BaseActivity;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.MainActivity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.TestData;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.conf.data.ConferenceDataHandler;
import com.huawei.esdk.uc.function.LoginFunc;
import com.huawei.esdk.uc.self.SelfInfoUtil;
import com.huawei.espace.framework.common.ThreadManager;
import com.huawei.service.ServiceProxy;
import com.huawei.service.login.LoginErrorResp;

public class LoginActivity extends BaseActivity implements OnClickListener
{

    private static final String TAG = LoginActivity.class.getSimpleName();

    private EditText edUsername;

    private EditText edPassword;

    private Button btnLogin;

    private ImageView ivClearUsername;

    private ImageView ivClearPassword;

    private ImageView ivSetting;

    private ProgressDialog progressDialog;

    private IntentFilter filter;

    private boolean isKickedOut = false;
    
    private Context mContext;
    
    private CheckBox saveAccountCheck;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mContext = this;
        
        isKickedOut = getIntent()
                .getBooleanExtra(IntentData.BEKICKEDOUT, false);
        if (isKickedOut)
        {
            Toast.makeText(this, "该账号已在其他设备上登录", Toast.LENGTH_SHORT).show();
            UCAPIApp.getApp().popAllExcept(this);
        }

        initView();

        filter = new IntentFilter();
        filter.addAction(CustomBroadcastConst.ACTION_CONNECT_TO_SERVER);
        filter.addAction(CustomBroadcastConst.ACTION_LOGIN_ERRORACK);
        registerRec();
    }

    private void initView()
    {
        edUsername = (EditText) findViewById(R.id.username);
        edPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.login);
        ivClearUsername = (ImageView) findViewById(R.id.ivClearUsername);
        ivClearPassword = (ImageView) findViewById(R.id.ivClearPassword);
        ivSetting = (ImageView) findViewById(R.id.setting);
        saveAccountCheck = (CheckBox) findViewById(R.id.sava_account);
        saveAccountCheck.setChecked(true);

        edUsername.addTextChangedListener(new TextWatcher()
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
                if (s.length() > 0)
                {
                    ivClearUsername.setVisibility(View.VISIBLE);
                }
                else
                {
                    ivClearUsername.setVisibility(View.GONE);
                }

            }
        });
        edPassword.addTextChangedListener(new TextWatcher()
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
                if (s.length() > 0)
                {
                    ivClearPassword.setVisibility(View.VISIBLE);
                }
                else
                {
                    ivClearPassword.setVisibility(View.GONE);
                }
            }
        });
        ivClearUsername.setOnClickListener(this);
        ivClearPassword.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        ivSetting.setOnClickListener(this);

        getAccount();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.login:

                login();
                break;

            case R.id.ivClearUsername:

                edUsername.setText("");
                break;

            case R.id.ivClearPassword:

                edPassword.setText("");
                break;

            case R.id.setting:

                Intent netWortSetIntent = new Intent(LoginActivity.this,
                        NetWorkSetActivity.class);
                startActivity(netWortSetIntent);
                break;

            default:
                break;
        }
    }

    private void login()
    {
        showLoginDlg();
        UCAPIApp.getApp().callWhenServiceConnected(new Runnable()
        {
            @Override
            public void run()
            {
                ServiceProxy mService = UCAPIApp.getApp().getService();
                if (null == mService)
                {
                    closeDialog();
                    return;
                }
                // 此时判断sdk已自动登录上，则直接跳转到MainActivity
//                if (mService.isConnected())
//                {
//                    closeDialog();
//                    LoginFunc.getIns().setLogin(true);
//                    SelfInfoUtil.getIns().setToLoginStatus();
//
//                    startActivity(new Intent(LoginActivity.this,
//                            MainActivity.class));
//                    UCAPIApp.getApp().popActivity(LoginActivity.this);
//                    return;
//                }
                
                //这里才是真正的登录
                ThreadManager.getInstance().addToFixedThreadPool(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String userName = edUsername.getText().toString()
                                .trim();
                        String password = edPassword.getText().toString()
                                .trim();
                        LoginFunc.getIns().login(userName, password);
                    }
                });
            }
        });
    }

    /**
     * 等待登录对话框
     */
    private void showLoginDlg()
    {
        if (null == progressDialog)
        {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在登录，请稍后······");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeDialog()
    {
        if (null != progressDialog && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    private void onLoginOtherTerminal(OtherLoginType loginType)
    {
        String desc = null;

        // 是否根据服务器返回错误码给相应提示语
        switch (loginType)
        {
            case PC:
                desc = getString(R.string.login_on_pc_desc);
                break;
            case MOBILE:
                desc = getString(R.string.login_on_mobile_desc);
                break;
            case WEB:
                desc = getString(R.string.login_on_web_desc);
                break;
            case PAD:
                desc = getString(R.string.login_on_pad_desc);
                break;
            case IPPHONE:
                desc = getString(R.string.login_on_ipphone_desc);
                break;
            default:
                break;
        }
        if (desc == null)
        {
            return;
        }
        // 弹框提示已在其他终端登录
        Toast.makeText(LoginActivity.this,
                "login filed | ResponseCode = " + desc, Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * 注册与注销广播
     * */
    private void registerRec()
    {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(loginReceiver, filter);
    }

    private void unRegisterRec()
    {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(loginReceiver);
    }

    private BroadcastReceiver loginReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d(CommonUtil.APPTAG, TAG + " |action = " + action);
            Log.i("action","aciton:-------------"+action);

            if (CustomBroadcastConst.ACTION_CONNECT_TO_SERVER.equals(action))
            {
                boolean respData = intent.getBooleanExtra(
                        UCResource.SERVICE_RESPONSE_DATA, false);
                Log.d(CommonUtil.APPTAG,
                        TAG + " |ACTION_CONNECT_TO_SERVER | response "
                                + String.valueOf(respData));
                closeDialog();
                if (respData)
                {
                	Log.i("respData", "respData:-------------" + respData);
                	//设置为登录状态
                    LoginFunc.getIns().setLogin(true);
                    SelfInfoUtil.getIns().setToLoginStatus();

                    //初始化会议列表
                    ConferenceDataHandler.getIns().initConfList();

                    startActivity(new Intent(LoginActivity.this,
                            MainActivity.class));
                   
                    //将该页面移出栈
                    UCAPIApp.getApp().popActivity(LoginActivity.this);
                    
                    //登陆成功将账号密码保存起来，这里因为是demo，所以未做加密处理,另外加这个功能主要是为了方便调试，每次都要输入账号密码，比较烦
                    boolean isCheck = saveAccountCheck.isChecked();
                    if(isCheck)
                    {
                    	savaAccount();
                    }
                    else
                    {
                    	clearAccount();
                    }
                }
                else
                {
                	
                    LoginFunc.getIns().setLogin(false);
                    SelfInfoUtil.getIns().setToLogoutStatus();
                    UCAPIApp.getApp().stopImService(true);
                    Toast.makeText(LoginActivity.this,
                            "connect to server failed", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            else if (CustomBroadcastConst.ACTION_LOGIN_ERRORACK.equals(action))
            {

                closeDialog();
                int result = intent.getIntExtra(
                        UCResource.SERVICE_RESPONSE_RESULT, 0);
                Log.d(CommonUtil.APPTAG, TAG
                        + " |ACTION_LOGIN_ERRORACK | result = " + result);

                LoginErrorResp errorData = (LoginErrorResp) intent
                        .getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

                if (ResponseCode.FORCEUPDATE == errorData.getStatus())
                {
//                    UCAPIApp.getApp().getService().continueLogin();
                }
                else if (ResponseCode.OTHERLOGIN == errorData.getStatus())
                {
                    OtherLoginType otherLoginType = errorData
                            .getOtherLoginType();
                    if (otherLoginType != MyOtherInfo.OtherLoginType.NULL
                            && otherLoginType != MyOtherInfo.OtherLoginType.MOBILE)
                    {
                        onLoginOtherTerminal(otherLoginType);
                    }
                }
                else
                {
                    // TODO 异常场景 -1等
                    Toast.makeText(
                            LoginActivity.this,
                            "login filed | ResponseCode = "
                                    + errorData.getStatus() + "["
                                    + errorData.getDesc() + "]",
                            Toast.LENGTH_SHORT).show();
                }
                SelfInfoUtil.getIns().setToLogoutStatus();
                UCAPIApp.getApp().stopImService(true);
            }
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unRegisterRec();
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
		
	};
	
	//将账号密码保存起来
	private void savaAccount()
	{
		SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
		
		SharedPreferences.Editor editor = account.edit();
		
        editor.putString("account", edUsername.getText().toString());
        editor.putString("password", edPassword.getText().toString());
        
        editor.commit();
	}
	
	//删除保存的账号密码
	private void clearAccount()
	{
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
		
		SharedPreferences.Editor editor = account.edit();
		
		editor.clear();
		
	}
	
	//从SharedPreferences中获取账号密码
	private void getAccount()
	{
		SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
		
		String userName = account.getString("account", null);
		String passWord = account.getString("password", null);
		
		if((userName==null) || (passWord==null))
		{
			edUsername.setText(TestData.ACCOUNT_MYSELF);
	        edPassword.setText(TestData.ACCOUNT_PSW);
		}
		else
		{
			edUsername.setText(userName);
	        edPassword.setText(passWord);
		}
	}
		

}
