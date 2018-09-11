// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2018 Opera Software AS. All rights reserved.
//
// This file is an original work developed by Opera Software AS

package com.ljr.com.multi.paging;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * It helps that the RecyclerView implements scrolling and fling automatically
 */
public class HorizontalPagerScrollHelper {
    private static final int FLING_ANIMATOR_DURATION = (int) TimeUnit.MILLISECONDS.toMillis(300);

    /**
     * It is used to monitor switching page event
     */
    public interface OnPageChangeListener {
        void onPageChange(int index);
    }

    private class PagerFlingListener extends RecyclerView.OnFlingListener {
        @Override
        public boolean onFling(int velocityX, int velocityY) {
            if (mScrollState != RecyclerView.SCROLL_STATE_IDLE) {
                velocityX = getVelocityX();
            }
            final int fromX = mTotalOffsetX;
            /*
             * It may be invoked because of scrolling or switching pages.
             * When velocityX isn't equal zero, onFling is invoked by scrolling.
             * On the contrary, it is invoked by switching.
             * The value of mCurrentPageNumber is from 0 to pageCount - 1.
             */
            if (velocityX < 0 && mCurrentPageNumber > 0) {
                mCurrentPageNumber--;
            } else if (velocityX > 0 && mCurrentPageNumber < mMaxPage) {
                mCurrentPageNumber++;
            }
            int toX = getTotalOffsetX();
            toX = velocityX == 0 && mOrientation < 0 ?
                    (int) (toX - getWidth() * mWidthIncrementRate) : toX;
            if (mAnimator == null) {
                mAnimator = ValueAnimator.ofInt(fromX, toX);
                mAnimator.setDuration(FLING_ANIMATOR_DURATION);
                mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int scrolledX = (int) animation.getAnimatedValue();
                        mRecyclerView.scrollBy(scrolledX - mTotalOffsetX, 0);
                    }
                });
                mAnimator.addListener(createAnimatorListenerAdapter(velocityX));
            } else {
                mAnimator.cancel();
                mAnimator.removeAllListeners();
                mAnimator.addListener(createAnimatorListenerAdapter(velocityX));
                mAnimator.setIntValues(fromX, toX);
            }
            mAnimator.start();
            return true;
        }

        private AnimatorListenerAdapter createAnimatorListenerAdapter(final int velocityX) {
            return new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mOnPageChangeListener != null) {
                        mOnPageChangeListener.onPageChange(getOffsetPageCount());
                    }
                    mOffsetX = velocityX == 0 && mOrientation < 0 ?
                            (int) (getWidth() * mWidthIncrementRate * -1) : 0;
                    mLastOrientation = velocityX == 0 && mOrientation < 0 ? mOrientation : 0;
                    mOrientation = 0;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mOffsetX = 0;
                    mOrientation = 0;
                }
            };
        }
    }

    private class GridPagerScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            mScrollState = newState;
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mOnFlingListener.onFling(getVelocityX(), 0);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            mRecyclerViewWidth =
                    mRecyclerViewWidth == 0 ? getWidth() : mRecyclerViewWidth;
            if (mRecyclerViewWidth != getWidth()) {
                mRecyclerViewWidth = getWidth();
                mTotalOffsetX = getTotalOffsetX();
                mOffsetX = 0;
            }
            mTotalOffsetX += dx;
            mOffsetX += dx;
            mOrientation = dx < 0 ? -1 : 1;
        }
    }

    private final RecyclerView mRecyclerView;
    private ValueAnimator mAnimator;
    private int mTotalOffsetX;
    private int mOffsetX;
    private int mCurrentPageNumber;
    private int mPageCount;
    private int mScrollState;
    private int mRecyclerViewWidth;
    private OnPageChangeListener mOnPageChangeListener;
    private final PagerFlingListener mOnFlingListener;
    private int mMaxPage;
    private Context mContext;
    private int mOrientation;
    private int mLastOrientation;
    private final int mScaledTouchSlop;
    private float mWidthIncrementRate;

    private HorizontalPagerScrollHelper(RecyclerView recyclerView) {
        Check.isNotNull(recyclerView);
        mRecyclerView = recyclerView;
        mOnFlingListener = new PagerFlingListener();
        mRecyclerView.setOnFlingListener(mOnFlingListener);
        mRecyclerView.addOnScrollListener(new GridPagerScrollListener());
        mContext = mRecyclerView.getContext();
        mScaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop() * 2;
    }

    private int getOffsetPageCount() {
        return mTotalOffsetX / getWidth();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    public static HorizontalPagerScrollHelper createGridPagerScrollHelper(
            RecyclerView recyclerView) {
        return new HorizontalPagerScrollHelper(recyclerView);
    }

    public void reinitialization(int pageCount, int currentPage) {
        if (mPageCount == pageCount) return;
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mOffsetX = 0;
        mCurrentPageNumber = currentPage;
        mPageCount = pageCount;
        mMaxPage = pageCount > 0 && pageCount == currentPage ? mPageCount : mPageCount - 1;
        mTotalOffsetX = isLayoutRtl() ? mCurrentPageNumber * getWidth() : 0;
        mOrientation = 0;
        mLastOrientation = 0;
    }

    private int getVelocityX() {
        int velocityX = 0;
        int dx = Math.abs(mOffsetX);
        boolean changed = mOrientation < 0 ? dx > getWidth() / 3
                : (dx > mScaledTouchSlop + (mLastOrientation < 0 ? getIncrementWidth() : 0));
        if (changed) {
            velocityX = mOffsetX < 0 ? -1 : 1;
        }
        mLastOrientation = 0;
        return velocityX;
    }

    private int getTotalOffsetX() {
        return mCurrentPageNumber * getWidth();
    }

    private boolean isLayoutRtl() {
        return UIUtils.isLayoutRtl(mContext);
    }

    private int getWidth() {
        return mRecyclerView.getWidth() + getIncrementWidth();
    }

    private int getIncrementWidth() {
        return (int) (mRecyclerView.getWidth() * mWidthIncrementRate);
    }

    public void setWidthIncrementRate(float widthIncrementRate) {
        mWidthIncrementRate = widthIncrementRate;
    }
}