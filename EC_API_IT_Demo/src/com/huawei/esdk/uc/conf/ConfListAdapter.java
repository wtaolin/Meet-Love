package com.huawei.esdk.uc.conf;

import java.util.ArrayList;
import java.util.List;

import com.huawei.common.res.LocContext;
import com.huawei.contacts.ContactCache;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.function.ContactFunc;
import com.huawei.utils.DateUtil;
import com.huawei.utils.StringUtil;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ConfListAdapter extends BaseAdapter
{

    private static final String TAG = ConfListAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    private List<ConferenceEntity> conferenceEntities = new ArrayList<ConferenceEntity>();

    private static int[][] confIconResources =
    {
            { R.drawable.conf_inprocess_tag, R.drawable.conf_inprocess_tag,
                    R.drawable.conf_inprocess_tag,
                    R.drawable.conf_inprocess_tag_gray },
            { R.drawable.conf_to_attend_tag, R.drawable.conf_to_attend_tag,
                    R.drawable.conf_to_attend_inprocess_tag,
                    R.drawable.conf_to_attend_tag_gray } };

    private static int[] confItemResouces =
    { R.drawable.bg_item_selector, R.drawable.bg_item_selector,
            R.drawable.conf_begining_select, R.drawable.bg_item_selector };

    private static int[] confTxtColorRes =
    { R.color.item_member, R.color.item_member, R.color.white,
            R.color.chat_item_tag };

    public ConfListAdapter(Context context)
    {
        mInflater = LayoutInflater.from(context);

    }

    public void notifyChanged(List<ConferenceEntity> conferenceEntities)
    {
        this.conferenceEntities.clear();
        if(conferenceEntities != null)
        {
            this.conferenceEntities = conferenceEntities;
        }
//        conferenceEntities = ConferenceDataHandler.getIns().getAllConf();
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return conferenceEntities.size();
    }

    @Override
    public Object getItem(int position)
    {
        return conferenceEntities.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder = null;
        if (convertView != null)
        {
            holder = (ViewHolder) convertView.getTag();
        }
        if (holder == null)
        {
            holder = new ViewHolder();

            convertView = mInflater.inflate(R.layout.conf_list_item, null);
            holder.confItem = (RelativeLayout) convertView
                    .findViewById(R.id.conf_item);
            holder.confIcon = (ImageView) convertView
                    .findViewById(R.id.conf_icon);
            holder.confSubject = (TextView) convertView
                    .findViewById(R.id.conf_name);
            holder.confTime = (TextView) convertView
                    .findViewById(R.id.conf_time);
            holder.confEmcee = (TextView) convertView
                    .findViewById(R.id.conf_emcee);
            holder.meetingBack = (ImageView) convertView
                    .findViewById(R.id.meeting_back);

            convertView.setTag(holder);
        }

        final ConferenceEntity conferenceEntity = conferenceEntities
                .get(position);

        holder.confSubject.setText(conferenceEntity.getSubject());
        holder.confTime.setText(DateUtil.getStringDate(
                conferenceEntity.getBeginTime(), true));
        holder.confEmcee.setText(getEmcee(conferenceEntity));

        int status = conferenceEntity.getState();
        holder.confItem.setBackgroundResource(confItemResouces[status]);
        holder.confIcon.setImageResource(confIconResources[conferenceEntity.getType() - 1][status]);

        int colorIndex = confTxtColorRes[status];
        setColor(colorIndex, colorIndex, holder);

        holder.meetingBack.setVisibility(View.GONE);
        if (status == ConferenceEntity.STATUS_CREATED)
        {
            holder.confTime.setText(LocContext.getContext().getResources()
                    .getString(R.string.conf_create_prompt));
            holder.confEmcee.setText("");
        }

        if (status == ConferenceEntity.STATUS_IN_PROGRESS)
        {
            holder.meetingBack.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    /**
     * 主持人字符串前加上‘主持人：’
     * 
     * @param entity
     * @return
     */
    public String getEmcee(ConferenceEntity entity)
    {
        String emcee = "";
        emcee = ContactFunc.getIns().getDisplayName(
                ContactCache.getIns().getContactByAccount(
                        entity.getHostAccount()));
        if (TextUtils.isEmpty(emcee))
        {
            List<ConferenceMemberEntity> list = entity.getConfMemberList();
            if (!list.isEmpty())
            {
                emcee = list.get(0).getDisplayName();
            }
            else
            {
                emcee = entity.getHostAccount();
            }
        }
        return null == emcee ? "" : LocContext.getContext()
                .getString(R.string.conf_main_item_emcee_prefix).concat(emcee);
    }

    /**
     * 设置item中各项显示颜色
     */
    private void setColor(int subjectColor, int otherColor, ViewHolder vh)
    {
        vh.confTime.setTextColor(LocContext.getContext().getResources()
                .getColor(otherColor));
        vh.confEmcee.setTextColor(LocContext.getContext().getResources()
                .getColor(otherColor));
        vh.confSubject.setTextColor(LocContext.getContext().getResources()
                .getColor(subjectColor));
    }

    class ViewHolder
    {
        /**
         * 整个item区域
         */
        private RelativeLayout confItem;

        /**
         * 会议状态图标
         */
        private ImageView confIcon;

        /**
         * 会议名称
         */
        private TextView confSubject;

        /**
         * 会议时间
         */
        private TextView confTime;

        /**
         * 主持人
         */
        private TextView confEmcee;

        public ImageView meetingBack;
    }

}
