package com.example.jimbraypull;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

public class PullView extends LinearLayout {
//	private static final String TAG = "PullView";
	
	private static final byte STATE_CLOSE = 0;
	private static final byte STATE_OPEN = STATE_CLOSE + 1;
	private static final byte STATE_OVER = STATE_OPEN + 1;
	private static final byte STATE_OPEN_RELEASE = STATE_OVER + 1;
	private static final byte STATE_OVER_RELEASE = STATE_OPEN_RELEASE + 1;
	private static final byte STATE_REFRESH = STATE_OVER_RELEASE + 1;
	private static final byte STATE_REFRESH_RELEASE = STATE_REFRESH + 1;
	private byte mState;
	
	private View mLoadingView,mHintView;
	
	private int mMargin, mMaxMargin;
	private boolean mEnablePull;
	private int mLastY;
	
	private GestureDetector mGestureDetector;
	private FlingBack mFlingBack;
	
	private boolean mIsPointUp;
	private Animation mTurnUpAnimation;
	private Animation mTurnDonwAnimation;
	
	private RefreshListener mRefreshListener;

	/**
	 * @param context
	 * @param attrs
	 */
	public PullView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(LinearLayout.VERTICAL);
		
		mGestureDetector = new GestureDetector(mGestureListener);
		mFlingBack = new FlingBack();
		init();
		loadAnimation();
	}
	
	/**
	 * 初始化布局
	 */
	private void init() {
		View headView = inflate(getContext(), R.layout.layout_header, null);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
				(RelativeLayout.LayoutParams.FILL_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		addView(headView, lp);
		headView.measure(0, 0);
		mMargin = mMaxMargin = headView.getMeasuredHeight();
		
		mLoadingView = headView.findViewById(R.id.layout_loading);
		mHintView = headView.findViewById(R.id.layout_hint);
	}
	
	
	/**
	 * 加载动画
	 */
	private void loadAnimation() {
		mTurnUpAnimation = new RotateAnimation(0, -180,
						RotateAnimation.RELATIVE_TO_SELF, 0.5f,
						RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mTurnUpAnimation.setInterpolator(new LinearInterpolator());
		mTurnUpAnimation.setDuration(250);
		mTurnUpAnimation.setAnimationListener(mAnimationListener);
		
		mTurnDonwAnimation = new RotateAnimation(0, 180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		mTurnDonwAnimation.setInterpolator(new LinearInterpolator());
		mTurnDonwAnimation.setDuration(250);
		mTurnDonwAnimation.setAnimationListener(mAnimationListener);
		
		mIsPointUp = false;
	}
	
	private AnimationListener mAnimationListener = new AnimationListener() {
		
		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
			ImageView indicator = (ImageView) getChildAt(0).findViewById(R.id.iv_point);
			if(animation == mTurnUpAnimation) {
				indicator.setImageResource(R.drawable.pull_arrow_up);
			} else {
				indicator.setImageResource(R.drawable.pull_arrow_down);
			}
		}
	};
	
	
	/* (non-Javadoc)
	 * @see android.widget.LinearLayout#onLayout(boolean, int, int, int, int)
	 * 初始化 布局（显示或隐藏 下拉头部）
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		View head = getChildAt(0);
		View contentView = getChildAt(1);
		
		int contentY = contentView.getTop();
		if(mState == STATE_REFRESH) {
			head.layout(l, 0, r, mMargin);
			contentView.layout(l, mMargin, r, getHeight());
		} else {
			head.layout(l, contentY - mMargin, r, contentY);
			contentView.layout(l, contentY, r, getHeight());
		}
		
		View otherView = null;
		for(int i = 2; i < getChildCount(); i++) {
			otherView = getChildAt(i);
			otherView.layout(l, t, r, b);
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(!mEnablePull) {
			return super.dispatchTouchEvent(ev);
		}
		if(!mFlingBack.isFinished()) {
			return false;
		}
		
		View headView = getChildAt(0);
		if(ev.getAction() == MotionEvent.ACTION_UP) {
			if(headView.getBottom() > 0) {
				if(mState == STATE_REFRESH && headView.getBottom() > mMargin) {
					release(headView.getBottom() - mMaxMargin);
					return false;
				} else if(mState != STATE_REFRESH){
					release(headView.getBottom());
					return false;
				}
			}
		}
		
		boolean bool = mGestureDetector.onTouchEvent(ev);
		if(bool || (mState != STATE_CLOSE && mState != STATE_REFRESH)) {
			ev.setAction(MotionEvent.ACTION_CANCEL);
			return super.dispatchTouchEvent(ev);
		}
		
		if(bool) {
			return true;
		} else {
			return super.dispatchTouchEvent(ev);
		}
	}
	
	/**
	 * @param dis
	 * 松手时的操作
	 */
	private void release(int dis) {
		if(mRefreshListener != null && dis > mMaxMargin) {
			mState = STATE_OVER_RELEASE;
			mFlingBack.recover(dis - mMaxMargin);
		} else {
			mState = STATE_OPEN_RELEASE;
			mFlingBack.recover(dis);
		}
	}
	
	/**
	 * @param isEnable
	 * 指定一个标记量，用作设置 此view是否可拉动
	 */
	public void setEnablePull(boolean isEnable) {
		mEnablePull = isEnable;
	}
	
	private OnGestureListener mGestureListener = new OnGestureListener() {
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		/* (non-Javadoc)
		 * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
		 * 滑动的处理
		 */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY) {
			View headView = getChildAt(0);
			View contentView = getChildAt(1);
			
			if(contentView instanceof AdapterView<?>) {
				if(((AdapterView<?>)contentView).getFirstVisiblePosition() != 0  || 
						(((AdapterView<?>) contentView).getFirstVisiblePosition() == 0 && 
						((AdapterView<?>) contentView).getChildAt(0) != null && 
						((AdapterView<?>) contentView).getChildAt(0).getTop() < 0)) {
					return false;
				}
			}
			
			if(contentView.getTop() <= 0 && distanceY > 0 || (headView.getTop() > 0 && distanceY > 0 && mState == STATE_REFRESH)) {
				return false;
			}
			
			int speed = mLastY;
			if(headView.getTop() >= 0) {
				speed = mLastY / 2;
			}
			
			boolean bool = moveDown(speed, true);
			mLastY = (int) -distanceY;
			return bool;
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
	};
	
	/**
	 * @param dis
	 * @param changeState
	 * @return
	 * 真正移动view 的操作
	 */
	private boolean moveDown(int dis, boolean changeState) {
		View headView = getChildAt(0);
		View contentView = getChildAt(1);
		
		int contentTop = contentView.getTop() + dis;
		
		if(contentTop <= 0) {
			if(contentTop < 0) {
				dis = -contentView.getTop();
			} 
			headView.offsetTopAndBottom(dis);
			contentView.offsetTopAndBottom(dis);
			if(mState != STATE_REFRESH) {
				mState = STATE_CLOSE;
			}
		} else if(contentTop <= mMaxMargin) {
			if(mIsPointUp) {
				playAnimUp(false);
			}
			headView.offsetTopAndBottom(dis);
			contentView.offsetTopAndBottom(dis);
			if(changeState && mState != STATE_REFRESH) {
				mState = STATE_OPEN;
			} else if(mState == STATE_OVER_RELEASE) {
				refresh();
			}
		} else if(mState != STATE_REFRESH) {
			if(!mIsPointUp) {
				playAnimUp(true);
			}
			headView.offsetTopAndBottom(dis);
			contentView.offsetTopAndBottom(dis);
			if(changeState) {
				mState = STATE_OVER;
			}
		}
		mMargin = contentView.getTop() - headView.getTop();
		invalidate();
		return true;
	} 
	
	private void playAnimUp(boolean isUp) {
		ImageView indicator = (ImageView) getChildAt(0).findViewById(R.id.iv_point);
		TextView tvRefreshHint = (TextView) getChildAt(0).findViewById(R.id.tv_refersh_hint);
		if(isUp) {
			mIsPointUp = true;
			tvRefreshHint.setText(R.string.release_to_refresh);
			indicator.startAnimation(mTurnUpAnimation);
		} else {
			mIsPointUp = false;
			tvRefreshHint.setText(R.string.pull_to_refresh);
			indicator.startAnimation(mTurnDonwAnimation);
		}
	}
	
	/**
	
	* @ClassName: FlingBack
	
	* @Description: 
	
	* @date 2013-4-15 上午9:29:09
	* 用作下拉放手后的回弹
	
	*/
	private class FlingBack implements Runnable {
		private Scroller mScroller;
		private int mLastY;
		private boolean mIsFinished;
		
		public FlingBack() {
			mScroller = new Scroller(getContext());
			mIsFinished = true;
		}
		
		@Override
		public void run() {
			boolean bool = mScroller.computeScrollOffset();
			if(bool) {
				moveDown(mLastY - mScroller.getCurrY(), false);
				mLastY = mScroller.getCurrY();
				post(this);
			} else {
				mIsFinished = true;
				removeCallbacks(this);
			}
		}
		
		/**
		 * @param dis
		 * 使用Scroller进行回弹
		 */
		public void recover(int dis) {
			if(dis < 0) {
				return ;
			}
			removeCallbacks(this);
			mLastY = 0;
			mIsFinished = false;
			mScroller.startScroll(0, 0, 0, dis, 300);
			post(this);
		}
		
		public boolean isFinished() {
			return mIsFinished;
		}
		
	}
	
	private void refresh() {
		if (mRefreshListener != null) {
			mState = STATE_REFRESH;
			mHintView.setVisibility(INVISIBLE);
			mLoadingView.setVisibility(VISIBLE);
			mRefreshListener.onRefresh();
		}
	}
	
	public void refreshFinished() {
		View headView = getChildAt(0);
		mHintView.setVisibility(View.VISIBLE);
		mLoadingView.setVisibility(View.INVISIBLE);
		if(headView.getBottom() > 0) {
			mState = STATE_REFRESH_RELEASE;
			release(headView.getBottom());
		} else {
			mState = STATE_CLOSE;
		}
	}
	
	/**
	 * @param refreshListener
	 * 与外部Activity进行交互的listener
	 */
	public void setRefreshListener(RefreshListener refreshListener) {
		mRefreshListener = refreshListener;
	}
	
	public interface RefreshListener {
		public void onRefresh();
	}

}
