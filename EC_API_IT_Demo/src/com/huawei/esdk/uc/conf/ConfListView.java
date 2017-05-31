package com.huawei.esdk.uc.conf;

import java.util.List;

import com.huawei.common.BaseData;
import com.huawei.common.BaseReceiver;
import com.huawei.common.constant.ResponseCodeHandler.ResponseCode;
import com.huawei.common.constant.UCResource;
import com.huawei.conference.entity.CtcEntity;
import com.huawei.conference.entity.GetConfListMsgAck;
import com.huawei.contacts.ContactLogic;
import com.huawei.data.base.BaseResponseData;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.esdk.uc.IntentData;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.conf.data.ConferenceDataHandler;
import com.huawei.esdk.uc.function.ConferenceFunc;
import com.huawei.esdk.uc.function.ConferenceFunc.ConferenceReceiveData;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class ConfListView extends LinearLayout implements BaseReceiver
{

    private ImageView imRefresh;

    private ListView lvConfView;

    private ConfListAdapter confListAdapter;

    private List<ConferenceEntity> confList = null;

    private final String[] sipBroadcast = new String[]
    { ConferenceFunc.UPDATE_CONFLIST_NOTIFY, ConferenceFunc.CONF_END };

    public ConfListView(Context context)
    {
        this(context, null);
    }

    public ConfListView(final Context context, AttributeSet attrs)
    {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(
                R.layout.view_conf_list, null);
        addView(view);

        imRefresh = (ImageView) view.findViewById(R.id.conf_refresh_image);

        lvConfView = (ListView) view.findViewById(R.id.list_conf);
        confListAdapter = new ConfListAdapter(context);
        lvConfView.setAdapter(confListAdapter);

        imRefresh.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ConferenceFunc.getIns().requestConferenceList();
            }
        });

        lvConfView.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3)
            {
                ConferenceEntity entity = (ConferenceEntity) arg0
                        .getItemAtPosition(arg2);
                if (null == entity)
                {
                    return;
                }
                Intent intent = new Intent();
                switch (entity.getState())
                {
                    case ConferenceEntity.STATUS_IN_PROGRESS:
                        if (entity.isFullInfo())
                        {
                            intent.setClass(context,
                                    ConferenceManageActivity.class);
                            intent.putExtra(IntentData.CTCENTITY, entity);
                            getContext().startActivity(intent);
                        }
                         else
                         {
                             intent.setClass(context, ConferenceDetailActivity.class);
                             intent.putExtra(IntentData.CTCENTITY, entity);
                             getContext().startActivity(intent);

                         }
                        break;
                    case ConferenceEntity.STATUS_CREATED:
                        // conferenceLogic.skipActivity(this,
                        // ConferenceManageActivity.class, bundle);
                        break;
                    case ConferenceEntity.STATUS_TO_ATTEND:

                        intent.setClass(context, ConferenceDetailActivity.class);
                        intent.putExtra(IntentData.CTCENTITY, entity);
                        //预约会议显示出与会人员 by lwx302895 start
                        List<ConferenceMemberEntity> bookConfList = ConferenceDataHandler.getIns().getBookMemberList(entity.getConfId());
                        entity.setConfMemberList(bookConfList);
                        //end
                        getContext().startActivity(intent);
                        break;

                    case ConferenceEntity.STATUS_END:
                        // conferenceLogic.skipActivity(this,
                        // ConferenceDetailActivity.class, bundle);
                        intent.setClass(context, ConferenceDetailActivity.class);
                        ConferenceEntity currentConfEntity = ConferenceDataHandler.getIns().getConference(entity.getConfId());
                        if (currentConfEntity != null)
                        {
                            intent.putExtra(IntentData.CTCENTITY, currentConfEntity);
                        }
                        getContext().startActivity(intent);
                        break;
                    default:
                        break;
                }
            }
        });

        lvConfView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3)
            {
                ConferenceEntity entity = (ConferenceEntity) arg0
                        .getItemAtPosition(arg2);

                ConferenceDataHandler.getIns().delConference(entity.getConfId(),true);

                List<ConferenceEntity> localAllConf = ConferenceDataHandler.getIns().getAllConf();
                confListAdapter.notifyChanged(localAllConf);
                return false;
            }
        });
    }

    private void updateConfList(List<ConferenceEntity> conferenceEntities)
    {
        if (confListAdapter != null)
        {
            confListAdapter.notifyChanged(conferenceEntities);
        }
    }

    private void registerSipCallback()
    {
        ConferenceFunc.getIns().registerBroadcast(this, sipBroadcast);
    }

    private void unRegisterSipCallback()
    {
        ConferenceFunc.getIns().unRegisterBroadcast(this, sipBroadcast);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        registerSipCallback();
        
        ConferenceFunc.getIns().requestConfListWithMem();

        
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        unRegisterSipCallback();
    }

    @Override
    public void onReceive(String arg0, BaseData arg1)
    {
        List<ConferenceEntity> localAllConf = ConferenceDataHandler.getIns().getAllConf();
        if (ConferenceFunc.UPDATE_CONFLIST_NOTIFY.equals(arg0))
        {
            ConferenceReceiveData receiveData = (ConferenceReceiveData) arg1;
            BaseResponseData data = receiveData.responseData;
            if (receiveData.result == UCResource.REQUEST_OK)
            {
                if (data.getStatus() == ResponseCode.REQUEST_SUCCESS
                        && data instanceof GetConfListMsgAck)
                {
                    confList = localAllConf;
//                    List<CtcEntity> rspList = ((GetConfListMsgAck) data).getConfList();
//                    List<ConferenceEntity> confList =  ConferenceDataHandler.getIns().updateConfList(rspList);
                    updateConfList(confList);
                }
            }
        }
        else if(ConferenceFunc.CONF_END.equals(arg0))
        {
            ConferenceFunc.getIns().requestConferenceList();
        }
    }

}
