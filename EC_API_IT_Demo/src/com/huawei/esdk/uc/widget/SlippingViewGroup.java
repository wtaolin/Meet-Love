package com.huawei.esdk.uc.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * 类名称：SlidingGroupView
 * 类描述：可以平滑的一组视图
 */
public class SlippingViewGroup extends ViewGroup
{
    /**
     * 手滑动的角度,即斜率 (1.5 大约45°)
     */
    private final static float HAND_SLOPE = 1.5f;

    /**
     * 速率计算器
     */
    private VelocityTracker mVelocityTracker;
    private final Scroller mScroller;
    
    /**
     * 第一次界面layout时为true,以后都为false
     */
    private boolean mFirstLayout = true;
    
    /**
     * 当前页面索引.
     */
    private int mCurrentScreen = 0;
    
    /**
     * 比率,计算滑动边界宽度
     */
    private float rate = 0.3f;
    
    /**
     * 移动前的x, y坐标位置
     */
    private float mLastMotionX;
    private float mLastMotionY;
    
    
    private int mTouchSlop = 30;
    
    /**
     * 滑动最大速度
     */
    private int mMaximumVelocity = 4000;
    
    /**
     * 滑动最小速度
     */
    private int mMinVelocity = 50;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    private int mTouchState = TOUCH_STATE_REST;
    
    private int screenWidth = 480;
    
    public SlippingViewGroup(Context context)
    {
        this(context, null);
    }

    public SlippingViewGroup(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        final ViewConfiguration configuration = ViewConfiguration
                .get(getContext());
        
        //初始化一个最小滑动距离  
        mTouchSlop = configuration.getScaledTouchSlop(); 
        
        mMinVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        int childLeft = 0;
        final int count = getChildCount();
        View child;
        for (int i = 0; i < count; i++)
        {
            child = getChildAt(i);
            if (child.getVisibility() != View.GONE)
            {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth,
                        child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        
        final int action = event.getAction();
        
        final float x = event.getX();
        final float y = event.getY();
        
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:

                mLastMotionX = x;
                mLastMotionY = event.getY();
                
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when being
                 * flinged.
                 */
                if (mScroller.isFinished())
                {
                    mTouchState = TOUCH_STATE_REST;
                }
                else
                {
                    mTouchState = TOUCH_STATE_SCROLLING;
                    mScroller.abortAnimation();
                }
                
                break;
            case MotionEvent.ACTION_MOVE:
                final float xDiff = Math.abs(x - mLastMotionX);
                final float yDiff = Math.abs(y - mLastMotionY);
                
                if (mTouchState != TOUCH_STATE_SCROLLING 
                        && (xDiff > mTouchSlop) 
                        && (Math.abs(xDiff / (yDiff == 0 ? 1f : yDiff)) > HAND_SLOPE))
                {
                    mTouchState = TOUCH_STATE_SCROLLING;
                }
                
                if (mTouchState == TOUCH_STATE_SCROLLING)
                {
                    float deltaX =  mLastMotionX - x;
                    
                    //计算滑动到的位置
                    int mDelta = getAvailableToScroll(Math.round(deltaX - 0.5F));
                    scrollTo(mDelta, 0);
                    invalidate();
                }
                
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchState == TOUCH_STATE_SCROLLING)
                {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    
                    float velocityX = velocityTracker.getXVelocity();
                    if (velocityX > mMinVelocity && mCurrentScreen > 0)
                    {
                        // Fling hard enough to move left
                        snapToScreen(mCurrentScreen - 1);
                    }
                    else if (velocityX < -mMinVelocity
                            && mCurrentScreen < getChildCount() - 1)
                    {
                        // Fling hard enough to move right
                        snapToScreen(mCurrentScreen + 1);
                    }
                    else
                    {
                        snapToDestination();
                    }
                }
                
                if (mVelocityTracker != null)
                {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mTouchState = TOUCH_STATE_REST;
                break;
            default:
                break;
        }
        
        return super.dispatchTouchEvent(event);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return true;
    }
    
    /**
     * 获取有效的滑动距离
     * @param deltaX
     * @return
     */
    protected int getAvailableToScroll(int deltaX)
    {
        int width = getWidth();
        int screenIndex = mCurrentScreen;
        int availableToScroll = deltaX;
        
        if (screenIndex == getChildCount() - 1 && deltaX > 0)
        {
            availableToScroll = Math.min(deltaX, Math.round(width * rate));
        }
        else if (screenIndex == 0 && deltaX < 0)
        {
            availableToScroll = Math.max(deltaX, -Math.round(width * rate));
        }
        
        return availableToScroll + width * screenIndex;
    }
    
    /**
     * 方法描述: 父view需要重新布局时,调用onMeasure.
     * 备注: onMeasure is called when the parent View needs to calculate the layout. 
     *       Typically, onMeasure may be called several times depending on the 
     *       different children present and their layout parameters.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY)
        {
            throw new IllegalStateException("error mode.");
        }
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY)
        {
            throw new IllegalStateException("error mode.");
        }
        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++)
        {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        if (mFirstLayout)
        {
            scrollTo(mCurrentScreen * width, 0);
            mFirstLayout = false;
        }
    }

    /**  
     * According to the position of current layout  
     * scroll to the destination page.  
     */
    public void snapToDestination()
    {
        final int screenWidth = getWidth();
        final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
        snapToScreen(destScreen);
    }

    /**
     * 方法名称：snapToScreen
     * 方法描述：移动到指定的屏幕
     * 输入参数：@param destScreen 
     * 返回类型：void
     * 备注：
     */
    public void snapToScreen(int whichScreen)
    {
        if (!mScroller.isFinished())
        {
            mScroller.abortAnimation();
        }
        
        screenWidth = getWidth();
        // get the valid layout page    
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        if (getScrollX() != (whichScreen * screenWidth))
        {
            mCurrentScreen = whichScreen;
            final int delta = whichScreen * screenWidth - getScrollX();
            
            mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta));
            
            invalidate(); // Redraw the layout    
            getChildAt(whichScreen).scrollTo(0, 0);
        }
    }

    /**
     * 执行scroll后计算移动屏幕的位移和重新绘制屏幕
     * {@inheritDoc}
     */
    @Override
    public void computeScroll()
    {
        if (mScroller.computeScrollOffset()) //计算新的位置,如果动画还没结束,返回true
        {
            scrollTo(mScroller.getCurrX(), 0);
            postInvalidate();
        }
    }

    /**
     * 方法名称：getmCurrentScreen
     * 方法描述：获取mCurrentScreen
     * 返回类型：@return the mCurrentScreen
     * 备注：
     */
    public int getmCurrentScreen()
    {
        return mCurrentScreen;
    }

    /**
     * 方法名称：setmCurrentScreen
     * 方法描述：设置当前页面的索引值.如果不是第一次执行布局,则跳到此页.
     * @param mCurrentScreen the mCurrentScreen to set
     * 返回类型：@return void 
     * 备注：
     */
    public void setmCurrentScreen(int mCurrentScreen)
    {
        this.mCurrentScreen = mCurrentScreen;
        if (!mFirstLayout)
        {
            snapToScreen(mCurrentScreen);
        }
    }

    public float getRate()
    {
        return rate;
    }

    public void setRate(float rate)
    {
        this.rate = rate;
    }
}
