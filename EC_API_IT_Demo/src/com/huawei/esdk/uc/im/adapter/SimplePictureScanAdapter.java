package com.huawei.esdk.uc.im.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.huawei.common.ui.SwitchPager;
import com.huawei.common.ui.ZoomAdapter;
import com.huawei.common.ui.ZoomImageView;
import com.huawei.ecs.mtk.log.Logger;
import com.huawei.esdk.uc.R;
import com.huawei.esdk.uc.utils.DeviceUtil;
import com.huawei.esdk.uc.utils.LocalLog;
import com.huawei.module.um.UmUtil;
import com.huawei.utils.img.BitmapUtil;
import com.huawei.utils.img.ExifOriUtil;
import com.huawei.utils.img.PhotoUtil;
import com.huawei.utils.io.EncryptUtil;
import com.huawei.utils.io.FileUtil;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by h00203586 on 2014/10/13.
 */
public abstract class SimplePictureScanAdapter<T> extends PagerAdapter implements ZoomAdapter
{
	private static final String TAG = SimplePictureScanAdapter.class.getSimpleName();

    protected List<T> datas;

    protected final Activity context;

    private final LayoutInflater inflater;

    private Map<Integer, SwitchPager> pagers = null;

    private boolean debug = false;

    private PictureListener listener;

    public SimplePictureScanAdapter(Activity context)
    {
        this.context = context;
        inflater = LayoutInflater.from(context);

        datas = new ArrayList<T>();
        pagers = new HashMap<Integer, SwitchPager>();
    }

    /**
     * 刷新数据
     * @param msgs
     */
    public void setData(List<T> msgs)
    {
        this.datas = msgs;

        notifyDataSetChanged();
    }

    public T getData(int pos)
    {
        return datas.get(pos);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        if (debug)
        {
            Logger.debug(TAG, "position = " + position);
            Logger.debug(TAG, "container size = " + container.getChildCount());
        }

        //构造view。
        View pictureScan = inflater.inflate(R.layout.picture_scan, null);
        final ScanHolder holder = new ScanHolder();
        pictureScan.setTag(holder);

        final T msg = datas.get(position);

        //添加view。
        addView(container, pictureScan, position);
//bu lwx302895 新增
        holder.relativeLayout = (RelativeLayout) pictureScan.findViewById(R.id.picture_scan_root);

        if (DeviceUtil.hasHoneycomb())
        {
            holder.ivContent = (ZoomImageView)pictureScan.findViewById(R.id.iv_content);
            holder.ivContent.setVisibility(View.GONE);

            holder.ivContent = (ZoomImageView) pictureScan.findViewById(R.id.iv_content2);
        }
        else
        {
            holder.ivContent = (ZoomImageView) pictureScan.findViewById(R.id.iv_content2);
            holder.ivContent.setVisibility(View.GONE);

            holder.ivContent = (ZoomImageView)pictureScan.findViewById(R.id.iv_content);
        }
        holder.ivContent.setVisibility(View.VISIBLE);
        pagers.put(position, holder.ivContent);

        holder.ivContent.setMaxScaleMultiple(3.f);
        holder.ivContent.setMinScaleMultiple(0.3f);
        holder.ivContent.setSingleClick(new ZoomImageView.SingleClick()
        {
            @Override
            public void onSingleClick()
            {
                if (listener != null) listener.onClick();
            }

            @Override
            public boolean onLongClick()
            {
                if (listener != null)
                {
                    return listener.onLongClick();
                }
                return false;
            }
        });
        pictureScan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (listener != null) listener.onClick();
            }
        });

        holder.progressBar = (SeekBar)pictureScan.findViewById(R.id.pb_picturescan);
        holder.loadLayout = (RelativeLayout)pictureScan.findViewById(R.id.layout_load);

        holder.loadTv = (TextView)pictureScan.findViewById(R.id.load_pic_process_txt);
        holder.loadLogo = (ImageView)pictureScan.findViewById(R.id.load_pic_logo);

        holder.loadBtn = (Button)pictureScan.findViewById(R.id.resume_load_btn);
        holder.loadBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                doDownload(holder, msg);
            }
        });

        // holder.rootView = pictureScan.findViewById(R.id.picture_scan_root);

        holder.progress = (ProgressBar)pictureScan.findViewById(R.id.dialog_progressbar);

        //数组填充到布局
        holder.position = position;
        fillInData(msg, holder);

        return pictureScan;
    }

    protected abstract void doDownload(ScanHolder holder, T msg);

    protected abstract void fillInData(T msg, ScanHolder holder);
    
    protected void checkPathForShow(String path, ScanHolder holder)
    {
        String tPath = UmUtil.getThumbnailPath(path);
        decodeBitmapForShow(tPath, holder);  //是否显示。

        decodeBitmapForShowAsync(path, holder);
    }

    protected void decodeBitmapForShowAsync(final String path, final ScanHolder holder)
    {
        new AsyncTask<String, Integer, Bitmap>()
        {
            @Override
            protected Bitmap doInBackground(String... params)
            {
                if (path == null)
                {
                    Logger.warn(LocalLog.APPTAG, "path is empty.");
                    return null;
                }

                return getBitmap(params[0]);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap)
            {
                if (holder.destroy)
                {
                    Logger.debug(LocalLog.APPTAG, "bitmap is destroyed.");
                    return;
                }

                if (bitmap == null)
                {
                    return;
                }

                showBitmap(bitmap, path, holder);
            }
        }.execute(path);
    }

    private void showBitmap(Bitmap bitmap, String path, ScanHolder holder)
    {
        int ori = ExifOriUtil.getExifOrientation(path);
        holder.ivContent.setVisibility(View.VISIBLE);
        holder.ivContent.setImageBitmap(bitmap);
        holder.ivContent.setTag(true);
        holder.ivContent.rotateAction(ori);
        holder.relativeLayout.setBackgroundColor(context.getResources().getColor(R.color.black));
    }

    private Bitmap getBitmap(String path)
    {
        //获取手机分辨率高宽
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int reqWidth = metrics.widthPixels > metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;
        int height = metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;

        Bitmap bitmap = null;
        if (FileUtil.isFileExist(path))
        {
            bitmap = decodeBitmap(path, reqWidth, height);
            return bitmap;
        }

        String mdmPath = EncryptUtil.getMdmPath(path);
        if (FileUtil.isFileExist(mdmPath))
        {
            bitmap = decodeBitmap(mdmPath, reqWidth, height);
        }
        return bitmap;
    }
    /**
     * 下载成功后刷新页面。
     *
     * @param holder
     * @param path
     */
    protected void refreshSuccess(ScanHolder holder, String path)
    {
        if (holder.loadLayout != null)
        {
            holder.loadLayout.setVisibility(View.GONE);
        }

        decodeBitmapForShow(path, holder);

        if (listener != null)
        {
            listener.onDownSuccess(holder.position);
        }
    }

    /**
     * 添加view到对应的ViewGroup，并保证顺序一致。
     * @param container 存储子view的parent对象。
     * @param child 子view
     * @param position
     */
    private void addView(ViewGroup container, View child, int position)
    {
        //这里保证顺序不错乱
        int size = container.getChildCount();
        int index = position;
        while (index > size)
        {
            index--;
        }
        container.addView(child, index);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        pagers.remove(position);

        if (debug)
        {
            Logger.debug(TAG, "container size = " + container.getChildCount());
            Logger.debug(TAG, "position = " + position + "/object = " + object);
        }

        View pictureScan = (View) object;
        ScanHolder holder = (ScanHolder)pictureScan.getTag();
        holder.destroy = true;

        container.removeView(pictureScan);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.ivContent.getDrawable();
        if (bitmapDrawable == null || bitmapDrawable.getBitmap() == null)
        {
            return;
        }

        //如果图片还未回收，先强制回收该图片
        holder.ivContent.setImageBitmap(null);

        Object tag = holder.ivContent.getTag();
        if (tag == null || !(tag instanceof Boolean))
        {
            return;
        }

        boolean mDelete = ((Boolean) tag).booleanValue();
        if(mDelete && !(bitmapDrawable.getBitmap().isRecycled()))
        {
            bitmapDrawable.getBitmap().recycle();
        }
    }

    @Override
    public int getCount()
    {
        return datas.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    @Override
    public SwitchPager currentPager(int location)
    {
        return pagers.get(location);
    }

    /**
     * 解析用来显示图片的bitmap
     *
     * @param path
     * @param holder
     */
    protected void decodeBitmapForShow(String path, ScanHolder holder)
    {
        if (path == null)
        {
            Logger.warn(TAG, "path is empty.");
            return;
        }

        //如果文件不存在，刷新界面显示。
        if (!(new File(path).exists()))
        {
            refreshFail(holder, true);
            return;
        }

        //获取手机分辨率高宽
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int reqWidth = metrics.widthPixels > metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;
        int height = metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;

        Bitmap bitmap = decodeBitmap(path, reqWidth, height);

        int ori = ExifOriUtil.getExifOrientation(path);
        holder.ivContent.setVisibility(View.VISIBLE);
        holder.ivContent.setImageBitmap(bitmap);
        holder.ivContent.setTag(true);
        holder.ivContent.rotateAction(ori);
    }

    /**
     * 刷新下载失败的界面显示。
     *
     * @param holder
     * @param deleted 是否已被删除。
     */
    protected void refreshFail(ScanHolder holder, boolean deleted)
    {
        if (holder.loadLayout == null)
        {
            return;
        }

        holder.ivContent.setVisibility(View.GONE);
        holder.loadLayout.setVisibility(View.VISIBLE);
        holder.loadLogo.setBackgroundResource(R.drawable.um_load_pic_fail);
        holder.loadTv.setText(context.getString(R.string.um_load_fail));
        holder.progressBar.setVisibility(View.GONE);
        if (deleted)
        {
            holder.loadBtn.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.loadBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 解析bitamp，由于bitmap经常会导致outofmemoryerror，每次出现OutOfMemoryError
     * 后，将获取的bitmap缩小4倍，再取。
     *
     * @param absolutePath
     * @param reqWidth
     * @param height
     * @return
     */
    private Bitmap decodeBitmap(String absolutePath, int reqWidth, int height)
    {
        Bitmap bitmap;
        try
        {
            bitmap = BitmapUtil.decodeBitmapFromFile(absolutePath, reqWidth, height);
        }
        catch (OutOfMemoryError error)
        {
            Logger.warn(TAG, error.toString());
            bitmap = decodeBitmap(absolutePath, reqWidth / 2, height / 2);
        }
        return bitmap;
    }

    /**
     * 游览的界面内容容器
     */
    protected static class ScanHolder
    {
    	//by lwx302895 start
    	public RelativeLayout relativeLayout;

    	public boolean destroy = false;
    	//by lwx302895 end

        public ZoomImageView ivContent;

        public SeekBar progressBar;

        public RelativeLayout loadLayout;

        public TextView loadTv;

        // public View rootView;

        public Button loadBtn;

        public ImageView loadLogo;

        public int position;

        public ProgressBar progress;
    }

    public static interface PictureListener
    {
        public void onDownSuccess(int position);

        void onClick();

        boolean onLongClick();
    }

    public void registerDownLoadListener(PictureListener listener)
    {
        this.listener = listener;
    }

    public void unRegisterDownloadListener()
    {
        this.listener = null;
    }
}
