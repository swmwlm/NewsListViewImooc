package com.example.shouke.newslistviewimooc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * 图片加载：两种方式
 * 1.通过多线程方式实现异步加载
 * 2.通过AsyncTask实现异步加载，底层也是通过多线程方式加载
 */
public class ImageLoader {
    private ImageView mImageView;
    private String mUrl;
    //使用LruCache缓存
    private LruCache<String, Bitmap> mCaches;
    private ListView mListView;
    private Set<NewsAsyncTask> mTasks;

    public ImageLoader(ListView listview) {
        mListView = listview;
        mTasks = new HashSet<>();
        int maxMemory= (int) Runtime.getRuntime().maxMemory();
        int cacheSize=maxMemory/4;
        /**
         * 通过重写匿名内部类的方法sizeOf，来
         */
        mCaches = new LruCache<String,Bitmap>(cacheSize){
            /**
             * 告诉我们的缓存，当前存入的数据有多大；而不是用默认的返回缓存的个数
             * @param key
             * @param value
             * @return
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //默认返回的是缓存的个数
//                return super.sizeOf(key, value);
                //在每次存入缓存时调用；
                return value.getByteCount();
            }
        };

    }

    //从缓存中获取bitmap
    public Bitmap getBitmapFromCache(String url) {
        return mCaches.get(url);
    }

    /**
     * 增加到缓存
     * @param url
     * @param bitmap
     */
    public void addBitmapToCache(String url,Bitmap bitmap) {
        if (getBitmapFromCache(url) == null) {
            mCaches.put(url, bitmap);
        }
    }

    /**
     * 由于android单一UI线程，无法在多线程run方法中直接对ImageView设置Bitmap;因此要使用Handler消息机制
     */
    private Handler mHandler = new Handler() {
        /**
         * Handler在接收到子线程的message时，先要判断imageview中的tag标记，是否为当前url的值
         * 相同才设置imageview的bitmap；
         * 为了避免缓存的图片对正确的图片显示造成的影响
         * @param msg
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mImageView.getTag().equals(mUrl)) {
                mImageView.setImageBitmap((Bitmap) msg.obj);
            }
        }
    };

    public void showImageByThread(ImageView imageView, final String url) {
        mImageView = imageView;
        mUrl = url;
        new Thread() {
            @Override
            public void run() {
                super.run();
                Bitmap bitmap = getBitmapFromURL(url);

                //将子线程中bitmap通过消息方式发送给主线程；子线程中使用message发送消息给主线程
                //通过Message.obtain()方式创建的message，可以使用现有的已经回收掉的message,提高message的使用效率
                Message message = Message.obtain();
                message.obj = bitmap;
                //使用handler发送message
                mHandler.sendMessage(message);
            }
        }.start();
    }

    public Bitmap getBitmapFromURL(String urlString) {
        Bitmap bitmap;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            //使用HttpURLConnection比URLConnection 可以执行disconnect
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            URLConnection connection = url.openConnection();

            is = new BufferedInputStream(connection.getInputStream());
            bitmap = BitmapFactory.decodeStream(is);
            connection.disconnect();
            return bitmap;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void showImageByAsyncTask(ImageView imageView, String url) {
        //1.从缓存中取出图片
        Bitmap bitmap = getBitmapFromCache(url);
        if (bitmap == null) {
            //2.取不到，则异步通过AsyncTask网络下载，并将其加入到缓存
            //new NewsAsyncTask(imageView, url).execute(url);
            //注解掉上面一行代码；设置默认加载的本地图片；将异步加载图片转移到了 滚动加载的loadImages方法中
            imageView.setImageResource(R.mipmap.ic_launcher);
        }else{
            //3.使用bitmap
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * 加载从start到end的所有图片
     * @param start
     * @param end
     */
    public void loadImages(int start, int end) {
        for (int i = start; i < end; i++) {
            String url = NewsAdapter.URLS[i];
            //1.从缓存中取出图片
            Bitmap bitmap = getBitmapFromCache(url);
            if (bitmap == null) {
                //2.取不到，则异步通过AsyncTask网络下载，并将其加入到缓存
                //new NewsAsyncTask(imageView, url).execute(url);
                NewsAsyncTask task = new NewsAsyncTask(url);
                task.execute(url);
                mTasks.add(task);
            }else{

                //3.使用bitmap
                //imageView.setImageBitmap(bitmap);
                ImageView imageView = (ImageView) mListView.findViewWithTag(url);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public void cancelAllTasks() {
        if (mTasks != null) {
            for (NewsAsyncTask task : mTasks) {
                task.cancel(false);
            }
        }
    }

    private class NewsAsyncTask extends AsyncTask<String, Void, Bitmap> {

        //private ImageView mImageView;
        private String mUrl;

        public NewsAsyncTask(/*ImageView imageView, */String url) {
            //mImageView = imageView;
            mUrl = url;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            //从网络异步获取图片
            Bitmap bitmap = getBitmapFromURL(url);
            if (bitmap != null) {
                //将下载的bitmap加入到缓存
                addBitmapToCache(url,bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            /**
             * 同多线程方式一样，避免Listview缓存的图片对正确的图片显示造成的影响，需要对imageview增加标记tag认证
             * 认证通过的才可以设置bitmap
             */
            /*if (mImageView.getTag().equals(mUrl)) {
                mImageView.setImageBitmap(bitmap);
            }*/
            ImageView imageView = (ImageView) mListView.findViewWithTag(mUrl);
            imageView.setImageBitmap(bitmap);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            //从集合中移除掉本次的aysnctask
            mTasks.remove(this);
        }
    }
}
