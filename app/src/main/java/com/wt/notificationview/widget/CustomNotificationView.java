package com.wt.notificationview.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.wt.notificationview.R;


/**
 * Created by wt on 2016/12/11.
 * 自定义的通知栏
 */
public class CustomNotificationView extends RelativeLayout {

    private static final int SNAP_VELOCITY = 500;

    private static final int ANIM_TYPE_DOWN = 1;
    private static final int ANIM_TYPE_UP = 2;

    private static final int MSG_DO_ANIM_UP = 0x11;
    private static final String TAG = "CustomNotificationView";
    private ImageView mIcon;
    private TextView mTitle;
    private TextView mContent;
    private int mHeight;

    private Scroller mScroller;

    private VelocityTracker mVelocityTracker;

    private float lastMotionX;

    private int mTouchSlop;

    private boolean mNeedRecover;

    private boolean mIsRecover;

    private GestureDetectorCompat mGestureDetector;

    private long mDownAnimEndTime;//记录下滑动画完成的时间

    private long WAIT_TIME = 1000 * 4;//停留时间

    private ClickCallBack mCallBack;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_DO_ANIM_UP:
                    doAnimation(ANIM_TYPE_UP);
                    break;
            }
        }
    };

    public CustomNotificationView(Context context)
    {
        this(context , null);
    }

    public CustomNotificationView(Context context, AttributeSet attrs) {
        this(context, attrs , 0);
    }

    public CustomNotificationView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs)
    {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, new int[]{android.R.attr.layout_height});
        mHeight = typedArray.getDimensionPixelSize(typedArray.getIndex(0) , -1);
        typedArray.recycle();
        Log.d("wt" , "height == "+mHeight);

        mScroller = new Scroller(getContext());
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        mGestureDetector = new GestureDetectorCompat(getContext() , new MyGeusterListener());//处理点击事件
    }


    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        mIcon = (ImageView) findViewById(R.id.internal_notify_icon);
        mTitle = (TextView) findViewById(R.id.internal_notify_title);
        mContent = (TextView) findViewById(R.id.internal_notify_content);

    }

    /**
     *根据数据显示
     * @param content model
     */
    public void refreshByData(String content , ClickCallBack callBack)
    {
        if(TextUtils.isEmpty(content))
        {
            return;
        }

        if(null == mContent)
        {
            return;
        }

        this.mCallBack = callBack;

        mContent.setText(content);
        setVisibility(VISIBLE);
        doAnimation(ANIM_TYPE_DOWN);
    }


    private void doAnimation(int type)
    {
        mHandler.removeMessages(MSG_DO_ANIM_UP);
        switch (type)
        {
            case ANIM_TYPE_DOWN:
            {
                ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY", 0 - mHeight, 0);
                animator.setDuration(500);
//                animator.setInterpolator(new BounceInterpolator());//弹跳
                animator.addListener(new MyAnimationListener()
                {
                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        super.onAnimationEnd(animation);
                        mDownAnimEndTime = System.currentTimeMillis();
                        mHandler.sendEmptyMessageDelayed(MSG_DO_ANIM_UP , WAIT_TIME);
                    }
                });
                animator.start();
            }
            break;
            case ANIM_TYPE_UP:
            {
                ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY", 0, 0 - mHeight);
                animator.setDuration(500);
                animator.addListener(new MyAnimationListener()
                {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        scrollTo(0 , 0);
                    }
                });
                animator.start();
            }
            break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mGestureDetector.onTouchEvent(event);
        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        final int action = event.getAction();
        final float x = event.getX();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                mHandler.removeMessages(MSG_DO_ANIM_UP);//移除，上滑动画消息
                if (!mScroller.isFinished())
                {
//                    mScroller.abortAnimation();
                    //如果正在滑动则不可点击
                    return true;
                }
                lastMotionX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltax = (int) (lastMotionX - x);
                lastMotionX = x;
                scrollBy(deltax, 0);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = this.mVelocityTracker;
                // 计算当前的速度
                this.mVelocityTracker.computeCurrentVelocity(1000);
                // 获得X轴方向当前的速度
                int velocityX = (int) velocityTracker.getXVelocity();
                if (velocityX > SNAP_VELOCITY)
                {
                    slideToRight();
                }
                else if (velocityX < -SNAP_VELOCITY)
                {
                    slideToLeft();
                }
                else
                {
                    if(getScrollX() <= -getWidth() / 2)
                    {
                        slideToRight();
                    }
                    else if( getScrollX() >= getWidth() / 2)
                    {
                        slideToLeft();
                    }
                    else
                    {
                        sliderToRecover();
                    }
                }
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                break;
        }
        return true;
    }

    private void sliderToRecover()
    {
        mNeedRecover =true;
        mIsRecover = true;
        smoothScrollTo(0 , 0);
    }

    /**
     * 向左滑出
     */
    private void slideToLeft()
    {
        mNeedRecover = true;
        smoothScrollTo(getWidth() , 0);
    }

    /**
     * 向右滑出
     */
    private void slideToRight()
    {
        mNeedRecover =true;
        smoothScrollTo(-getWidth() , 0);
    }

    public void smoothScrollTo(int destX , int destY)
    {
        int scrollX = getScrollX();
        int deltaX = destX - scrollX;
        int scrollY = getScrollY();
        int deltaY =  destY - scrollY;
        mScroller.startScroll(scrollX, scrollY, deltaX , deltaY);
        invalidate();
    }

    @Override
    public void computeScroll()
    {
        if (mScroller.computeScrollOffset())
        {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
        else
        {
            if(mNeedRecover)
            {
                mNeedRecover = false;
                if(mIsRecover)
                {
                    mIsRecover = false;
                    long time = System.currentTimeMillis() - mDownAnimEndTime;
                    if(time < WAIT_TIME)
                    {
                        mHandler.sendEmptyMessageDelayed(MSG_DO_ANIM_UP ,WAIT_TIME - time);
                    }
                    else
                    {
                        mHandler.sendEmptyMessage(MSG_DO_ANIM_UP);
                    }
                }
                else
                {
                    mHandler.sendEmptyMessage(MSG_DO_ANIM_UP);
                }
            }
        }
    }

    public interface ClickCallBack
    {
        void clickCall();
    }

    /**
     * 动画期间不让点击
     */
    private class MyAnimationListener implements Animator.AnimatorListener
    {
        @Override
        public void onAnimationStart(Animator animation)
        {
            CustomNotificationView.this.setEnabled(false);
        }

        @Override
        public void onAnimationEnd(Animator animation)
        {
            CustomNotificationView.this.setEnabled(true);

        }

        @Override
        public void onAnimationCancel(Animator animation)
        {
            CustomNotificationView.this.setEnabled(true);
        }

        @Override
        public void onAnimationRepeat(Animator animation)
        {

        }
    }

    private class MyGeusterListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            Log.d(TAG , "onSingleTapUp , x == ");
            if(null != mCallBack)
            {
                mCallBack.clickCall();

                mHandler.removeMessages(MSG_DO_ANIM_UP);
//                mHandler.sendEmptyMessage(MSG_DO_ANIM_UP);
                CustomNotificationView.this.setVisibility(GONE);
            }
            return true;
        }

//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
//        {
//            Log.d(TAG , "onScroll , x == "+distanceX);
//            scrollBy((int)distanceX , 0);
//            return true;
//        }
//
//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
//        {
//            Log.d(TAG , "onFling :: "+"velocityX == "+velocityX + "e1.x == "+ e1.getX() + "e2.x == "+e2.getX());
//            if(velocityX > SNAP_VELOCITY)
//            {
//                slideToRight();
//            }
//            else if(velocityX < -SNAP_VELOCITY)
//            {
//                slideToLeft();
//            }
//            else
//            {
//                if(getScrollX() <= -getWidth() / 2)
//                {
//                    slideToRight();
//                }
//                else if( getScrollX() >= getWidth() / 2)
//                {
//                    slideToLeft();
//                }
//                else
//                {
//                    sliderToRecover();
//                }
//            }
//            return true;
//        }
    }

}
