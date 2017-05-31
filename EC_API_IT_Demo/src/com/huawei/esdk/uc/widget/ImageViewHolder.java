package com.huawei.esdk.uc.widget;

import com.huawei.esdk.uc.R;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by h00203586 on 2014/9/19.
 */
public class ImageViewHolder
{
    public ImageView defaultImage;
    public ImageView background;
    public ImageView imageView;
    public ImageView takePhotoImage;
    public ImageView checkBoxImage;

    public ImageView videoPlay;

    public RelativeLayout selectLayout;

   public ImageViewHolder(View convertView)
   {
       imageView = (ImageView)convertView.findViewById(R.id.img_view);
       takePhotoImage = (ImageView)convertView.findViewById(R.id.take_photo);
       checkBoxImage = (ImageView)convertView.findViewById(R.id.select);
       selectLayout = (RelativeLayout)convertView.findViewById(R.id.select_layout);
       background = (ImageView)convertView.findViewById(R.id.select_img_background);
       videoPlay = (ImageView)convertView.findViewById(R.id.video_tip);
       defaultImage = (ImageView)convertView.findViewById(R.id.defaultImage);
   }
}
