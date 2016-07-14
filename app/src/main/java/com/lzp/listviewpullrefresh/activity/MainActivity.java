package com.lzp.listviewpullrefresh.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lzp.listviewpullrefresh.R;
import com.lzp.listviewpullrefresh.activity.mview.RefreshListview;
import com.lzp.listviewpullrefresh.activity.mview.mListViewAdapter;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    ArrayList<mListViewAdapter.ApkEntity> apk_list;
    private void setData() {
        apk_list = new ArrayList<mListViewAdapter.ApkEntity>();
        for (int i = 0; i < 10; i++) {
            mListViewAdapter.ApkEntity entity = new mListViewAdapter.ApkEntity();
            entity.name = "默认数据";
            entity.des = "这是一个神奇的应用";
            entity.info = "50w用户";
            apk_list.add(entity);
        }
    }

    private void setReflashData() {

        for (int i = 0; i < 2; i++) {
            mListViewAdapter.ApkEntity entity = new mListViewAdapter.ApkEntity();
            entity.name = "刷新数据"+i;
            entity.des = "这是一个神奇的应用";
            entity.info = "50w用户";
            apk_list.add(0,entity);
        }
    }

    mListViewAdapter adapter = null ;
    RefreshListview refreshListview = null;
    /**
     * 初始化
     *
     */
    private void init() {
        setData();
        refreshListview =  (RefreshListview) findViewById(R.id.mlistview);
        adapter = new mListViewAdapter(this,apk_list);
        refreshListview.setAdapter(adapter);

        refreshListview.setRefreshDataLinstener(new RefreshListview.RefreshDataLinstener() {
            @Override
            public void mOnRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setReflashData();//获取数据
                                //通知显示数据
                                adapter.setData(apk_list);
                                //通知listview 刷新数据完毕
                                refreshListview.refreshComplete();
                            }
                        });


                    }
                }).start();


            }
        });
    }


}
