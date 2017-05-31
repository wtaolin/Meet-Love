package com.huawei.esdk.uc.contact;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.contacts.ContactTools;
import com.huawei.contacts.PersonalContact;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.headphoto.ContactHeadFetcher;

/**
 * Created by lWX302895 on 2016/6/12.
 */
public class ContactDetailActivity extends Activity implements View.OnClickListener
{
    private PersonalContact currentContact;

    private ImageView ivHead;

    private TextView tvAccount;

    private TextView tvDepartment;

    private TextView tvNumber;

    private ContactHeadFetcher headFetcher;

    private TextView tvTitle;

    private TextView btnBack;

    private RelativeLayout rlAccount;

    private RelativeLayout rlDepartment;

    private RelativeLayout rlBindNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.contact_detail);
        initData();
        initView();
        showContactDetail();
    }

    private void initData()
    {
        currentContact = (PersonalContact) getIntent().getSerializableExtra("CONTACT_DETAIL");
        if (currentContact == null)
        {
            Toast.makeText(ContactDetailActivity.this, "未获取联系人信息", Toast.LENGTH_SHORT).show();
            UCAPIApp.getApp().popActivity(ContactDetailActivity.this);
        }
    }
    private void initView()
    {
        rlAccount = (RelativeLayout)findViewById(R.id.set_account_layout);
        rlDepartment = (RelativeLayout)findViewById(R.id.set_department_layout);
        rlBindNumber = (RelativeLayout)findViewById(R.id.set_bindnum_layout);

        headFetcher = new ContactHeadFetcher(ContactDetailActivity.this);
        ivHead = (ImageView)findViewById(R.id.round_corner_head_image);

        tvAccount = (TextView)findViewById(R.id.personal_account);
        tvDepartment = (TextView)findViewById(R.id.personal_department);
        tvNumber = (TextView)findViewById(R.id.personal_numbers);
        tvTitle = (TextView)findViewById(R.id.username);
        btnBack = (TextView)findViewById(R.id.btn_back);

        btnBack.setOnClickListener(this);
    }

    private void showContactDetail()
    {
        if (currentContact != null)
        {
            showHead();
            showAccount();
            showDepartment();
            showBindNumber();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_back:
                UCAPIApp.getApp().popActivity(ContactDetailActivity.this);
                break;
            default:
                break;
        }
    }

    private void showHead()
    {
        headFetcher.loadHead(currentContact, ivHead, false);
        tvTitle.setText(ContactTools.getNameByLan(currentContact) + "的个人详情");
    }

    private void showAccount()
    {
        if (!TextUtils.isEmpty(currentContact.getEspaceNumber()))
        {
            tvAccount.setText(currentContact.getEspaceNumber());
            return;
        }
        rlAccount.setVisibility(View.GONE);
    }

    private void showDepartment()
    {
        if (!TextUtils.isEmpty(currentContact.getDepartmentName()))
        {
            tvDepartment.setText(currentContact.getDepartmentName());
            return;
        }
        rlDepartment.setVisibility(View.GONE);
    }

    private void showBindNumber()
    {
        if (!TextUtils.isEmpty(currentContact.getBinderNumber()))
        {
            tvNumber.setText(currentContact.getBinderNumber());
            return;
        }
        rlBindNumber.setVisibility(View.GONE);
    }
}
