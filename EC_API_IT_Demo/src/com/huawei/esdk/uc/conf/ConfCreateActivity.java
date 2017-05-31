package com.huawei.esdk.uc.conf;

import java.util.ArrayList;
import java.util.Collection;

import com.huawei.common.CommonVariables;
import com.huawei.contacts.ContactLogic;
import com.huawei.contacts.PersonalContact;
import com.huawei.contacts.SelfDataHandler;
import com.huawei.data.ExecuteResult;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.data.entity.People;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ContactFunc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ConfCreateActivity extends Activity implements OnClickListener
{

    private Button btnCreate;

    private EditText edSubject;

    private ImageButton ibClear;

    private TextView tvConfNum;

    // private TextView tvTime;

    // private RelativeLayout rlVoiceType;
    // private RelativeLayout rlMultiType;
    // private ImageView ivVoice;
    // private ImageView ivMulti;

    private LinearLayout llMember;

    private ImageView ivAdd;

    private String emcee;

    private String subject;

    private int mediaType = 1;

    private String myEspaceAccount;

    private int beginTime;

    private int endTime;

    private int confType = 1;

    private boolean sendMail = true;

    private ArrayList<ConferenceMemberEntity> members = new ArrayList<ConferenceMemberEntity>();

    // private Dialog confTimeDialog;

    private boolean isNow = true;

    private boolean isMulti = false;

    private ProgressDialog progressDialog;

    private Handler mHandler;

    private static final int FIRST_CREATECONF_NEED_TIME = 4000;

    private static final int FIRST_CREATECONF_TAG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_conf_create);
        initView();
        initData();
        initHandler();
    }

    private void initView()
    {
        btnCreate = (Button) findViewById(R.id.conference_create_btn_send);

        edSubject = (EditText) findViewById(R.id.subject);
        ibClear = (ImageButton) findViewById(R.id.meeting_clear_subject);

        tvConfNum = (TextView) findViewById(R.id.conference_members_number);

        // tvTime = (TextView) findViewById(R.id.conf_create_time);

        // rlVoiceType = (RelativeLayout) findViewById(R.id.voice_conf_type);
        // rlMultiType = (RelativeLayout) findViewById(R.id.multi_conf_type);
        // ivVoice = (ImageView) findViewById(R.id.voice_conf_select);
        // ivMulti = (ImageView) findViewById(R.id.multi_conf_select);

        llMember = (LinearLayout) findViewById(R.id.auto_location);
        ivAdd = (ImageView) findViewById(R.id.add);

        btnCreate.setOnClickListener(this);
        edSubject.addTextChangedListener(watcher);
        ibClear.setOnClickListener(this);
        // tvTime.setOnClickListener(this);
        // rlVoiceType.setOnClickListener(this);
        // rlMultiType.setOnClickListener(this);
        ivAdd.setOnClickListener(this);
    }

    private void initData()
    {
        myEspaceAccount = CommonVariables.getIns().getUserAccount();
        edSubject.setText(myEspaceAccount + "的会议");

        emcee = SelfDataHandler.getIns().getSelfData().getCallbackNmb();
        tvConfNum.setText(emcee);

        // updateMeidaType();

        members.add(getHost());
        addMemberView();

    }

    private void initHandler()
    {
        mHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case FIRST_CREATECONF_TAG:
                        closeDialog();
                        createConference();
                        break;

                    default:
                        break;
                }
            }
        };
    }

    // private void updateMeidaType()
    // {
    // if (isMulti)
    // {
    // ivVoice.setVisibility(View.GONE);
    // ivMulti.setVisibility(View.VISIBLE);
    // }
    // else
    // {
    // ivVoice.setVisibility(View.VISIBLE);
    // ivMulti.setVisibility(View.GONE);
    // }
    // }

    private void addMemberView()
    {
        for (int i = 0; i < llMember.getChildCount() - 1; i++)
        {
            llMember.removeViewAt(i);
        }
        for (ConferenceMemberEntity member : members)
        {
            addMemberView(member);
        }
    }

    private void addMemberView(final ConferenceMemberEntity member)
    {
        final View child = getLayoutInflater().inflate(
                R.layout.create_member_item, null);
        child.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        TextView name = (TextView) child.findViewById(R.id.name);
        name.setText(member.getAccount());
        ImageView operate = (ImageView) child.findViewById(R.id.operate);
        if (member.getRole() == 1)
        {
            operate.setVisibility(View.INVISIBLE);
        }
        operate.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (member.getRole() != 1)
                {
                    llMember.removeView(child);
                    members.remove(member);
                }
            }
        });
        llMember.addView(child, llMember.getChildCount() - 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            members.clear();
            members.add(getHost());
            members.addAll((Collection<? extends ConferenceMemberEntity>) data
                    .getSerializableExtra("member"));

            addMemberView();
        }

    }

    public ConferenceMemberEntity getHost()
    {
        String account = CommonVariables.getIns().getUserAccount();

        PersonalContact pContact = ContactLogic.getIns().getMyContact();
        String number = SelfDataHandler.getIns().getSelfData()
                .getCallbackNmb();
        String name = ContactFunc.getIns().getDisplayName(pContact);

        People p = new People(account, null, null);

        ConferenceMemberEntity memberEntity = new ConferenceMemberEntity(p,
                name, number);

        memberEntity.setAccount(account);
        memberEntity.setRole(ConferenceMemberEntity.ROLE_PRESIDER);
        memberEntity.setEmail(pContact != null ? pContact.getEmail() : null);
        memberEntity.setStatus(ConferenceMemberEntity.STATUS_LEAVE_CONF);

        return memberEntity;

    }

    private TextWatcher watcher = new TextWatcher()
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
                ibClear.setVisibility(View.VISIBLE);
            }
            else
            {
                ibClear.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            //第一次创建会议如果立即创建可能会出错
            case R.id.conference_create_btn_send:

                if (ConferenceFunc.getIns().CREATE_CONF_NUM == 0)
                {
                    showWaitDlgAndCreateConf();
                }
                else
                {
                    createConference();
                }
                ConferenceFunc.getIns().CREATE_CONF_NUM++;

                break;
            case R.id.meeting_clear_subject:

                edSubject.setText("");
                break;
            // case R.id.conf_create_time:
            //
            // showBookConfTimeDailog();
            // break;
            // case R.id.voice_conf_type:
            // case R.id.multi_conf_type:
            //
            // isMulti = !isMulti;
            // updateMeidaType();
            // break;
            case R.id.add:

                Intent intent = new Intent(ConfCreateActivity.this,
                        ConferenceAddMemberActivity.class);
                startActivityForResult(intent, 0);
                break;

            default:
                break;
        }
    }


    /**
     * 等待创建会议对话框
     */
    private void showWaitDlgAndCreateConf()
    {
        if (null == progressDialog)
        {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("初始化中，请稍后······");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

        //四秒后关闭对话框，并创建会议
        Message msg = new Message();
        msg.what = FIRST_CREATECONF_TAG;
        mHandler.sendMessageDelayed(msg,FIRST_CREATECONF_NEED_TIME);
    }

    private void closeDialog()
    {
        if (null != progressDialog && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    private void createConference()
    {
        subject = edSubject.getText().toString().trim();
        mediaType = (!isMulti) ? 1 : 2;
        confType = isNow ? 1 : 2;
        if (isNow)
        {
            beginTime = 0;
            endTime = 0;
            ExecuteResult result = ConferenceFunc.getIns()
                    .requestCreateConference(emcee, subject, mediaType,
                            myEspaceAccount, beginTime, endTime,
                            confType, sendMail, members);

            if (result.isResult())
            {
                Intent intent = new Intent(ConfCreateActivity.this,
                        ConferenceManageActivity.class);
                intent.putExtra("result_id", result.getId());
                intent.putExtra("subject", subject);
                intent.putExtra("members", members);
                startActivity(intent);
                finish();
            }
            Log.d("ConferenceFunc", "emcee ="+emcee+",members = "+members.get(0).toString());
        }
        else
        {
            ExecuteResult result = ConferenceFunc.getIns()
                    .requestCreateConference(emcee, subject, mediaType,
                            myEspaceAccount, beginTime, endTime,
                            confType, sendMail, members);
            if (result.isResult())
            {
                finish();
            }
        }
    }
    // private Calendar currentCalendar;
    //
    // private String[] date;
    //
    // private static final String[] MINUTE =
    // { "00", "10", "20", "30", "40", "50" };
    //
    // private int timeLong;
    //
    // @SuppressLint("NewApi")
    // private void showBookConfTimeDailog()
    // {
    // if (confTimeDialog == null)
    // {
    // confTimeDialog = new Dialog(this);
    // confTimeDialog.setContentView(R.layout.conf_create_date);
    // confTimeDialog.setTitle(R.string.conference_time);
    // }
    // initDate();
    //
    // final LinearLayout llTime = (LinearLayout) confTimeDialog
    // .findViewById(R.id.pickers);
    //
    // CheckBox cbIsNow = (CheckBox) confTimeDialog.findViewById(R.id.isNow);
    // cbIsNow.setOnCheckedChangeListener(new OnCheckedChangeListener()
    // {
    //
    // @Override
    // public void onCheckedChanged(CompoundButton buttonView,
    // boolean isChecked)
    // {
    // if (isChecked)
    // {
    // llTime.setVisibility(View.GONE);
    // }
    // else
    // {
    // llTime.setVisibility(View.VISIBLE);
    // }
    // }
    // });
    // cbIsNow.setChecked(isNow);
    //
    // final NumberPicker datePicker = (NumberPicker) confTimeDialog
    // .findViewById(R.id.dayofyear);
    // datePicker.setMinValue(0);
    // datePicker.setMaxValue(date.length - 1);
    // datePicker.setDisplayedValues(date);
    // datePicker.setWrapSelectorWheel(false);
    // datePicker.setValue(0);
    //
    // final NumberPicker hourPicker = (NumberPicker) confTimeDialog
    // .findViewById(R.id.hourofday);
    // hourPicker.setMinValue(0);
    // hourPicker.setMaxValue(23);
    // hourPicker.setValue(currentCalendar.get(Calendar.HOUR_OF_DAY));
    //
    // final NumberPicker minutePicker = (NumberPicker) confTimeDialog
    // .findViewById(R.id.minuteofhour);
    // minutePicker.setMinValue(0);
    // minutePicker.setMaxValue(MINUTE.length - 1);
    // minutePicker.setDisplayedValues(MINUTE);
    // minutePicker.setWrapSelectorWheel(true);
    // minutePicker.setValue(currentCalendar.get(Calendar.MINUTE) / 10);
    //
    // final NumberPicker lengthHourPicker = (NumberPicker) confTimeDialog
    // .findViewById(R.id.lenghofhour);
    // lengthHourPicker.setMinValue(0);
    // lengthHourPicker.setMaxValue(23);
    //
    // final NumberPicker lengthMinutePicker = (NumberPicker) confTimeDialog
    // .findViewById(R.id.lenghofminute);
    // lengthMinutePicker.setMinValue(0);
    // lengthMinutePicker.setMaxValue(MINUTE.length - 1);
    // lengthMinutePicker.setDisplayedValues(MINUTE);
    // lengthMinutePicker.setWrapSelectorWheel(true);
    // lengthMinutePicker.setValue(3);
    //
    // OnValueChangeListener listener = new OnValueChangeListener()
    // {
    //
    // @Override
    // public void onValueChange(NumberPicker picker, int oldVal,
    // int newVal)
    // {
    // Calendar calendar = Calendar.getInstance(Locale.getDefault());
    // int hour = calendar.get(Calendar.HOUR_OF_DAY);
    // if (picker == hourPicker)
    // {
    // int minValue = hourPicker.getMinValue();
    // int maxValue = hourPicker.getMaxValue();
    // if (oldVal == maxValue && newVal == minValue)
    // {
    // datePicker.setValue(datePicker.getValue() + 1);
    // }
    // else if (oldVal == minValue && newVal == maxValue)
    // {
    // datePicker.setValue(datePicker.getValue() - 1);
    // }
    // }
    // else if (picker == minutePicker)
    // {
    // int minValue = minutePicker.getMinValue();
    // int maxValue = minutePicker.getMaxValue();
    // if (oldVal == maxValue && newVal == minValue)
    // {
    // hourPicker.setValue(hourPicker.getValue() + 1);
    // }
    // else if (oldVal == minValue && newVal == maxValue)
    // {
    // if (hourPicker.getValue() > hour)
    // {
    // hourPicker.setValue(hourPicker.getValue() - 1);
    // }
    // else
    // {
    // hourPicker.setValue(hour);
    // }
    // }
    // }
    // else if (picker == lengthMinutePicker)
    // {
    // int minValue = lengthMinutePicker.getMinValue();
    // int maxValue = lengthMinutePicker.getMaxValue();
    // if (oldVal == maxValue && newVal == minValue)
    // {
    // lengthHourPicker
    // .setValue(lengthHourPicker.getValue() + 1);
    // }
    // else if (oldVal == minValue && newVal == maxValue)
    // {
    // lengthHourPicker
    // .setValue(lengthHourPicker.getValue() - 1);
    // }
    // }
    // }
    // };
    //
    // datePicker.setOnValueChangedListener(listener);
    // hourPicker.setOnValueChangedListener(listener);
    // minutePicker.setOnValueChangedListener(listener);
    // lengthHourPicker.setOnValueChangedListener(listener);
    // lengthMinutePicker.setOnValueChangedListener(listener);
    //
    // Button btnCancel = (Button) confTimeDialog.findViewById(R.id.cancel);
    // Button btnOK = (Button) confTimeDialog.findViewById(R.id.ok);
    // btnCancel.setOnClickListener(new OnClickListener()
    // {
    //
    // @Override
    // public void onClick(View v)
    // {
    // // TODO Auto-generated method stub
    // confTimeDialog.dismiss();
    // }
    // });
    // btnOK.setOnClickListener(new OnClickListener()
    // {
    //
    // @Override
    // public void onClick(View v)
    // {
    // // TODO Auto-generated method stub
    // if (llTime.getVisibility() == View.GONE)
    // {
    // isNow = true;
    // }
    // else
    // {
    // isNow = false;
    // }
    // getTime(datePicker.getValue(), hourPicker.getValue(),
    // minutePicker.getValue(), lengthHourPicker.getValue(),
    // lengthMinutePicker.getValue());
    // confTimeDialog.dismiss();
    // }
    // });
    //
    // confTimeDialog.show();
    // }
    //
    // private void getTime(int day, int hour, int minute, int lengthHour,
    // int lengthMinute)
    // {
    //
    // if (isNow)
    // {
    // tvTime.setText(R.string.conf_create_init_time);
    // }
    // else
    // {
    // Calendar calendar = Calendar.getInstance(Locale.getDefault());
    // calendar.add(Calendar.DAY_OF_YEAR, day);
    // calendar.set(Calendar.HOUR_OF_DAY, hour);
    // calendar.set(Calendar.MINUTE, minute * 10);
    //
    // String timeStr = new SimpleDateFormat("yyyy/MM/dd HH:mm")
    // .format(calendar.getTime());
    // tvTime.setText(timeStr);
    //
    // beginTime = (int) (calendar.getTimeInMillis() / 1000);
    //
    // calendar.set(Calendar.HOUR_OF_DAY, hour + lengthHour);
    // calendar.set(Calendar.MINUTE, (minute + lengthMinute) * 10);
    //
    // timeStr = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(calendar
    // .getTime());
    // tvTime.append(" - " + timeStr);
    //
    // timeLong = (lengthHour * 60 + lengthMinute * 10) * 60;
    // endTime = beginTime + timeLong;
    // }
    // }
    //
    // private void initDate()
    // {
    // Locale.setDefault(Locale.ENGLISH);
    // Locale locale = Locale.getDefault();
    // currentCalendar = Calendar.getInstance(locale);
    // int year = currentCalendar.getActualMaximum(Calendar.DAY_OF_YEAR);
    // date = new String[year];
    //
    // // DateFormat format = null;
    // // if ("zh".equals(locale.getLanguage()))
    // // {
    // // format = DateFormat.getDateInstance(DateFormat.FULL, locale);
    // // date[0] = format.format(currentCalendar.getTime());
    // // for (int i = 1; i < year; i++)
    // // {
    // // currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
    // // date[i] = format.format(currentCalendar.getTime());
    // // }
    // // }
    // // else
    // // {
    // // Locale.setDefault(Locale.ENGLISH);
    // String partten = "yyyy/MM/dd";
    //
    // date[0] = new SimpleDateFormat(partten).format(new Date(currentCalendar
    // .getTimeInMillis()));
    // for (int i = 1; i < year; i++)
    // {
    // currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
    // date[i] = new SimpleDateFormat(partten).format(new Date(
    // currentCalendar.getTimeInMillis()));
    // }
    // // }
    // }

}
