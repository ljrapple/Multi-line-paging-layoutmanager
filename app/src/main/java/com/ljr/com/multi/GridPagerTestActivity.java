package com.ljr.com.multi;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ljr.com.multi.paging.GridPagerLayoutManager;
import com.ljr.com.multi.paging.HorizontalPagerScrollHelper;
import com.ljr.com.multi.paging.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class GridPagerTestActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid_layout);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        GridPagerLayoutManager layoutManager = new GridPagerLayoutManager();
        layoutManager.setRecycleChildrenOnDetach(true);
        recyclerView.setLayoutManager(layoutManager);
        Adapter adapter = new Adapter();
        final HorizontalPagerScrollHelper scrollHelper =
                HorizontalPagerScrollHelper.createGridPagerScrollHelper(recyclerView);
        layoutManager.setOnCompleteLayout(new GridPagerLayoutManager.OnCompleteLayout() {
            @Override
            public void onCompleteLayout(boolean refresh, int pageSize) {
                if (refresh) {
                    scrollHelper.reinitialization(pageSize);
                }
            }
        });
        int padding = (int) UIUtils.dpToPixels(6, getApplicationContext());
        layoutManager.setPaddingLeft(padding);
        layoutManager.setPaddingRight(padding);
        layoutManager.setRow(4);
        int column = getIntent().getIntExtra("column", 0);
        layoutManager.setColumn(column);
        recyclerView.setAdapter(adapter);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView;
        }

        private void bind(String text) {
            mTextView.setText(text);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private List<String> mData = new ArrayList<>();

        Adapter() {
            mData.addAll(DataGenerator.getStringsData(80));
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View textView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.grid_item, parent, false);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }
}
