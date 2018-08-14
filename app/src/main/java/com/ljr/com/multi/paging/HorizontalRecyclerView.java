package com.ljr.com.multi.paging;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class HorizontalRecyclerView extends RecyclerView {
    public HorizontalRecyclerView(Context context) {
        this(context, null);
    }

    public HorizontalRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Check.isNotNull(context);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return getLayoutManager().canScrollHorizontally() || super.canScrollHorizontally(direction);
    }
}
