package com.huawei.esdk.uc.widget;

import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.SpannableStringParser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by cWX198123 on 2014/12/18.
 */
public class SpanPasteEditText extends EditText
{
    private Context context;
    private SpannableStringParser parser;
    private boolean needUpdateSelection = true;

    public SpanPasteEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        parser = new SpannableStringParser();
    }

    /**
     * 重载文本粘贴，使粘贴后仍正确显示表情
     * @param id
     * @return
     */
    @Override
    public boolean onTextContextMenuItem(int id)
    {
        if (id == android.R.id.paste && DeviceUtil.hasHoneycomb())
        {
            ClipboardManager clipboardManager = (ClipboardManager) (context
                    .getSystemService(Context.CLIPBOARD_SERVICE));
            if (clipboardManager != null)
            {
                ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null)
                {
                    String text = item.getText().toString();
                    int start = Math.min(getSelectionStart(), getSelectionEnd());
                    int end = Math.max(getSelectionStart(), getSelectionEnd());

                    android.text.Editable tempEditable = getEditableText();
                    if (tempEditable != null)
                    {
                        tempEditable.delete(start, end);
                    }

                    android.text.Editable tempEdi = getEditableText();
                    if (tempEdi != null)
                    {
                        tempEdi.insert(start, parser.parseInnerEmotion(text));
                    }
                }
                return true;
            }
        }
        return super.onTextContextMenuItem(id);
    }

    /**
     * 防止选到表情对应转义字符中的某几个 如/:<，使其成为一个整体。
     * @param selStart
     * @param selEnd
     */
    @Override
    protected void onSelectionChanged(int selStart, int selEnd)
    {
        String text = getText().toString();
        int start = Math.min(selStart, selEnd);
        int end = Math.max(selStart, selEnd);
        int length = text.length();

        if (end - start > 0)
        {
            String first = text.substring(selStart, length);
             int index = first.indexOf('/');
             if (index >= 0 && index+3 <= length)
                {
                    String firstSpan = text.substring(selStart + index, selStart + index+3);
                    if (parser.parseEmotion(firstSpan) != null)
                    {
                        start = selStart + index;
                    }
               }

            String last = text.substring(0, selEnd);
            index = last.lastIndexOf('/');
                if (index >= 0 && index+3 <= length)
                {
                    String lastSpan = text.substring(index, index+3);
                    if (parser.parseEmotion(lastSpan) != null)
                    {
                        end = index+3;
                    }
                }

            //需要把重新得到的start和end设一遍，否则通过getSelectionEnd() getSelectionStart()得到的仍是错的。
            if (needUpdateSelection)
            {
                needUpdateSelection = false;
                setSelection(start, end);
                needUpdateSelection = true;
            }
        }

        super.onSelectionChanged(start, end);
    }
}
