
package com.horizon.collector.huaban.ui;


import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.horizon.base.network.NetworkUtil;
import com.horizon.base.ui.BaseAdapter;
import com.horizon.base.util.CollectionUtil;
import com.horizon.base.util.LogUtil;
import com.horizon.base.util.ToastUtil;
import com.horizon.collector.R;
import com.horizon.collector.common.ChannelFragment;
import com.horizon.collector.common.channel.Channel;
import com.horizon.collector.huaban.source.HuabanCatcher;
import com.horizon.collector.huaban.source.Pin;
import com.horizon.task.UITask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HuabanChannelFragment extends ChannelFragment {

    private PinAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public static HuabanChannelFragment newInstance(Channel channel) {
        HuabanChannelFragment fragment = new HuabanChannelFragment();
        initFragment(fragment, channel);
        return fragment;
    }


    @Override
    protected int getLayoutResource() {
        return R.layout.fragement_channel_page;
    }

    @Override
    protected void initView() {
        super.initView();

        mAdapter = new PinAdapter(mActivity, new ArrayList<Pin>(), true);
        mAdapter.setLoadingFooter(R.layout.footer_loading);
        mAdapter.setHost(this);
        mAdapter.setOnLoadMoreListener(new BaseAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(boolean forceReload) {
                if (mLastPage == mNextPage && !forceReload) {
                    return;
                }
                mLastPage = mNextPage;
                mLoadingMode = MODE_LOAD_MORE;
                loadData();
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.page_sfl);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent,
                R.color.colorPrimaryDark);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mLoadingMode = MODE_REFRESH;
                loadData();
            }
        });

        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.page_rv);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
    }

    protected void loadData() {
        if(NetworkUtil.isConnected()){
            new FetchPinsTask().host(this).execute();
        }else {
            ToastUtil.showTips(R.string.connect_tips);
        }
    }


    private class FetchPinsTask extends UITask<Void, Void, List<Pin>> {
        private String pinID = "";
        private boolean hasData = true;

        @Override
        protected void onPreExecute() {
            if (mLoadingMode == MODE_LOAD_MORE) {
                Pin lastPin = mAdapter.getLastItem();
                if (lastPin != null) {
                    pinID = lastPin.id;
                }
            }
        }

        @Override
        protected List<Pin> doInBackground(Void... params) {
            try {
                List<Pin> pinList = HuabanCatcher.pickPins(mChannel.id, pinID);
                hasData = !CollectionUtil.isEmpty(pinList);
                // 过滤重复的图片
                Iterator<Pin> iterator = pinList.iterator();
                while (iterator.hasNext()) {
                    String id = iterator.next().id;
                    if (mIdSet.contains(id)) {
                        iterator.remove();
                    } else {
                        mIdSet.add(id);
                    }
                }

                return pinList;
            } catch (Exception e) {
                LogUtil.e(TAG, e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Pin> pinList) {
            if (mLoadingMode == MODE_LOAD_MORE) {
                if (pinList == null) {
                    // 返回null说明获取列表时发生异常
                    mAdapter.setFailedFooter(R.layout.footer_failed);
                } else if (!hasData) {
                    mAdapter.setEndFooter(R.layout.footer_end);
                } else {
                    mNextPage++;
                    mAdapter.appendData(pinList);
                }
            } else {
                if (!CollectionUtil.isEmpty(pinList)) {
                    if (mAdapter.getDataSize() == 0) {
                        mAdapter.setData(pinList);
                    } else {
                        mAdapter.insertFront(pinList);
                    }
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }

}
