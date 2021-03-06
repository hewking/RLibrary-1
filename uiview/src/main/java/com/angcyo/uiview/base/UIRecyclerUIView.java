package com.angcyo.uiview.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.angcyo.uiview.R;
import com.angcyo.uiview.container.ContentLayout;
import com.angcyo.uiview.container.UIParam;
import com.angcyo.uiview.recycler.RRecyclerView;
import com.angcyo.uiview.recycler.adapter.RBaseAdapter;
import com.angcyo.uiview.recycler.adapter.RExBaseAdapter;
import com.angcyo.uiview.rsen.BasePointRefreshView;
import com.angcyo.uiview.rsen.RGestureDetector;
import com.angcyo.uiview.rsen.RefreshLayout;
import com.angcyo.uiview.utils.RUtils;
import com.angcyo.uiview.widget.viewpager.UIViewPager;

import java.util.List;

/**
 * Created by angcyo on 2017-03-11.
 */

public abstract class UIRecyclerUIView<H, T, F> extends UIContentView
        implements RefreshLayout.OnRefreshListener, RBaseAdapter.OnAdapterLoadMoreListener {

    /**
     * 刷新控件
     */
    protected RefreshLayout mRefreshLayout;
    /**
     * 列表
     */
    protected RRecyclerView mRecyclerView;
    protected RExBaseAdapter<H, T, F> mExBaseAdapter;

    protected int mBaseOffsetSize;
    protected int mBaseLineSize;
    protected int page = 1;

    @NonNull
    @Override
    protected LayoutState getDefaultLayoutState() {
        return LayoutState.LOAD;
    }

    @Override
    public void onViewCreate(View rootView, UIParam param) {
        super.onViewCreate(rootView, param);
        mBaseOffsetSize = getDimensionPixelOffset(R.dimen.base_xhdpi);
        mBaseLineSize = getDimensionPixelOffset(R.dimen.base_line);
    }

    @Override
    final protected void inflateContentLayout(@NonNull ContentLayout baseContentLayout, @NonNull LayoutInflater inflater) {
        beforeInflateView(baseContentLayout);

        createRecyclerRootView(baseContentLayout, inflater);

        afterInflateView(baseContentLayout);

        baseInitLayout();
    }

    /**
     * 复写此方法, 重写根布局
     */
    protected void createRecyclerRootView(@NonNull ContentLayout baseContentLayout, @NonNull LayoutInflater inflater) {
        int layoutId = getRecyclerRootLayoutId();
        if (layoutId == -1) {
            mRefreshLayout = new RefreshLayout(mActivity);
            mRecyclerView = new RRecyclerView(mActivity);
        } else {
            View view = inflater.inflate(layoutId, baseContentLayout);
            mRefreshLayout = view.findViewById(R.id.base_refresh_view);
            mRecyclerView = view.findViewById(R.id.base_recycler_view);
        }

        initRefreshLayout(mRefreshLayout, baseContentLayout);
        initRecyclerView(mRecyclerView, baseContentLayout);
    }

    /**
     * 返回自定义Layout的id, 请确保控件id R.id.base_refresh_view/R.id.base_recycler_view
     */
    protected int getRecyclerRootLayoutId() {
        if (haveSoftInput()) {
            return R.layout.base_input_recycler_view_layout;
        }
        return R.layout.base_recycler_view_layout;
    }

    /**
     * 是否需要软键盘
     */
    protected boolean haveSoftInput() {
        return false;
    }

    /**
     * 自动监听滚动事件, 设置标题栏的透明度
     */
    protected boolean hasScrollListener() {
        return false;
    }

    /**
     * 滚动过程是否控制标题文本的显示
     */
    protected boolean hasScrollVisibleTitle() {
        return true;
    }

    /**
     * 双击自动自动滚动置顶
     */
    public void onDoubleScrollToTop() {
        if (mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    protected void baseInitLayout() {
        if (getUITitleBarContainer() != null) {
            //双击标题, 自动滚动到顶部
            RGestureDetector.onDoubleTap(getUITitleBarContainer(), new RGestureDetector.OnDoubleTapListener() {
                @Override
                public void onDoubleTap() {
                    onDoubleScrollToTop();
                }
            });
        }

        if (mRecyclerView != null && hasScrollListener()) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
                    onRecyclerViewScrolled(recyclerView, dx, dy, verticalScrollOffset);

                    if (mUITitleBarContainer != null) {
                        if (hasScrollVisibleTitle()) {
                            mUITitleBarContainer.evaluateBackgroundColorSelf(verticalScrollOffset);
                        } else {
                            mUITitleBarContainer.evaluateBackgroundColor(verticalScrollOffset, null);
                        }
                    }
                }
            });
        }
    }

    protected void onRecyclerViewScrolled(@NonNull RecyclerView recyclerView, int dx, int dy, int verticalScrollOffset) {

    }

    /**
     * 填充试图之前调用
     */
    protected void beforeInflateView(@NonNull ContentLayout baseContentLayout) {

    }

    /**
     * 内容试图填充之后调用
     */
    protected void afterInflateView(@NonNull ContentLayout baseContentLayout) {

    }

    protected void initRecyclerView(RRecyclerView recyclerView, ContentLayout baseContentLayout) {
        if (recyclerView == null) {
            return;
        }
        final RecyclerView.ItemDecoration itemDecoration = createItemDecoration();
        if (itemDecoration != null) {
            recyclerView.addItemDecoration(itemDecoration);
        }
        mExBaseAdapter = createAdapter();
        initAdapter(mExBaseAdapter);

        recyclerView.setAdapter(mExBaseAdapter);

        if (mExBaseAdapter != null) {
            mExBaseAdapter.setOnLoadMoreListener(this);
        }

        if (recyclerView.getParent() == null) {
            if (mRefreshLayout == null) {
                baseContentLayout.addView(recyclerView, new ViewGroup.LayoutParams(-1, -1));
            } else {
                mRefreshLayout.addView(recyclerView, new ViewGroup.LayoutParams(-1, -1));
            }
        }
    }

    @NonNull
    protected abstract RExBaseAdapter<H, T, F> createAdapter();

    protected void initAdapter(@NonNull RExBaseAdapter<H, T, F> baseAdapter) {

    }

    protected RecyclerView.ItemDecoration createItemDecoration() {
        return null;
    }

    protected void initRefreshLayout(RefreshLayout refreshLayout, ContentLayout baseContentLayout) {
        if (refreshLayout == null) {
            return;
        }
        refreshLayout.setRefreshDirection(RefreshLayout.TOP);
        refreshLayout.addOnRefreshListener(this);
        refreshLayout.setTopView(new BasePointRefreshView(mActivity));
        refreshLayout.setBottomView(new BasePointRefreshView(mActivity));
        if (refreshLayout.getParent() == null) {
            baseContentLayout.addView(refreshLayout, new ViewGroup.LayoutParams(-1, -1));
        }
    }

    /**
     * 刷新控件, 刷新事件回调
     */
    @Override
    public void onRefresh(@RefreshLayout.Direction int direction) {
        if (direction == RefreshLayout.TOP) {
            //刷新事件, 清空缓存
            mBaseDataObject = null;
            onBaseLoadData("onRefreshTop", true);
        } else if (direction == RefreshLayout.BOTTOM) {
            //加载更多事件
            onBaseLoadMore("onRefreshBottom", true);
        }
    }

    /**
     * 会显示LoadView
     */
    public void onBaseLoadMore() {
        onBaseLoadMore("", true);
    }

    /**
     * 会显示LoadView
     */
    public void onBaseLoadData() {
        onBaseLoadData("", true);
    }

    public void onBaseLoadMore(String extend, boolean showLoadView) {
        page++;
        if (showLoadView) {
            showLoadView();
        }
        onUILoadData(page);
        onUILoadData(page, extend);
    }

    public void onBaseLoadData(String extend, boolean showLoadView) {
        if (mBaseDataObject == null) {
            page = 1;
        } else {
            //有缓存的时候, page 维持不变
        }
        if (showLoadView) {
            showLoadView();
        }
        onUILoadData(page);
        onUILoadData(page, extend);
    }

    /**
     * 触发刷新控件的刷新
     */
    public void refreshUIData() {
        if (mRefreshLayout == null) {
            loadData();
        } else {
            mRefreshLayout.setRefreshState(RefreshLayout.TOP);
        }
    }

    /**
     * 不显示LoadView
     */
    public void loadData() {
        loadData(false);
    }

    public void loadData(boolean showLoadView) {
        loadData("loadData", showLoadView);
    }

    /**
     * 不显示LoadView
     */
    public void loadMore() {
        onBaseLoadMore("loadMore", false);
    }

    public void loadData(String extend, boolean showLoadView) {
        onBaseLoadData(extend, showLoadView);
    }

    @Deprecated
    public void onUILoadData(int page) {

    }

    public void onUILoadData(int page, String extend) {

    }

    /**
     * 重置UI状态
     */
    public void resetUI() {
        hideLoadView();
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshEnd();
        }
        if (mExBaseAdapter != null) {
            if (mExBaseAdapter.isEnableLoadMore()) {
                mExBaseAdapter.setLoadMoreEnd();
            }
        }
    }

    /**
     * 调用此方法自动设置视图
     */
    public void onUILoadFinish(List<T> datas) {
        resetUI();
        if (isUIDataEmpty(page, datas)) {
            if (page <= 1) {
                onUILoadEmpty();
            } else {
                if (mExBaseAdapter.isEnableLoadMore()) {
                    mExBaseAdapter.setNoMore(true);
                }
            }
        } else {
            showContentLayout();
            if (mExBaseAdapter != null) {
                if (isUIFirstLoadData()) {
                    mExBaseAdapter.resetAllData(datas);
                } else {
                    mExBaseAdapter.appendAllData(datas);
                }

                if (mExBaseAdapter.isAutoEnableLoadMore()) {
                    if (isUIHaveLoadMore(datas)) {
                        mExBaseAdapter.setEnableLoadMore(true);
                    }
                }

                if (mExBaseAdapter.isEnableLoadMore()) {
                    if (isUIHaveLoadMore(datas)) {
                        mExBaseAdapter.setLoadMoreEnd();
                    } else {
                        mExBaseAdapter.setNoMore(true);
                    }
                }
            }
        }
    }

    /**
     * 数据为空时,回调
     */
    public void onUILoadEmpty() {
        showEmptyLayout();
    }

    /**
     * 当网络加载失败时, 如果有数据, 则不显示错误视图
     */
    public void onUILoadDataError() {
        resetUI();
        if (page <= 1 && (mExBaseAdapter == null || mExBaseAdapter.getRawItemCount() < 1)) {
            showNonetLayout(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBaseLoadData();
                }
            });
        } else {
            if (mExBaseAdapter != null) {
                if (mExBaseAdapter.getRawItemCount() > 0 && mExBaseAdapter.isEnableLoadMore()) {
                    mExBaseAdapter.setLoadError();
                }
            }
        }
    }

    @Override
    public void showNonetLayout() {
        showNonetLayout(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadLayout();
                onBaseLoadData();
            }
        });
    }

    public boolean isUIFirstLoadData() {
        return page == 1;
    }

    public boolean isUIHaveLoadMore(List<T> datas) {
        return datas != null && datas.size() >= 20;
    }

    public boolean isUIDataEmpty(int page, List<T> datas) {
        return RUtils.isListEmpty(datas);
    }

    /**
     * 需要触发加载数据
     */
    protected boolean needLoadData() {
        boolean load = getDefaultLayoutState() == LayoutState.LOAD;
        if (isShowInViewPager()) {
            return showInPagerCount <= 1 && load;
        }
        return load;
    }

    @Override
    public void onViewShowFirst(Bundle bundle) {
        super.onViewShowFirst(bundle);
        if (getDefaultLayoutState() == LayoutState.CONTENT) {
            onLoadDefaultDataFirst();
        }
        if (needLoadData() && !isShowInViewPager()) {
            onBaseLoadData("onViewShowFirst", true);
        }
    }

    public void onLoadDefaultDataFirst() {

    }

    @Override
    public void onShowInPager(UIViewPager viewPager) {
        super.onShowInPager(viewPager);
        if (needLoadData() && isShowInViewPager()) {
//            (getLayoutState() == LayoutState.LOAD
//                    && mBaseDataObject == null)
            onBaseLoadData("onShowInPager", true);
        }
    }

    /**
     * adapter加载更多回调
     */
    @Override
    public void onAdapterLodeMore(RBaseAdapter baseAdapter) {
        onBaseLoadMore();
    }
}
