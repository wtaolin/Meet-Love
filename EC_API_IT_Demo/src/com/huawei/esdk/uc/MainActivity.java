package com.huawei.esdk.uc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.huawei.esdk.uc.home_fragment.Fragment1;
import com.huawei.esdk.uc.home_fragment.Fragment2;
import com.huawei.esdk.uc.home_fragment.Fragment3;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity implements
        ViewPager.OnPageChangeListener, TabHost.OnTabChangeListener {

    private FragmentTabHost mTabHost;
    private LayoutInflater layoutInflater;
    private Class fragmentArray[] = { Fragment1.class, Fragment2.class, Fragment3.class };
    private int imageViewArray[] = { R.drawable.tab_setting_btn, R.drawable.tab_home_btn, R.drawable.tab_contact_btn};
    private String textViewArray[] = {"setting", "home", "contact"};
    private List<Fragment> list = new ArrayList<Fragment>();
    private ViewPager vp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        initView();//初始化控件
        initPage();//初始化页面
    }

    //    控件初始化控件
    private void initView() {
        vp = (ViewPager) findViewById(R.id.pager);

        /*实现OnPageChangeListener接口,目的是监听Tab选项卡的变化，然后通知ViewPager适配器切换界面*/
        /*简单来说,是为了让ViewPager滑动的时候能够带着底部菜单联动*/

        vp.addOnPageChangeListener(this);//设置页面切换时的监听器
        layoutInflater = LayoutInflater.from(this);//加载布局管理器

        /*实例化FragmentTabHost对象并进行绑定*/
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);//绑定tahost
        mTabHost.setup(this, getSupportFragmentManager(), R.id.pager);//绑定viewpager

        /*实现setOnTabChangedListener接口,目的是为监听界面切换），然后实现TabHost里面图片文字的选中状态切换*/
        /*简单来说,是为了当点击下面菜单时,上面的ViewPager能滑动到对应的Fragment*/
        mTabHost.setOnTabChangedListener(this);

        int count = imageViewArray.length;

        /*新建Tabspec选项卡并设置Tab菜单栏的内容和绑定对应的Fragment*/
        for (int i = 0; i < count; i++) {
            // 给每个Tab按钮设置标签、图标和文字
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(textViewArray[i])
                    .setIndicator(getTabItemView(i));
            // 将Tab按钮添加进Tab选项卡中，并绑定Fragment
            mTabHost.addTab(tabSpec, fragmentArray[i], null);
            mTabHost.setTag(i);
        }

//        mTabHost.getTabWidget().getChildAt(0)
//                .setBackgroundResource(R.drawable.tab_btn_selector_0);//设置Tab被选中的时候颜色改变
//        mTabHost.getTabWidget().getChildAt(1)
//                .setBackgroundResource(R.drawable.tab_btn_selector_1);//设置Tab被选中的时候颜色改变
//        mTabHost.getTabWidget().getChildAt(2)
//                .setBackgroundResource(R.drawable.tab_btn_selector_2);//设置Tab被选中的时候颜色改变
    }

    /*初始化Fragment*/
    private void initPage() {
        Fragment1 fragment1 = new Fragment1();
        Fragment2 fragment2 = new Fragment2();
        Fragment3 fragment3 = new Fragment3();

        list.add(fragment1);
        list.add(fragment2);
        list.add(fragment3);

        //绑定Fragment适配器
        vp.setAdapter(new MyFragmentAdapter(getSupportFragmentManager(), list));
        vp.setCurrentItem(1);
        mTabHost.getTabWidget().setDividerDrawable(null);
    }

    private View getTabItemView(int i) {
        View view;
        if(i==1){
            view = layoutInflater.inflate(R.layout.activity_tabbtn_2, null);
        }
        else {
            view = layoutInflater.inflate(R.layout.activity_tabbtn_1, null);
        }

        //将xml布局转换为view对象
        //利用view对象，找到布局中的组件,并设置内容，然后返回视图
        ImageView mImageView = (ImageView) view
                .findViewById(R.id.tab_imageview);
//        mImageView.setImageResource(imageViewArray[i]);
        switch (i){
            case(0):
                mImageView.setImageResource(R.drawable.tab_btn_selector_0);
                break;
            case(1):
                mImageView.setImageResource(R.drawable.tab_btn_selector_1);
                break;
            case(2):
                mImageView.setImageResource(R.drawable.tab_btn_selector_2);
                break;
        }
        return view;
    }


    @Override
    public void onPageScrollStateChanged(int arg0) {

    }//arg0 ==1的时候表示正在滑动，arg0==2的时候表示滑动完毕了，arg0==0的时候表示什么都没做，就是停在那。

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }//表示在前一个页面滑动到后一个页面的时候，在前一个页面滑动前调用的方法

    @Override
    public void onPageSelected(int arg0) {//arg0是表示你当前选中的页面位置Postion，这事件是在你页面跳转完毕的时候调用的。
        TabWidget widget = mTabHost.getTabWidget();
        int oldFocusability = widget.getDescendantFocusability();
        widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);//设置View覆盖子类控件而直接获得焦点
        mTabHost.setCurrentTab(arg0);//根据位置Postion设置当前的Tab
        widget.setDescendantFocusability(oldFocusability);//设置取消分割线

    }

    @Override
    public void onTabChanged(String tabId) {//Tab改变的时候调用
        int position = mTabHost.getCurrentTab();
        vp.setCurrentItem(position);//把选中的Tab的位置赋给适配器，让它控制页面切换
    }

}

