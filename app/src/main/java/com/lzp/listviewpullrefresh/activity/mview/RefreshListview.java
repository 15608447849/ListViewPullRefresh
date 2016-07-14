package com.lzp.listviewpullrefresh.activity.mview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lzp.listviewpullrefresh.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by user on 2016/7/14.
 */
public class RefreshListview extends ListView{

    private final String TAG = "mlistviews";
    public RefreshListview(Context context) {
        super(context);
        init(context);
    }

    public RefreshListview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshListview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private Context conx;
    private void init(Context c){
        conx = c;
        initHeadView();
        mListenScrollEvent();
    }
////////

    //头视图 子视图 持有者
    private class headerViewHolder {
        TextView tip;
        TextView lastupdate_time;
        ImageView img;
        ProgressBar progress ;

        //旋转动画
        RotateAnimation rotate_toTop;
        RotateAnimation rotate_toBotton;
        public headerViewHolder(TextView tip, TextView lastupdate_time, ImageView img, ProgressBar progress) {
            this.tip = tip;
            this.lastupdate_time = lastupdate_time;
            this.img = img;
            this.progress = progress;
            rotate_toTop = new RotateAnimation(0,180, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            rotate_toTop.setDuration(500);
            rotate_toTop.setFillAfter(true);


            rotate_toBotton = new RotateAnimation(180,0, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            rotate_toBotton.setDuration(500);
            rotate_toBotton.setFillAfter(true);
        }
    }


    /////////
    ////////////////////////////////////////////////////////////////
    /**头视图*/
    View header ;
    private int pullPoint = 30;//临界点
    private int headerHeight = 0;//头视图 高度
    private headerViewHolder hviewHolder ;

    private void initHeadView() {

        header = LayoutInflater.from(conx)
                .inflate(R.layout.header_layout,null);

        notifiyHeaderOnParentSize();
        //隐藏 头视图  ->  上边距 设置成 自己高度 的 负值
        headerHeight =  header.getMeasuredHeight();  //如果 没有通知过父容器自己的宽高 是获取不到的
        Log.i(TAG,"头视图 高度:"+headerHeight);
        pullPoint = pullPoint + headerHeight;
        topPadding(-headerHeight);
        this.addHeaderView(header); //添加顶布局

        hviewHolder = new headerViewHolder(
                (TextView)header.findViewById(R.id.tip),
                (TextView)header.findViewById(R.id.lastupdate_time),
                (ImageView) header.findViewById(R.id.arrow),
                (ProgressBar)header.findViewById(R.id.progress)
        );

    }

    /**
     * 通知父布局 到底占多大
     * MeasureSpec封装了父布局传递给子布局的布局要求
     */
    private void notifiyHeaderOnParentSize(){
        ViewGroup.LayoutParams param = header.getLayoutParams();
        if(param == null){
            param = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        //listView是限制宽度的，所以需要getChildMeasureSpec
        int width = ViewGroup.getChildMeasureSpec(
                0,//spec 左右边距0
                 0,//padding 内边距
                  param.width//childDimension子尺寸
        );

        //listView不限制高度
        int height = 0;
        if(param.height>0){
            //高度不是0  填充这个布局
            height = MeasureSpec.makeMeasureSpec(param.height,MeasureSpec.EXACTLY);//尺寸大小 , 模式精确的
        }else{
            height = MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);//尺寸大小 , 模式 未指定的
        }
        header.measure(width,height); //测量

    }
    /**
     * 设置header 布局 上边距；
     *
     * @param topPadding
     *
     */
    private void topPadding(int topPadding) {
        //int left, int top, int right, int bottom
        header.setPadding(
                header.getPaddingLeft(),
                topPadding,
                header.getPaddingRight(),
                header.getPaddingBottom());
        header.invalidate();
    }
//////////////////////////////////////////////////////////////////////////
private int current_firstVisibleItem = 0 ;//当前视图第一个可见下标
    private boolean isScolling = false;
    /**
     * 滚动监听
     */
    private void mListenScrollEvent(){

        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            //firstVisibleItem 第一个可见子项 位置
                //如果 firstVisibleItem 是 0, 当前在最顶端
                   //按下 ->  移动 -> 抬起  的手势监听 触发 下拉刷新 -> 去监听 触摸事件
                current_firstVisibleItem = firstVisibleItem;
                Log.i(TAG,"onScroll "+firstVisibleItem );
            }
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == SCROLL_STATE_TOUCH_SCROLL){
                        isScolling = true;
                        Log.i(TAG,"onScrollStateChanged "+scrollState +"--"+SCROLL_STATE_TOUCH_SCROLL);
                    }else{
                        isScolling = false;
                        Log.i(TAG,"onScrollStateChanged "+false);
                    }
            }
        });
    }

    private boolean  isTopByDown = false;
    private float startX,startY = -1; //开始 坐标
    private float endX,endY = -1; //结束 坐标


    private final int NONE = 100;//正常
    private final int PULL = 101;//提示下拉状态
    private final int RELESE = 102;//提示释放
    private final int REFRESH = 103;//刷新中
    private int moving_state = NONE;  //当前状态

    //触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        switch (ev.getAction()){
            //按下
            case MotionEvent.ACTION_DOWN:
                Log.i(TAG,"onTouchEvent  down" );
                //是不是在最顶端
                if (current_firstVisibleItem == 0){
                    //设置一个标记表示 当前在最顶端按下的
                    isTopByDown = true;
                    startY =  ev.getY();
                    Log.i(TAG,"ACTION_DOWN  isTopByDown:"+isTopByDown );
                }
            break;
            //移动
            case MotionEvent.ACTION_MOVE:
                Log.i(TAG,"onTouchEvent  move" );
                //是否在顶端按下
                if (isTopByDown){

                    //需要定义状态变量
                    // 1. 往下拖的过程中  头视图 出现, 显示 "下拉刷新"
                    // 2.到达一个临界点  显示 "松开刷新"
                    //3.  松开 "正在刷新" 跳出进度条

                    float temY = ev.getY();//当前移动到哪一个点 垂直方向
                    int move_distance = (int)(temY - startY); //移动距离  如果大于0说明往下移动了

                    //设置 头视图 慢慢下移的效果 的 距离
                    int topPadding = move_distance - headerHeight;
                    Log.i(TAG,"onTouchEvent  move distance:"+ move_distance +" , top height :"+ topPadding );
                    switch (moving_state){ //移动状态

                        case NONE: //正常
                            if (move_distance>0){
                                moving_state = PULL;
                                reflashViewByState();
                            }
                        break;
                        case PULL:

                            //设置 头视图 慢慢下移的效果
                            topPadding(topPadding);

                        //如果大于一定高度 ,并且正在滚动中 就变成释放状态
                            if (move_distance>pullPoint && isScolling){
                                moving_state = RELESE;
                                reflashViewByState();
                            }
                            break;

                        case RELESE://释放
                            topPadding(topPadding);

                            if (move_distance<pullPoint){
                                moving_state = PULL;
                                reflashViewByState();
                            }else if(move_distance<=0){
                                moving_state = NONE;
                                isTopByDown = false; //重置 按下事件
                                reflashViewByState();
                            }
                        break;
                    }
                }
            break;
            //抬起
            case MotionEvent.ACTION_UP:

                Log.i(TAG,"onTouchEvent  up" );
                //如果在 释放状态 抬起了手 -> 刷新
                if (moving_state == RELESE){
                    moving_state = REFRESH;
                    reflashViewByState();
                    //加载最新数据
                    if (refresh!=null){
                        refresh.mOnRefresh();
                    }

                }else if (moving_state == PULL){//如果时 下拉时 松手了, 变成正常状态啦
                    moving_state = NONE;
                    isTopByDown = false;
                    reflashViewByState();
                }
            break;
        }
        return super.onTouchEvent(ev);
    }
    ///////////////////////////////////////////////////////////////////


    /**
     * 根据当前状态，改变界面显示；
     */
    private void reflashViewByState() {

        switch (moving_state) {
            case NONE: //正常
                hviewHolder.img.clearAnimation();
                topPadding(-headerHeight);
                break;

            case PULL:
                hviewHolder.img.setVisibility(View.VISIBLE);
                hviewHolder.progress.setVisibility(View.GONE);
                hviewHolder.tip.setText("下拉可以刷新！");
                hviewHolder.img.clearAnimation();
                hviewHolder.img.setAnimation(hviewHolder.rotate_toBotton);
                break;
            case RELESE:
                hviewHolder.img.setVisibility(View.VISIBLE);
                hviewHolder.progress.setVisibility(View.GONE);
                hviewHolder.tip.setText("松开可以刷新！");
                hviewHolder.img.clearAnimation();
                hviewHolder.img.setAnimation(hviewHolder.rotate_toTop);
                break;
            case REFRESH:
                topPadding(headerHeight);
                hviewHolder.img.setVisibility(View.GONE);
                hviewHolder.progress.setVisibility(View.VISIBLE);
                hviewHolder.tip.setText("正在刷新...");
                hviewHolder.img.clearAnimation();

                break;
        }
    }


    /**
     * 数据获取完毕后
     * 刷新完毕
     */
    public void refreshComplete(){
        moving_state = NONE;
        isTopByDown = false;
        reflashViewByState();

            //设置上次更新时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String dateStr = format.format(date);
        hviewHolder.lastupdate_time.setText(" ["+dateStr+"]");
    }


    /**
     * 刷新数据接口回调
     */
    public interface RefreshDataLinstener{
       public void mOnRefresh();
    }

    private RefreshDataLinstener refresh;
    public void setRefreshDataLinstener(RefreshDataLinstener refresh){
        this.refresh =  refresh;
    }


}













/**
 *源码 listview 获取子项宽高
 *
  private void measureItem(View child) {
            ViewGroup.LayoutParams p = child.getLayoutParams();
                 if (p == null) {
                 p = new ViewGroup.LayoutParams(
                 ViewGroup.LayoutParams.MATCH_PARENT,
                 ViewGroup.LayoutParams.WRAP_CONTENT);
                 }

 int childWidthSpec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec,
 mListPadding.left + mListPadding.right,
 p.width);

 int lpHeight = p.height;
 int childHeightSpec;
 if (lpHeight > 0) {
 childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
 }
 else {
 childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
 }
 child.measure(childWidthSpec, childHeightSpec);
 }
 *
 *
 */