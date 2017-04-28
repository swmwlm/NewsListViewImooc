package com.example.shouke.newslistviewimooc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * 实现listview的OnScrollListener接口，是为了优化滚动时卡顿的现象；
 * 当滚动时，停止异步加载任务；当停止滚动时，加载可见项
 */
public class NewsAdapter extends BaseAdapter implements AbsListView.OnScrollListener{
    private List<NewsBean> mList;
    private LayoutInflater mInflater;
    private ImageLoader mImageLoader;

    private int mStart,mEnd;
    //保存获取所有图片的url地址
    public static String[] URLS;
    private boolean mFirstIn;//判断是否第一次加载

    public NewsAdapter(Context context, List<NewsBean> data, ListView listView) {
        mInflater = LayoutInflater.from(context);
        mList = data;
        mImageLoader=new ImageLoader(listView);

        //初始化URLS；设置所有的图片路径；以供外部可以调用该URLS静态属性
        URLS = new String[data.size()];
        for (int i = 0; i < data.size(); i++) {
            URLS[i] = data.get(i).newsIconUrl;
        }
        // 一定要注册滚动事件
        listView.setOnScrollListener(this);
        mFirstIn = true;//初始化时 设置为true

    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_icon);
            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
            viewHolder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);

            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        NewsBean newsBean=mList.get(position);
        //设置一个默认的本地图片
        viewHolder.ivIcon.setImageResource(R.mipmap.ic_launcher);

        //给imageview绑定tag，值为图片的url路径;
        viewHolder.ivIcon.setTag(newsBean.newsIconUrl);
        //多线程方式加载图片
//        new ImageLoader().showImageByThread(viewHolder.ivIcon, newsBean.newsIconUrl);
        //使用AsyncTask方式加载图片
        //当加入LruCache后，不能通过new 创建类的方式进行调用ImageLoader,否则会产生多个LruCache,达不到缓存效果
//        new ImageLoader().showImageByAsyncTask(viewHolder.ivIcon,newsBean.newsIconUrl);
        //通过构造方法初始化mImageLoader；然后这里进行调用
        mImageLoader.showImageByAsyncTask(viewHolder.ivIcon,newsBean.newsIconUrl);
        viewHolder.tvTitle.setText(newsBean.newsTitle);
        viewHolder.tvContent.setText(newsBean.newsContent);

        return convertView;
    }

    /**
     * 处于停止滚动状态时，加载可见项
     * @param view
     * @param scrollState
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            //加载可见项
            mImageLoader.loadImages(mStart, mEnd);
        }else{
            //停止任务
            mImageLoader.cancelAllTasks();
        }
    }

    /**
     *
     * @param view
     * @param firstVisibleItem 第一个可见的item
     * @param visibleItemCount 可见item的总数
     * @param totalItemCount 总的item个数
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mStart=firstVisibleItem;
        mEnd = firstVisibleItem + visibleItemCount;
        //第一次显示的时候调用
        if (mFirstIn && visibleItemCount > 0) {
            mImageLoader.loadImages(mStart, mEnd);
            mFirstIn = false;
        }
    }

    /**
     * 文艺式 获取View
     */
    class ViewHolder{
        public TextView tvTitle,tvContent;
        public ImageView ivIcon;
    }
}
