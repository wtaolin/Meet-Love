package com.huawei.esdk.uc.conf;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.conf.data.AddMemberEntity;
import com.huawei.esdk.uc.conf.data.MemberContant;
import com.huawei.esdk.uc.utils.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lWX303895 on 2016/4/26.
 */
public class DisplayMemberWidget
{
    /**
     * ��¼��ǰ�������Ŀؼ����ܿ��
     */
    private int width = 0;
    /**
     * ��¼��ǰ�������Ŀؼ����ܸ߶�
     */
    private int top = 0;
    /**
     * ���ÿؼ���������
     */
    private Context context;
    /**
     * ��¼�ÿؼ�������list
     */
    private List<AddMemberEntity> selectMembers = new ArrayList<AddMemberEntity>();

    /**
     * ����֪ͨ��������Ӧ����
     */
    private Handler mHandler = new Handler();

    /**
     * ��¼��ǰ�ؼ�������
     */
    private int lines = 1;
    /**
     * layout���ܳ���
     */
    private int length;
    /**
     * ���ؼ���layout
     */
    private RelativeLayout layout;

    /**
     * layout�ؼ���û������ʱ�����ʾview.
     */
    private TextView layoutHint = null;

    private int fromTransfer;

    private int imageViewWidth = 0;

    private boolean more;

    private boolean isBook = false;

    /**
     * �ؼ���ɾ�����ֵļ����ص�
     */
    private ItemRemoveListener listener;

    private DisplayMemberWidgetLogic widgetLogic;
    // private boolean isVisibility;

    public DisplayMemberWidget()
    {
    }

    /**
     * ���췽��
     * ���������顱����ʹ�õĹ��췽��
     *
     * @param layout        ��Ҫ���ؼ���layout
     * @param context       ��Ҫ���ؼ���������
     * @param selectMembers ��Ҫ���ؼ�������list
     */
    public DisplayMemberWidget(RelativeLayout layout, Context context,
                               List<AddMemberEntity> selectMembers, int fromTransfer, int length)
    {
        initViewLayout(layout, context, selectMembers, null, fromTransfer,
                length);
    }

    /**
     * ���췽��
     * ���½����顱����ʹ�õĹ��췽��
     *
     * @param layout        ��Ҫ���ؼ���layout
     * @param context       ��Ҫ���ؼ���������
     * @param selectMembers ��Ҫ���ؼ�������list
     * @param listener      Ϊ�ؼ�ɾ���ļ����¼�����һ���ص�
     */
    public DisplayMemberWidget(RelativeLayout layout, Context context,
                               List<AddMemberEntity> selectMembers,
                               ItemRemoveListener listener, int fromTransfer, int length)
    {
        initViewLayout(layout, context, selectMembers, listener, fromTransfer,
                length);
    }


    /**
     * ��ʼ�����пؼ���������λ��
     *
     * @param layout   ��Ҫ���ؼ���layout
     * @param context  ��Ҫ���ؼ���������
     * @param members  ��Ҫ���ؼ��ĸ�����id,��ʾ������
     * @param listener ɾ���ؼ�ʱ�Ļص�
     */
    public void initViewLayout(RelativeLayout layout, Context context,
                               List<AddMemberEntity> members, ItemRemoveListener listener,
                               int fromTransfer, int length)
    {
        this.context = context;
        this.layout = layout;
        this.listener = listener;
        this.length = length;
        this.fromTransfer = fromTransfer;

        if (members != null && !members.isEmpty())
        {
            this.selectMembers = members;

            top = 0;
            width = 0;
            lines = 1;
            more = false;

            widgetLogic = new DisplayMemberWidgetLogic();
            RelativeLayout infoLayout;
            RelativeLayout opterateLayout = null;
            TextView txtView;
            ImageView statusImage;
            ImageView opterateImage = null;

            layout.removeAllViews();
            setFirstViewAttrue(layout, context);

            int size = selectMembers.size();
            for (int i = 1; i < size; i++)
            {
                if (MemberContant.CONF_DITAIL_ACTIVITY == fromTransfer && lines == 4)
                {
                    break;
                }
                infoLayout = new RelativeLayout(context);
                txtView = new TextView(context);
                statusImage = new ImageView(context);
                if (MemberContant.CONF_DITAIL_ACTIVITY != fromTransfer)
                {
                    opterateLayout = new RelativeLayout(context);
                    opterateImage = new ImageView(context);
                    setimageViewOnClicListener(opterateImage, layout, i + 1);
                }

                initViewAttrue(infoLayout, txtView, statusImage,
                        opterateLayout, opterateImage, i + 1,
                        selectMembers.get(i));
                setViewPosition(txtView, infoLayout, opterateLayout, i, layout);
            }

            if (MemberContant.CONF_DITAIL_ACTIVITY == fromTransfer && lines < 3)
            {
                setAddOrMoreViewAttrue(layout, 0);
            }
            else if (MemberContant.CONF_DITAIL_ACTIVITY != fromTransfer && !selectMembers.isEmpty())
            {
                setAddOrMoreViewAttrue(layout, 0);
            }
        }
    }

    /**
     * ���á����ࡱ�ؼ������Ժ�λ��
     *
     * @param layout ��䡰���ࡱ�ؼ���layout
     * @param dialId �����ڻ����������������Ҫ����ǰ������
     */
    private void setAddOrMoreViewAttrue(RelativeLayout layout, int dialId)
    {
        int id;
        int postion;
        more = true;

        RelativeLayout moreLayout = new RelativeLayout(context);
        TextView txtView = new TextView(context);
        if (dialId > 0)
        {
            id = dialId;
            postion = id - 1;
        }
        else
        {
            id = selectMembers.size() + 1;
            postion = selectMembers.size();
        }

        initAddOrMoreAttrue(moreLayout, txtView, id);

        setViewPosition(txtView, moreLayout, null, postion, layout);
    }

    /**
     * ���á����ࡱ�ؼ�������
     *
     * @param textView �����ࡱ�ؼ�����벿��
     * @param id       �����ࡱ�ؼ���Id
     */
    private void initAddOrMoreAttrue(final RelativeLayout moreLayout, final TextView textView, int id)
    {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.leftMargin = 10;
        if (lines > 1)
        {
            layoutParams.topMargin = 10;
        }

        if (MemberContant.CONF_CREATE_ACTIVITY == fromTransfer)
        {
            textView.setBackgroundResource(R.drawable.meeting_add_txt);
        }
        else
        {
            textView.setBackgroundResource(R.drawable.meeting_more_txt);
        }
        textView.setId(id); //for automatic test

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                getViewWidthOrHeigth(textView, true) + 20, getViewWidthOrHeigth(
                textView, false) + 10
        );
        moreLayout.setLayoutParams(params);
        moreLayout.setId(id);
        moreLayout.addView(textView, layoutParams);

        moreLayout.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    if (MemberContant.CONF_CREATE_ACTIVITY == fromTransfer)
                    {
                        textView.setBackgroundResource(R.drawable.meeting_more_add_pressed);
                    }
                    else
                    {
                        textView.setBackgroundResource(R.drawable.meeting_more_txt_pressed);
                    }
                }
                else if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL)
                {
                    if (MemberContant.CONF_CREATE_ACTIVITY == fromTransfer)
                    {
                        textView.setBackgroundResource(R.drawable.meeting_add_txt);
                    }
                    else
                    {
                        textView.setBackgroundResource(R.drawable.meeting_more_txt);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP)
                    {
                        if (MemberContant.CONF_CREATE_ACTIVITY == fromTransfer)
                        {
                            mHandler.sendEmptyMessage(MemberContant.SKIP_ADD);
                        }
                        else
                        {
                            mHandler.sendEmptyMessage(MemberContant.SKIP_MORE);
                        }
                    }
                }
                return true;
            }
        });
    }

    /**
     * ��ʼ����һ���ؼ�������
     *
     * @param layout  ���ؼ���layout
     * @param context ���ؼ���������
     */
    private void setFirstViewAttrue(RelativeLayout layout, Context context)
    {
        RelativeLayout hostInfoLayout = new RelativeLayout(context);

        TextView textView1 = new TextView(context);
        ImageView statusImage = new ImageView(context);

        RelativeLayout hostLayout = new RelativeLayout(context);
        ImageView hostImage = new ImageView(context);
        initViewAttrue(hostInfoLayout, textView1, statusImage, hostLayout,
                hostImage, 1, selectMembers.get(0));
        setFirstViewPosition(hostInfoLayout, hostLayout, 1, layout);
    }

    /**
     * ��ʼ�����ҿؼ���һЩ����
     *
     * @param id           �ؼ���Id,���Id�Ǹ���list���±������õ�
     * @param entity �������SelectMember�����ؼ���ʾ����
     */
    private void initViewAttrue(RelativeLayout infoLyt, TextView tv,
                                ImageView statusIv, RelativeLayout optLyt,
                                ImageView optIv, int id, AddMemberEntity entity)
    {
        int normalLength;

        String account = entity.getAccount();
        boolean visible = widgetLogic.getStatusResource(account, statusIv);
        if (visible)
        {
            statusIv.setId(id + 10);
        }

        tv.setTextColor(context.getResources().getColor(R.color.item_member));
        tv.setTextSize(14);
        tv.setSingleLine(true);
        tv.setGravity(Gravity.CENTER);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setText(entity.getName());

        handleInfoBg(infoLyt, entity);
        handleOptBg(optIv, id, entity);

        if (optIv != null)
        {
            RelativeLayout.LayoutParams mParams;
            int width = getViewWidthOrHeigth(optIv, true);
            int height = getViewWidthOrHeigth(optIv, false);
            mParams = new RelativeLayout.LayoutParams(width, height);

            if (optLyt != null)
            {
                optLyt.setLayoutParams(mParams);
                optLyt.addView(optIv);
            }

            tv.setPadding(0, 0, 5, 0);
        }

        handleVisible(infoLyt, statusIv, optLyt, entity, visible);

        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        infoParams.addRule(RelativeLayout.CENTER_VERTICAL);

        infoLyt.setLayoutParams(infoParams);
        infoLyt.setId(id);

        RelativeLayout.LayoutParams nameParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        nameParams.addRule(RelativeLayout.CENTER_VERTICAL);

        if (visible)
        {
            nameParams.addRule(RelativeLayout.RIGHT_OF, id + 10);
            nameParams.addRule(RelativeLayout.ALIGN_TOP, id + 10);
        }
        else
        {
            if (optLyt != null && !isBookPurview(entity))
            {
                nameParams.leftMargin = 20;
            }
        }

        infoLyt.addView(tv, nameParams);

        normalLength = getViewWidthOrHeigth(infoLyt, true);

        if (optLyt != null)
        {
            normalLength = normalLength + getViewWidthOrHeigth(optLyt, true);
        }

        if (normalLength > length)
        {
            int statusWidth = computeStatusWidth(statusIv, visible);
            computeTvWidth(tv, optLyt, optIv, statusWidth);

            infoLyt.removeView(tv);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

            if (visible)
            {
                layoutParams.addRule(RelativeLayout.RIGHT_OF, id + 10);
                layoutParams.addRule(RelativeLayout.ALIGN_TOP, id + 10);
            }

            infoLyt.addView(tv, layoutParams);
        }
    }

    private void computeTvWidth(TextView tv, RelativeLayout optLyt, ImageView optIv,
                                int statusWidth)
    {
        if (optIv == null)
        {
            int commentWidth = UIUtil.dipToPx(7);
            tv.setWidth(length - statusWidth - commentWidth * 3);
        }
        else
        {
            if (optLyt != null)
            {
                int commentWidth = UIUtil.dipToPx(7);
                int value = getViewWidthOrHeigth(optLyt, true);
                tv.setWidth(length - value - statusWidth - commentWidth);
            }
        }
    }

    private int computeStatusWidth(ImageView statusIv, boolean visible)
    {
        return visible ? getViewWidthOrHeigth(statusIv, true) : 0;
    }

    private void handleVisible(RelativeLayout infoLyt, ImageView statusIv
            , RelativeLayout optLyt, AddMemberEntity entity, boolean visible)
    {
        if (visible)
        {
            int width = RelativeLayout.LayoutParams.WRAP_CONTENT;
            int height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            RelativeLayout.LayoutParams nParams = new RelativeLayout.LayoutParams(width, height);

            nParams.addRule(RelativeLayout.CENTER_VERTICAL);
            nParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            if (optLyt != null)
            {
                if (isBook && TextUtils.isEmpty(entity.getAccount()))
                {
                    nParams.leftMargin = -2;
                }
                else
                {
                    nParams.leftMargin = 12;
                }
            }
            else
            {
                nParams.leftMargin = -2;
            }

            infoLyt.addView(statusIv, nParams);
        }
    }

    private void handleOptBg(ImageView optImg, int id, AddMemberEntity entity)
    {
        if (id == 1)
        {
            if (MemberContant.CONF_ADD_MEMBER_ACTIVITY == fromTransfer && optImg != null)
            {
                optImg.setBackgroundResource(R.drawable.capsule_operate_blue_select);
                setimageViewOnClicListener(optImg, layout, id);
            }
            else
            {
                if (null != optImg && entity.isFirstMember())
                {
                    optImg.setBackgroundResource(R.drawable.meeting_add_host);
                }
            }
        }
        else
        {
            if (null != optImg)
            {
                if (isBook && TextUtils.isEmpty(entity.getAccount()))
                {
                    optImg.setBackgroundResource(R.drawable.capsule_operate_grey_select);
                }
                else
                {
                    optImg.setBackgroundResource(R.drawable.capsule_operate_blue_select);
                }
            }
        }
    }

    private void handleInfoBg(RelativeLayout infoLyt, AddMemberEntity entity)
    {
        if (MemberContant.CONF_DITAIL_ACTIVITY == fromTransfer && !entity.isFirstMember())
        {
            infoLyt.setBackgroundResource(R.drawable.btn_title_name);
        }
        else
        {
            if (isBookPurview(entity))
            {
                infoLyt.setBackgroundResource(R.drawable.btn_name_disable);
            }
            else
            {
                infoLyt.setBackgroundResource(R.drawable.btn_name_host);
            }
        }
    }

    /**
     * layout�����һ�����ڡ������ˡ��Ŀؼ�����������������λ��
     *
     * @param hostInfoLayout �ؼ������ֲ���
     * @param hostLayout     �ؼ��Ŀ�ɾ������
     * @param id             ��һ���ؼ���IdΪ1
     * @param layout         ��Ҫ���ؼ���layout
     */
    private void setFirstViewPosition(RelativeLayout hostInfoLayout, RelativeLayout hostLayout,
                                      int id, RelativeLayout layout)
    {
        setFirstViewPosition(hostInfoLayout, layout);

        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params2.addRule(RelativeLayout.RIGHT_OF, id);
        layout.addView(hostLayout, params2);
        imageViewWidth = getViewWidthOrHeigth(hostLayout, true);
        width = width + imageViewWidth;
    }

    private void setFirstViewPosition(RelativeLayout hostInfoLayout,
                                      RelativeLayout layout)
    {
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params1.topMargin = top;
        params1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layout.addView(hostInfoLayout, params1);
        width = getViewWidthOrHeigth(hostInfoLayout, true);
    }

    /**
     * ���ÿؼ���layout��λ��
     *
     * @param txtView        �ؼ������ֲ���
     * @param infoLayout     �ؼ��Ŀ�ɾ������
     * @param optLayout ��¼layout����ȥ�Ŀؼ��ĵ�ǰ�ܸ߶�
     * @param rightOfId      ��¼layout�У���ͬһ��ʱ����ߵĿؼ�Id
     * @param layout         ��Ҫ���ؼ���layout
     */
    private void setViewPosition(TextView txtView, RelativeLayout infoLayout,
                                 RelativeLayout optLayout, int rightOfId, RelativeLayout layout)
    {
        RelativeLayout.LayoutParams mParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams nParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        int nWidth = getViewWidthOrHeigth(infoLayout, true);
        if (null != optLayout)
        {
            nWidth = nWidth + getViewWidthOrHeigth(optLayout, true);
        }

        width = width + nWidth + (more ? 0 : 10);

        if (length > width)
        {
            mParams.addRule(RelativeLayout.RIGHT_OF, rightOfId);
            mParams.topMargin = top - ((more && lines > 1) ? 10 : 0);

            leftMargin(rightOfId, mParams);
            layout.addView(infoLayout, mParams);

            if (null != optLayout)
            {
                nParams.topMargin = top;
                int anchor = infoLayout.getId();
                nParams.addRule(RelativeLayout.RIGHT_OF, anchor);

                layout.addView(optLayout, nParams);
                imageViewWidth = getViewWidthOrHeigth(optLayout, true);
            }
        }
        else
        {
            lines = lines + 1;
            if (MemberContant.CONF_DITAIL_ACTIVITY == fromTransfer && lines == 3)
            {
                setAddOrMoreViewAttrue(layout, infoLayout.getId());
                return;
            }

            mParams.addRule(RelativeLayout.BELOW, 1);

            topMargin(txtView, infoLayout, mParams);
            layout.addView(infoLayout, mParams);

            int anchor = infoLayout.getId();
            addLayout(anchor, optLayout, layout, nParams);

            width = nWidth;
            top = top + getViewWidthOrHeigth(infoLayout, false) + 10;

            if (MemberContant.CONF_ADD_MEMBER_ACTIVITY == fromTransfer)
            {
                mHandler.sendEmptyMessage(MemberContant.UPDATE_SCROLLVIEW);
            }
        }
    }

    private void addLayout(int anchor, RelativeLayout
            source, RelativeLayout target, RelativeLayout.LayoutParams params)
    {
        if (null != source)
        {
            params.topMargin = top + 10;
            params.addRule(RelativeLayout.BELOW, 1);
            params.addRule(RelativeLayout.RIGHT_OF, anchor);

            target.addView(source, params);
        }
    }

    private void topMargin(TextView tv, RelativeLayout rl,RelativeLayout.LayoutParams mParams)
    {
        if (!more)
        {
            mParams.topMargin = top + 10;
        }
        else
        {
            int wrap = RelativeLayout.LayoutParams.WRAP_CONTENT;
            RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(wrap, wrap);
            lParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lParams.topMargin = 10;

            rl.removeAllViews();
            rl.addView(tv, lParams);

            mParams.topMargin = top;
        }
    }

    private void leftMargin(int rightOfId, RelativeLayout.LayoutParams mParams)
    {
        if (rightOfId <= 0)
        {
            return;
        }

        if (MemberContant.CONF_DITAIL_ACTIVITY == fromTransfer && rightOfId > 1)
        {
            if (!more)
            {
                mParams.leftMargin = 10;
            }
        }
        else
        {
            mParams.leftMargin = imageViewWidth + (more ? 0 : 10);
        }
    }

    /**
     * ������ɾ�������ܵĿؼ��ĵ���¼�
     *
     * @param opterateImage ��ɾ�����õĿؼ�
     * @param layout        �����������layout
     * @param id            ����Id,ɾ����������list�����Ӧ����
     */
    private void setimageViewOnClicListener(final ImageView opterateImage,
                                            final RelativeLayout layout, final int id)
    {
        opterateImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                int index = 0;
                if (id > 1)
                {
                    index = id - 1;
                }
                listener.removeItem(index);
                selectMembers.remove(index);
                if (!selectMembers.isEmpty())
                {
                    initViewLayout(layout, context, selectMembers, listener, fromTransfer, length);
                }
                else
                {
                    layout.removeAllViews();
                    if (null != layoutHint)
                    {
                        layout.addView(layoutHint);
                    }

                }
            }
        });
    }

    /**
     * ��������ࡱ�Ŀؼ�������handler������Ϣ������
     *
     * @param mHandler
     */
    public void setHandler(Handler mHandler)
    {
        this.mHandler = mHandler;
    }

    /**
     * ����isWidth���жϣ���ȡ�ؼ��Ŀ�Ȼ��߸߶�
     *
     * @param view    Ҫ��ȡ�䳤�Ȼ��ȵĿؼ�view
     * @param isWidth ��ȡ�ؼ��Ŀ�Ȼ��Ǹ߶ȵı�־
     * @return
     */
    private int getViewWidthOrHeigth(View view, boolean isWidth)
    {
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        view.measure(w, h);

        if (isWidth)
        {
            return view.getMeasuredWidth();
        }
        else
        {
            return view.getMeasuredHeight();
        }
    }

    /**
     * ɾ������ߵļ����¼�
     */
    public interface ItemRemoveListener
    {
        void removeItem(int index);
    }

    public void notifyView(List<AddMemberEntity> memberEntities)
    {
        if (null != memberEntities && !memberEntities.isEmpty())
        {
            initViewLayout(layout, context, memberEntities, listener, fromTransfer, length);
        }
    }

    public void setLayoutHint(TextView layoutHint)
    {
        this.layoutHint = layoutHint;
    }

    public void setBookConfFlag(boolean isBook)
    {
        this.isBook = isBook;
    }

    private boolean isBookPurview(AddMemberEntity entity)
    {
        return isBook && TextUtils.isEmpty(entity.getAccount());
    }
}
