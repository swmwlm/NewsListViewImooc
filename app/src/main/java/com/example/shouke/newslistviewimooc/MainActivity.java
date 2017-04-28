package com.example.shouke.newslistviewimooc;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String URL = "http://www.imooc.com/api/teacher?type=4&num=30";
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.lv_main);
        new newsAsyncTask().execute(URL);
    }

    /**
     * 将url对应的json字符串数据，解析为list集合
     * @param url 获取json数据的网络路径
     * @return 对象集合
     */
    private List<NewsBean> getJsonData(String url) {
        List<NewsBean> newsBeanList = new ArrayList<>();
        try {
            /**
             *
             {
                 status: 1,
                 data: [{
                 id: 1,
                 name: "Tony老师聊shell——环境变量配置文件",
                 picSmall: "http://img.mukewang.com/55237dcc0001128c06000338-300-170.jpg",
                 picBig: "http://img.mukewang.com/55237dcc0001128c06000338.jpg",
                 description: "为你带来shell中的环境变量配置文件",
                 learner: 12312
                 },{
                 id: 2,
                 name: "数学知识在CSS动画中的应用",
                 picSmall: "http://img.mukewang.com/55249cf30001ae8a06000338-300-170.jpg",
                 picBig: "http://img.mukewang.com/55249cf30001ae8a06000338.jpg",
                 description: "数学知识与CSS结合实现酷炫效果",
                 learner: 45625
                 }],
                 msg: "成功"
             }
             */
            String jsonString = readStream(new URL(url).openStream());
//          Log.d("xys", jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            NewsBean newsBean;
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                newsBean = new NewsBean();
                newsBean.newsIconUrl = jsonObject.getString("picSmall");
                newsBean.newsTitle = jsonObject.getString("name");
                newsBean.newsContent = jsonObject.getString("description");
                newsBeanList.add(newsBean);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return newsBeanList;
    }

    /**
     * 从is流中解析网页返回的数据
     * @param is
     * @return
     */
    private String readStream(InputStream is){
        InputStreamReader isr=null;
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        try {
            //将is字节流转为字符流
            isr = new InputStreamReader(is, "utf-8");
            //将字符流包装成BufferedReader
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    /**
     * 实现网络的异步访问
     */
    class newsAsyncTask extends AsyncTask<String, Void, List<NewsBean>> {

        @Override
        protected List<NewsBean> doInBackground(String... params) {
            return getJsonData(params[0]);
        }

        /**
         * 将doInBackground返回的数据，进行执行ui线程操作
         * @param list
         */
        @Override
        protected void onPostExecute(List<NewsBean> list) {
            super.onPostExecute(list);
            NewsAdapter newsAdapter = new NewsAdapter(MainActivity.this, list,mListView);
            //设置listview的adapter
            mListView.setAdapter(newsAdapter);
        }
    }


}
