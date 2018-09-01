// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2018 Opera Software AS. All rights reserved.
//
// This file is an original work developed by Opera Software AS

package com.ljr.com.multi.paging;

import android.content.Context;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * It implements grouping and paging of layout manager
 */
public class GridPagerLayoutManager extends RecyclerView.LayoutManager {

    public interface OnCompleteLayout {
        void onCompleteLayout(boolean refresh, int pageSize);
    }

    private class CurrentPageInfo {
        private int mCurrentPageLeft;
        private int mCurrentRowTotalWidth;
        private int mCurrentPageTop;
        private int mRow;
        private int mCurrentPage;
        private int mHorizontalOffset;
        private int mStartPos;
        private int mLastPos;

        private void reset() {
            mCurrentPageLeft = isRevertLayout() ? getWidth() - getPaddingRight() : getPaddingLeft();
            mRow = 0;
            mCurrentPageTop = 0;
            mHorizontalOffset = 0;
            mCurrentPage = 1;
            mStartPos = -1;
            mLastPos = -1;
            mCurrentRowTotalWidth = 0;
        }
    }

    private class LayoutState {
        private int mTotalRow;
        private int mItemHeight;
        private int mTotalPageSize;
        private int mVerticalPadding;

        void reset() {
            mTotalPageSize = 1;
            mVerticalPadding = 0;
            mItemHeight = 0;
            mTotalRow = 0;
        }
    }

    private class AnchorInfo {
        private int mWidth;
        private int mTargetPage;

        private boolean isConfigurationChanged() {
            return mWidth != getWidth();
        }

        private void reset() {
            mWidth = 0;
            mTargetPage = 0;
        }

        private int getTargetLeftOffset() {
            return mWidth * mTargetPage;
        }
    }

    private class Location {
        private final int mLeft;
        private final int mRight;
        private final int mTop;
        private final int mBottom;
        private final int mPageIndex;
        private final int mPosition;

        Location(int left, int top, int right, int bottom, int pageIndex, int position) {
            mLeft = left;
            mRight = right;
            mTop = top;
            mBottom = bottom;
            mPageIndex = pageIndex;
            mPosition = position;
        }

    }

    // The horizontal scrolled size
    private int mOffsetX;
    private int mTotalSpace;
    private boolean mRecycleChildrenOnDetach;
    private final OrientationHelper mOrientationHelper;
    private final LayoutState mLayoutState;
    private final CurrentPageInfo mCurrentPageInfo;
    private OnCompleteLayout mOnCompleteLayout;
    private final AnchorInfo mAnchorInfo;
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mRowHeight;
    private int mColumnWidth;
    private int mColumn;
    private int mRow = 1;
    private List<Location> mLocations = new ArrayList<>(0);
    private Context mContext;

    public GridPagerLayoutManager(Context context) {
        mOrientationHelper =
                OrientationHelper.createOrientationHelper(this, OrientationHelper.HORIZONTAL);
        mLayoutState = new LayoutState();
        mCurrentPageInfo = new CurrentPageInfo();
        mAnchorInfo = new AnchorInfo();
        mContext = context;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        mOffsetX = 0;
        mLayoutState.reset();
        mCurrentPageInfo.reset();
        mAnchorInfo.reset();
        if (mAnchorInfo.isConfigurationChanged()) {
            mAnchorInfo.mWidth = mOrientationHelper.getEnd();
        }
        detachAndScrapAttachedViews(recycler);
        fill(recycler, state);
        mLayoutState.mTotalPageSize = mCurrentPageInfo.mCurrentPage;
        mTotalSpace = (mLayoutState.mTotalPageSize - 1) * getWidth();
        if (mAnchorInfo.mTargetPage == 0) {
            mAnchorInfo.mTargetPage = isRevertLayout() ? mLayoutState.mTotalPageSize : 0;
        }
        if (mOnCompleteLayout != null) {
            mOnCompleteLayout
                    .onCompleteLayout(mAnchorInfo.mTargetPage == 0
                                    || mAnchorInfo.mTargetPage == mLayoutState.mTotalPageSize,
                            mLayoutState.mTotalPageSize);
        }
        mCurrentPageInfo.reset();
        if (isRevertLayout()) {
            mOffsetX = mAnchorInfo.getTargetLeftOffset() - getWidth();
            offsetChildrenHorizontal(-mOffsetX);
        } else {
            mOffsetX = mAnchorInfo.getTargetLeftOffset();
        }
    }

    private synchronized void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            return;
        }
        if (isRevertLayout()) {
            revertLayoutChunk(recycler);
        } else {
            layoutChunk(recycler);
        }

    }

    private void layoutChunk(RecyclerView.Recycler recycler) {
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            layoutChildItem(i, recycler.getViewForPosition(i));
        }
    }

    private void revertLayoutChunk(RecyclerView.Recycler recycler) {
        int count = getItemCount();
        mLocations.clear();
        for (int i = 0; i < count; i++) {
            computeLayoutChildItem(i, recycler.getViewForPosition(i));
        }
        for (int i = count - 1; i >= 0; i--) {
            Location location = mLocations.get(i);
            View itemView = recycler.getViewForPosition(location.mPosition);
            measureChildWithMargins(itemView, 0, 0);
            addView(itemView);
            int left = location.mLeft - (location.mPageIndex - 1) * getWidth();
            int right = location.mRight - (location.mPageIndex - 1) * getWidth();
            left += (mCurrentPageInfo.mCurrentPage - location.mPageIndex) * getWidth();
            right += (mCurrentPageInfo.mCurrentPage - location.mPageIndex) * getWidth();
            layoutDecoratedWithMargins(itemView, left, location.mTop, right, location.mBottom);
        }
        mLocations.clear();
    }

    private void computeLayoutChildItem(int pos, View itemView) {
        measureChildWithMargins(itemView, 0, 0);
        if (mLayoutState.mItemHeight == 0) {
            mLayoutState.mItemHeight =
                    mOrientationHelper.getDecoratedMeasurementInOther(itemView);
            if (mLayoutState.mItemHeight != 0) {
                mLayoutState.mTotalRow = getHeight() / mLayoutState.mItemHeight;
                mLayoutState.mVerticalPadding =
                        getHeight() - mLayoutState.mTotalRow * mLayoutState.mItemHeight;
            }
            mCurrentPageInfo.mHorizontalOffset = mCurrentPageInfo.mCurrentPageLeft;
        }
        resetCurrentPageIndex(pos);
        final int itemWidth = mColumnWidth > 0 ? mColumnWidth :
                mOrientationHelper.getDecoratedMeasurement(itemView);
        int right = mCurrentPageInfo.mCurrentPageLeft;
        int left = right - itemWidth;
        mCurrentPageInfo.mCurrentRowTotalWidth += itemWidth;
        if (exceedRealWidth(mCurrentPageInfo.mCurrentRowTotalWidth)) {
            if (exceedBottom(mLayoutState.mItemHeight, mCurrentPageInfo.mRow)) {
                resetCurrentPageInfo();
            } else {
                addRow();
            }
            right = mCurrentPageInfo.mHorizontalOffset;
            left = right - itemWidth;
            mCurrentPageInfo.mCurrentRowTotalWidth = itemWidth;
        }
        mCurrentPageInfo.mCurrentPageLeft = left;
        final int top = mCurrentPageInfo.mCurrentPageTop;
        final int bottom = top + mLayoutState.mItemHeight;
        // Scrolled to the target page, So it is need to use the target page as the start point
        // for correcting the offset
        left -= mAnchorInfo.getTargetLeftOffset();
        right -= mAnchorInfo.getTargetLeftOffset();
        mLocations.add(new Location(left, top, right, bottom, mCurrentPageInfo.mCurrentPage, pos));
    }

    private void layoutChildItem(int pos, View itemView) {
        measureChildWithMargins(itemView, 0, 0);
        addView(itemView);
        if (mLayoutState.mItemHeight == 0) {
            mLayoutState.mItemHeight =
                    mOrientationHelper.getDecoratedMeasurementInOther(itemView);
            if (mLayoutState.mItemHeight != 0) {
                mLayoutState.mTotalRow = getHeight() / mLayoutState.mItemHeight;
                mLayoutState.mVerticalPadding =
                        getHeight() - mLayoutState.mTotalRow * mLayoutState.mItemHeight;
            }
            mCurrentPageInfo.mHorizontalOffset = mCurrentPageInfo.mCurrentPageLeft;
        }
        resetCurrentPageIndex(pos);
        final int itemWidth = mColumnWidth > 0 ? mColumnWidth :
                mOrientationHelper.getDecoratedMeasurement(itemView);
        int left = mCurrentPageInfo.mCurrentPageLeft;
        int right = left + itemWidth;
        if (exceedRightEdge(right, mCurrentPageInfo.mCurrentPage)) {
            if (exceedBottom(mLayoutState.mItemHeight, mCurrentPageInfo.mRow)) {
                resetCurrentPageInfo();
            } else {
                addRow();
            }
            left = mCurrentPageInfo.mHorizontalOffset;
            right = left + itemWidth;
        }
        mCurrentPageInfo.mCurrentPageLeft = right;
        final int top = mCurrentPageInfo.mCurrentPageTop;
        final int bottom = top + mLayoutState.mItemHeight;
        left -= mAnchorInfo.getTargetLeftOffset();
        right -= mAnchorInfo.getTargetLeftOffset();
        layoutDecoratedWithMargins(itemView, left, top, right, bottom);
    }

    private void resetCurrentPageIndex(int pos) {
        if (mCurrentPageInfo.mStartPos == -1) {
            mCurrentPageInfo.mStartPos = pos;
        } else {
            mCurrentPageInfo.mLastPos = pos;
        }
    }

    private void addRow() {
        mCurrentPageInfo.mRow += 1;
        mCurrentPageInfo.mCurrentPageTop += mLayoutState.mItemHeight;
    }

    private void resetCurrentPageInfo() {
        mCurrentPageInfo.mHorizontalOffset += getWidth();
        mCurrentPageInfo.mCurrentPage += 1;
        mCurrentPageInfo.mRow = 0;
        mCurrentPageInfo.mCurrentPageTop = 0;
        mCurrentPageInfo.mStartPos = mCurrentPageInfo.mLastPos;
    }

    private boolean exceedRightEdge(int right, int currentPage) {
        return right + getPaddingRight() > currentPage * getWidth();
    }

    private boolean exceedRealWidth(int width) {
        return width > getRealWidth();
    }

    private boolean exceedBottom(int itemHeightUsed, int row) {
        return (row + 1) * itemHeightUsed + mLayoutState.mVerticalPadding >= getHeight();
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        if (mRecycleChildrenOnDetach) {
            removeAndRecycleAllViews(recycler);
            recycler.clear();
        }
        mOffsetX = 0;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        int distance = mOffsetX + dx;
        int result = dx;
        /*
         * From left to right, if distance is larger than mTotalSpace, it means
         * that it will scroll over the right edge of the last page. So it is need
         * to correct the value of result. From right to left, if distance is negative,
         * it means it will scroll over the left edge of the first page. Of course, it
         * is also need to correct the value of result.
         */
        if (distance > mTotalSpace) {
            result = mTotalSpace - mOffsetX;
        } else if (distance < 0) {
            result = 0 - mOffsetX;
        }
        mOffsetX += result;
        mAnchorInfo.mTargetPage = mOffsetX / getWidth();
        offsetChildrenHorizontal(-result);
        return result;
    }


    /**
     * Set whether LayoutManager will recycle its children when it is detached from
     * RecyclerView.
     * <p>
     * If you are using a {@link RecyclerView.RecycledViewPool}, it might be a good idea to set
     * this flag to <code>true</code> so that views will be available to other RecyclerViews
     * immediately.
     * <p>
     * Note that, setting this flag will result in a performance drop if RecyclerView
     * is restored.
     * @param recycleChildrenOnDetach Whether children should be recycled in detach or not.
     */
    public void setRecycleChildrenOnDetach(boolean recycleChildrenOnDetach) {
        mRecycleChildrenOnDetach = recycleChildrenOnDetach;
    }

    /**
     * Returns whether LayoutManager will recycle its children when it is detached from
     * RecyclerView.
     * @return true if LayoutManager will recycle its children when it is detached from
     * RecyclerView.
     */
    public boolean isRecycleChildrenOnDetach() {
        return mRecycleChildrenOnDetach;
    }

    public void setOnCompleteLayout(OnCompleteLayout onCompleteLayout) {
        mOnCompleteLayout = onCompleteLayout;
    }

    @Override
    public int getPaddingLeft() {
        return mPaddingLeft > 0 ? mPaddingLeft : super.getPaddingLeft();
    }

    @Override
    public int getPaddingRight() {
        return mPaddingRight > 0 ? mPaddingRight : super.getPaddingRight();
    }

    public void setPaddingLeft(int paddingLeft) {
        mPaddingLeft = paddingLeft;
    }

    public void setPaddingRight(int paddingRight) {
        mPaddingRight = paddingRight;
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
            int widthSpec, int heightSpec) {
        final int width = View.MeasureSpec.getSize(widthSpec);
        final int itemCount = state.getItemCount();
        int row = 0;
        int offsetX = getPaddingLeft();
        mColumnWidth = mColumn > 0 ? (width - getPaddingRight() - getPaddingLeft()) / mColumn : 0;
        for (int i = 0; i < itemCount; i++) {
            View view = recycler.getViewForPosition(i);
            if (view != null) {
                measureChildWithMargins(view, 0, 0);
                final int itemWidth = mColumn > 0 ? mColumnWidth : view.getMeasuredWidth();
                mRowHeight = Math.max(mRowHeight,
                        mOrientationHelper.getDecoratedMeasurementInOther(view));
                final int right = offsetX + itemWidth;
                if (right + getPaddingRight() > width) {
                    row++;
                    offsetX = getPaddingLeft();
                    if (row >= mRow) {
                        break;
                    }
                } else {
                    offsetX = right;
                }
                if (i == itemCount - 1) row++;
            }
        }
        setMeasuredDimension(width, mRowHeight * row);
    }

    public void setRow(int row) {
        mRow = row;
    }

    public void setColumn(int column) {
        mColumn = column;
    }

    public void setRowHeight(int rowHeight) {
        mRowHeight = rowHeight;
    }

    private int getRealWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private boolean isRevertLayout() {
        return UIUtils.isLayoutRtl(mContext);
    }
}