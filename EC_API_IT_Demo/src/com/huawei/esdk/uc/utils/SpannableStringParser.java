package com.huawei.esdk.uc.utils;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import com.huawei.common.res.LocContext;
import com.huawei.contacts.ContactLogic;
import com.huawei.data.Message;
import com.huawei.data.entity.InstantMessage;
import com.huawei.data.unifiedmessage.JsonMultiUniMessage;
import com.huawei.data.unifiedmessage.MediaResource;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.application.UCAPIApp;
import com.huawei.esdk.uc.widget.ClickableImageSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * //todo SpannableString 改为 SpannableStringBuilder，效率应该更好，具体优势还需分析
 * 类名称：SpannableStringParser 作者： Luo Tianjia 创建时间：2011-6-8 类描述 ： TextView的工具类，
 * 将TextView中需要标记的 图片 链接标记出来 版权声明： Copyright (C) 2008-2010 华为技术有限公司(Huawei
 * Tech.Co.,Ltd) 修改时间：
 */
public class SpannableStringParser
{
	private static final String TAG = SpannableStringParser.class.getSimpleName();
    private static final String FAIL = "fail";
    private static final String EMPTY = " ";
    private final Pattern urlPattern;
    private boolean showUnderLine = true;
    private final Pattern emotionPattern;
    private final Pattern numberPattern;
    private UmParser umParser;
    //private Context context;

    public SpannableStringParser()
    {
        umParser = new UmParser();
        urlPattern = Pattern.compile(
                "(http://|https://|ftp://|www\\.)[[^\\s]&&[^\\u4E00-\\u9Fa5]]+", Pattern.CASE_INSENSITIVE);
        numberPattern = Pattern.compile("(/:|/){0,1}[0-9]+");
        emotionPattern = Pattern.compile("(/:D|/:\\)|/:\\*"
                + "|/:8|/D~|/\\-\\(|/\\-O|/:\\$|/CO|/YD|/;\\)|/;P"
                + "|/:!|/:0|/GB|/:S|/:\\?|/:Z|/88|/SX"
                + "|/TY|/OT|/NM|/\\:X|/DR|/:<|/ZB|/BH|/HL"
                + "|/XS|/YH|/KI|/DX|/KF|/KL|/LW|/PG|/XG"
                + "|/CF|/TQ|/DH|/\\*\\*|/@@|/:\\{|/FN|/0\\(|/;>"
                + "|/FD|/ZC|/JC|/ZK|/:\\(|/LH|/SK|/\\$D|/CY"
                + "|/\\%S|/LO|/PI|/DB|/MO|/YY|/FF|/ZG|/;I"
                + "|/XY|/MA|/GO|/\\%@|/ZD|/SU|/MI|/BO"
                + "|/GI|/DS|/YS|/DY|/SZ|/DP|/:\\\\|/00)");
    }

    public SpannableStringParser(boolean showUnderLine)
    {
        this();
        this.showUnderLine = showUnderLine;
    }

    public SpannableStringParser(Context context)
    {
        this();
        //this.context = context;
    }

    private int getIndex(String tagName, String[] array)
    {
        int length = array.length;
        for (int i = 0; i < length; i++)
        {
            if (tagName.equals(array[i]))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取表情span
     *
     * @param tagName
     * @return
     */
    public ImageSpan getEmotionSpan(String tagName)
    {
//        int index = getIndex(tagName, ChatCommon.EMOTION_STR.split("\\|"));
//        if (index != -1)
//        {
//            Drawable drawable = UCAPIApp.getApp().getResources().getDrawable(R.drawable.emotion01 + index);
//            if (drawable != null)
//            {
//                int width = CommonUtil.dipTopx(20f);
//                drawable.setBounds(0, 0, width, width);
//                return new ImageSpan(drawable);
//            }
//        }
        return null;
    }

    /**
     * 解析会话的span数据
     * 会话显示数据时，不显示链接和url。
     *
     * @param textView
     * @param msg
     */
    public void parseConversationSpan(TextView textView, InstantMessage msg)
    {
        if (!checkValidate(textView, msg))
        {
            return;
        }
        try
        {
            String strMessage = msg.getContent();

            //strMessage=replaceBlank(strMessage);
            /*if (strMessage != null)
            {
                strMessage = getShowTextLength(strMessage);
            }*/
            String showStr;
            SpannableString ss;
            if (msg.getMediaType() == MediaResource.TYPE_NORMAL)
            {
                ss = parseEmotion(strMessage);
                showStr = ss == null ? strMessage : "";
            }
            else if (msg.getMediaType() == MediaResource.MEDIA_JSON_MUTLI
                    && msg.getMediaRes() != null && msg
                    .getMediaRes() instanceof JsonMultiUniMessage)
            {
                JsonMultiUniMessage json = (JsonMultiUniMessage) msg
                        .getMediaRes();
                ss = parseEmotion(json.getTitle());
                showStr = ss == null ? json.getTitle() : "";
            }
            else
            {
                ss = umParser.getUmSpannableString(msg);
                showStr = ss == null ? strMessage : "";
            }
            if (TextUtils.isEmpty(textView.getText().toString()))
            {
                textView.setText(ss == null ? showStr : ss);
            }
            else
            {
                textView.append(ss == null ? showStr : ss);
            }
            textView.setFocusableInTouchMode(false);
            textView.setFocusable(false);
        }
        catch (Exception e)
        {
            Logger.warn(TAG, e.toString());
        }
    }

    /**
     * 解析聊天界面显示的Span数据。
     * 1 解析url
     * 2 解析电话号码
     * 3 解析表情
     *
     * @param text
     * @param isSend
     * @return 如果有span，返回SpannableString；没有则返回字符串。
     */
    public CharSequence parseSpan(String text, boolean isSend)
    {
        if (TextUtils.isEmpty(text))
        {
            return text;
        }
        SpannableString ss = null;
        try
        {
            //解析表情,如果没有匹配到返回null。
            ss = parseEmotion(text);
            //解析网址,如果没有匹配到返回ss参数。
            ss = parseUrl(ss, text, isSend);
            //解析电话号码,如果没有匹配到返回ss参数。
            ss = parseNumber(ss, text, isSend);
        }
        catch (Exception e)
        {
            Logger.warn(TAG, e.toString());
        }
        return ss == null ? text : ss;
    }

    /**
     * 加载span
     *
     * @param text
     * @param textView
     */
    public void applySpan(String text, TextView textView)
    {
        CharSequence mText = parseSpan(text, false);
        textView.setText(mText);
        if (mText != null && mText instanceof SpannableString)
        {
            textView.setMovementMethod(MyLinkMovementMethod.getMyInstance());
            textView.setFocusableInTouchMode(false);
            textView.setFocusable(false);
        }
    }

    /**
     * 检测是否要转为SpannableString
     *
     * @param tx         字符显示控件
     * @param instantMsg 聊天消息
     * @return
     */
    private boolean checkValidate(TextView tx, InstantMessage instantMsg)
    {
        if (null == instantMsg || TextUtils.isEmpty(instantMsg.getContent()))
        {
            tx.setText("");
            return false;
        }
        //离线文件
        if (instantMsg.getType() == Message.IM_FILE_TRANSFER)
        {
            String content;
            if (instantMsg.isSender())
            {
                content = LocContext.getContext()
                        .getString(R.string.um_file_sender_tip);
            }
            else
            {
                content = UCAPIApp.getApp()
                        .getString(R.string.um_file_receiver_tip);
            }
            tx.setText(content);
            return false;
        }
        return true;
    }

    /**
     * Add a tip when fail
     *
     * @param tx             TextView
     * @param msg            InstantMessage
     * @param isConversation Is Conversation
     */
    public void addFailTip(final TextView tx, final InstantMessage msg, boolean isConversation)
    {
        addFailTip(tx, msg, false, isConversation);
    }

    /**
     * Add a tip when fail
     *
     * @param tx             TextView
     * @param msg            InstantMessage
     * @param responseClick  Is Clickable
     * @param isConversation Is Conversation
     */
    private void addFailTip(final TextView tx, final InstantMessage msg,
            boolean responseClick, boolean isConversation)
    {
        if (msg == null || !InstantMessage.STATUS_SEND_FAILED.equals(msg.getStatus()))
        {
            return;
        }
        final SpannableString str = new SpannableString(FAIL);
        final ClickableImageSpan span = new ClickableImageSpan(
                LocContext.getContext(), R.drawable.androidphone_recent_im_cue,
                R.drawable.androidphone_recent_im_cue_click);
        str.setSpan(span, 0, FAIL.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        if (responseClick)
        {
            str.setSpan(new TempClickableSpan(span, tx), 0, FAIL.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        if (!isConversation)
        {
            tx.append(EMPTY);
        }
        tx.append(str);
        if (isConversation)
        {
            tx.append(EMPTY);
        }
    }

    private static class TempClickableSpan extends ClickableSpan
    {
        private ClickableImageSpan span;
        private TextView tx;

        TempClickableSpan(ClickableImageSpan span, TextView tx)
        {
            this.span = span;
            this.tx = tx;
        }

        @Override
        public void onClick(View widget)
        {
            span.updateDrawState(tx);
        }
    }

    /**
     * 解析电话号码
     *
     * @param ss      Spannable String
     * @param content
     * @param isSelf
     * @return 如果没有匹配到，返回ss； 如果匹配到，则返回匹配后的span
     */
    private SpannableString parseNumber(SpannableString ss, String content, final boolean isSelf)
    {
        List<Tag> listTag2 = getTags(content, numberPattern);
        if (listTag2 == null || listTag2.isEmpty())
        {
            return ss;
        }
        if (ss == null)
        {
            ss = new SpannableString(content);
        }
        String tagName;
        String num;
        for (Tag t : listTag2)
        {
            tagName = t.getTagStr();
            if (hasChild(tagName))
            {
                String name = t.getTagStr().replace("/88", "")
                        .replace("/:8", "").replace("/:0", "")
                        .replace("/00", "");
                t.setTagStr(name);
                t.setIndexBegin(t.getIndexBegin() + 3);
                if (name.length() < 3)
                {
                    continue;
                }
            }
            else if (t.getTagStr().contains("/:"))
            {
                t.setTagStr(t.getTagStr().replace("/:", ""));
                t.setIndexBegin(t.getIndexBegin() + 2);
            }
            else if (t.getTagStr().contains("/"))
            {
                t.setTagStr(t.getTagStr().replace("/", ""));
                if (t.getTagStr().length() < 3)
                {
                    continue;
                }
                t.setIndexBegin(t.getIndexBegin() + 1);
            }
            num = t.getTagStr();
            ss.setSpan(getClickableSpan(num, isSelf), t.getIndexBegin(), t.getIndexEnd(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }

    private ClickableSpan getClickableSpan(final String num, final boolean isSelf)
    {
        ClickableSpan cs = new ClickableSpan()
        {
            @Override
            public void onClick(View widget)
            {
                showChooseDialog(num);
            }

            @Override
            public void updateDrawState(TextPaint ds)
            {
                ds.setColor(LocContext.getContext().getResources()
                        .getColor(isSelf ? R.color.primary
                                : R.color.textunderline));
                ds.setUnderlineText(showUnderLine);
            }
        };
        return cs;
    }

    /**
     * 解析网址
     */
    private SpannableString parseUrl(SpannableString ss, String text, final boolean isSelf)
    {
        List<Tag> listTag = getTags(text, urlPattern);
        if (listTag == null || listTag.isEmpty())
        {
            return ss;
        }
        if (ss == null)
        {
            ss = new SpannableString(text);
        }
        URLSpan urlSpan;
        for (Tag t : listTag)
        {
            if (ss.getSpans(t.getIndexBegin(), t.getIndexEnd(),
                    ImageSpan.class) != null)
            {
                ss.removeSpan(ss.getSpans(t.getIndexBegin(),
                        t.getIndexEnd(), ImageSpan.class));
            }
            String urlStr = parseHttpLowerCase(t.getTagStr());
            if (urlStr != null && urlStr.toLowerCase(Locale.ENGLISH).startsWith("www."))
            {
                urlStr = "http://" + urlStr;
            }
            urlSpan = new URLSpan(urlStr)
            {
                @Override
                public void updateDrawState(TextPaint ds)
                {
                    ds.setUnderlineText(showUnderLine);
                    ds.setColor(LocContext.getContext().getResources()
                            .getColor(isSelf ? R.color.primary
                                    : R.color.textunderline));
                }

                @Override
                public void onClick(final View widget)
                {
                    if (!ContactLogic.getIns().getMyOtherInfo().isEnableAnyOfficeBrowser())
                    {
                        try
                        {
                            super.onClick(widget);
                        }
                        catch (Exception ex)
                        {
                            Logger.debug(TAG, "error url");
                        }
                        return;
                    }
                    else
                    {
                        Intent it = new Intent("com.huawei.espace.anyofficeBrowser");
                        it.putExtra("url", getURL());
                        widget.getContext().startActivity(it);
                    }
//                    DialogUtil.showChooseBrowerDialog(widget.getContext(), getURL());
                }
            };
            ss.setSpan(urlSpan, t.getIndexBegin(), t.getIndexEnd(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }

    /**
     * 将https，http， ftp都转为小写
     *
     * @param tagStr 被转换的字符串
     * @return
     */
    private String parseHttpLowerCase(String tagStr)
    {
        if (tagStr == null)
        {
            return null;
        }
        int index = tagStr.indexOf("://");
        if (index <= 0)
        {
            return tagStr;
        }
        return tagStr.substring(0, index).toLowerCase(Locale.ENGLISH) + tagStr.substring(index);
    }

    /**
     * 解析表情
     *
     * @param text Text
     */
    public SpannableString parseEmotion(String text)
    {
        List<Tag> urlTags = getTags(text, urlPattern);
        List<Tag> listTag = getTags(text, emotionPattern);
        if (listTag == null || listTag.isEmpty())
        {
            return null;
        }
        SpannableString ss = SpannableString.valueOf(text);
        ImageSpan span1;
        boolean isInUrl;
        for (Tag t : listTag)
        {
            if (null != urlTags && !urlTags.isEmpty())
            {
                //如果表情在url中，则不解析表情
                isInUrl = false;
                for (Tag tag : urlTags)
                {
                    if ((t.getIndexBegin() >= tag.getIndexBegin()) && (t.getIndexEnd() <= tag.getIndexEnd()))
                    {
                        isInUrl = true;
                        break;
                    }
                }
                if (isInUrl)
                {
                    continue;
                }
            }
            span1 = getEmotionSpan(t.getTagStr());
            if (span1 != null)
            {
                ss.setSpan(span1, t.getIndexBegin(), t.getIndexEnd(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return ss;
    }

    /**
     * 判断是否含有表情
     *
     * @param text Text //no use now
     */
    /*public boolean  IsParseEmotion(String text)
    {
        List<Tag> urlTags = getTags(text, urlPattern);
        List<Tag> listTag = getTags(text, emotionPattern);
        if (listTag == null || listTag.isEmpty())
        {
            return false;
        }
        else
        {
            return  true;
        }

    }*/
    public SpannableString parseInnerEmotionShow(String text)
    {

        String strMessage =text;
       /*String strMessage=replaceBlank(text);
        if (strMessage != null)
        {
            strMessage = getShowTextLength(strMessage);
        }*/
        List<Tag> listTag = getTags(strMessage, emotionPattern);
        if (listTag == null || listTag.isEmpty())
        {
            return new SpannableString(strMessage);
        }
        SpannableString ss = SpannableString.valueOf(strMessage);
        ImageSpan span1;
        for (Tag t : listTag)
        {
            span1 = getEmotionSpan(t.getTagStr());
            if (span1 != null)
            {
                ss.setSpan(span1, t.getIndexBegin(), t.getIndexEnd(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return ss;
    }

    public SpannableString parseInnerEmotion(String text)
    {
        List<Tag> listTag = getTags(text, emotionPattern);
        if (listTag == null || listTag.isEmpty())
        {
            return new SpannableString(text);
        }
        SpannableString ss = SpannableString.valueOf(text);
        ImageSpan span1;
        for (Tag t : listTag)
        {
            span1 = getEmotionSpan(t.getTagStr());
            if (span1 != null)
            {
                ss.setSpan(span1, t.getIndexBegin(), t.getIndexEnd(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return ss;
    }

    /**
     * 类名称：Tag 作者： Luo Tianjia 创建时间：2011-6-8 类描述 ： 标签实体类， 用来按规则区分String 中的标签
     * 版权声明： Copyright (C) 2008-2010 华为技术有限公司(Huawei Tech.Co.,Ltd) 修改时间：
     */
    private static class Tag
    {
        int indexBegin;
        int indexEnd;
        String tagStr;

        public int getIndexBegin()
        {
            return indexBegin;
        }

        public void setIndexBegin(int indexBegin)
        {
            this.indexBegin = indexBegin;
        }

        public int getIndexEnd()
        {
            return indexEnd;
        }

        public void setIndexEnd(int indexEnd)
        {
            this.indexEnd = indexEnd;
        }

        public String getTagStr()
        {
            return tagStr;
        }

        public void setTagStr(String tagStr)
        {
            this.tagStr = tagStr;
        }
    }

    private List<Tag> getTags(String text, Pattern pattern)
    {
        List<Tag> listTag = new ArrayList<Tag>();
        if (text == null || text.length() == 0)
        {
            return null;
        }
        Matcher urlMatcher = pattern.matcher(text);
        int indexBegin;
        int indexEnd;
        Tag t;
        while (urlMatcher.find())
        {
            indexBegin = urlMatcher.start();
            indexEnd = urlMatcher.end();
            t = new Tag();
            t.setIndexBegin(indexBegin);
            t.setIndexEnd(indexEnd);
            t.setTagStr(text.substring(indexBegin, indexEnd));
            if (pattern.equals(numberPattern))
            {
                // 电话号码限制位数
                if (t.getTagStr().length() <= 21 && t.getTagStr().length() >= 5)
                {
                    listTag.add(t);
                }
            }
            else
            {
                listTag.add(t);
            }
        }
        return listTag;
    }

    public String getFirstUrl(String text)
    {
        List<Tag> tags = getTags(text, urlPattern);
        if (tags == null || tags.isEmpty())
        {
            return null;
        }
        return tags.get(0).getTagStr();
    }

    private boolean hasChild(String text)
    {
        Pattern urlPattern = Pattern.compile("(/:8|/88|/:0|/00)");
        Matcher urlMatcher = urlPattern.matcher(text);
        return urlMatcher.find();
    }

    /**
     * 方法名称：showChooseDialog
     * 作者：xuchunping
     * 方法描述：呼叫提取号码
     * 输入参数:@param num
     * 返回类型：void 备注：
     */
    public void showChooseDialog(final String num)
    {
//        List<Object> data = new ArrayList<Object>();
//        data.add(UCAPIApp.getApp().getString(R.string.dial) + "  " + num);
//        data.add(UCAPIApp.getApp().getString(R.string.edit_num_before_dial));
//        final SimpleListDialog dialog = new SimpleListDialog(
//        		UCAPIApp.getApp().getCurActivity(), data);
//        dialog.setOnItemClickListener(new ShowChooseItemClick(dialog, num));
//        dialog.show();
    }

//    private static class ShowChooseItemClick implements OnItemClickListener
//    {
//        private SimpleListDialog dialog;
//        private String num;
//
//        ShowChooseItemClick(SimpleListDialog dialog, String num)
//        {
//            this.dialog = dialog;
//            this.num = num;
//        }
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view,
//                int position, long id)
//        {
//            dialog.dismiss();
//            if (0 == position)
//            {
//                // 直接呼叫
//                CallFunc.getIns().dial(num);
//            }
//            else if (1 == position)
//            {
//                // 去拨号页面
//            	UCAPIApp.getApp().popupAbove(MainActivity.class);
//                Intent intent = new Intent(ActivityStack.getIns()
//                        .getCurActivity(), MainActivity.class);
//                intent.putExtra(IntentData.PHONE_NUMBER_CALLED, num);
//                ActivityStack.getIns().getCurActivity()
//                        .startActivity(intent);
//            }
//        }
//    }

}
