// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2018 Opera Software AS. All rights reserved.
//
// This file is an original work developed by Opera Software AS

package com.ljr.com.multi.paging;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

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
            } else if (velocityX > 0 && mCurrentPageNumber < mPageCount - 1) {
                mCurrentPageNumber++;
            }
            int toX = getTotalOffsetX();
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
                mAnimator.addListener(createAnimatorListenerAdapter());
            } else {
                mAnimator.cancel();
                mAnimator.removeAllListeners();
                mAnimator.addListener(createAnimatorListenerAdapter());
                mAnimator.setIntValues(fromX, toX);
            }
            mAnimator.start();
            return true;
        }

        private AnimatorListenerAdapter createAnimatorListenerAdapter() {
            return new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mOnPageChangeListener != null) {
                        mOnPageChangeListener.onPageChange(getOffsetPageCount());
                    }
                    mOffsetX = 0;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mOffsetX = 0;
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
                    mRecyclerViewWidth == 0 ? mRecyclerView.getWidth() : mRecyclerViewWidth;
            if (mRecyclerViewWidth != mRecyclerView.getWidth()) {
                mRecyclerViewWidth = mRecyclerView.getWidth();
                mTotalOffsetX = getTotalOffsetX();
                mOffsetX = 0;
            }
            mTotalOffsetX += dx;
            mOffsetX += dx;
        }
    }

    @NonNull
    private final RecyclerView mRecyclerView;
    @Nullable
    private ValueAnimator mAnimator;
    private int mTotalOffsetX;
    private int mOffsetX;
    private int mCurrentPageNumber;
    private int mPageCount;
    private int mScrollState;
    private int mRecyclerViewWidth;
    @Nullable
    private OnPageChangeListener mOnPageChangeListener;
    @NonNull
    private final PagerFlingListener mOnFlingListener;

    private HorizontalPagerScrollHelper(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mOnFlingListener = new PagerFlingListener();
        mRecyclerView.setOnFlingListener(mOnFlingListener);
        mRecyclerView.addOnScrollListener(new GridPagerScrollListener());
    }

    private int getOffsetPageCount() {
        return mTotalOffsetX / mRecyclerView.getWidth();
    }

    public void setOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    public static HorizontalPagerScrollHelper createGridPagerScrollHelper(
            @NonNull RecyclerView recyclerView) {
        return new HorizontalPagerScrollHelper(recyclerView);
    }

    public void reinitialization(int pageCount) {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mOffsetX = 0;
        mCurrentPageNumber = 0;
        mTotalOffsetX = 0;
        mPageCount = pageCount;
    }

    private int getVelocityX() {
        int velocityX = 0;
        int dx = Math.abs(mOffsetX);
        boolean changed = dx > mRecyclerView.getWidth() / 3;
        if (changed) {
            velocityX = mOffsetX < 0 ? -1 : 1;
        }
        return velocityX;
    }

    private int getTotalOffsetX() {
        return mCurrentPageNumber * mRecyclerView.getWidth();
    }
}