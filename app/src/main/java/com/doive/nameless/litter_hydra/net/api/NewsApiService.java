package com.doive.nameless.litter_hydra.net.api;

import com.doive.nameless.litter_hydra.model.bean.DocNewsBean;
import com.doive.nameless.litter_hydra.model.bean.NewsBean;
import com.doive.nameless.litter_hydra.model.bean.NewsCommentBean;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

import static android.R.attr.action;
import static android.R.attr.id;

/**
 * Created by Administrator on 2017/3/17.
 *
 * 新闻网络接口的Service,提供基础网络地址,以及相应的Observable
 *
 */

public interface NewsApiService {

    String BASE_NEWS_URL = "http://api.iclient.ifeng.com/";

    String BASE_DOC_DETAIL_URL = "http://api.3g.ifeng.com/";

    String BASE_COMMENT_URL = "http://icomment.ifeng.com/";

    /**
     * 头条新闻
     * @param id
     * @param action
     * @param screen
     * @param uid
     * @return
     */
    @GET("ClientNews")
    Observable<List<NewsBean>> getData(@Query("id") String id,
                                       @Query("action") String action,
                                       @Query("screen") String screen,
                                       @Query("uid") String uid);

    /**
     * 加载更多新闻
     * @param id
     * @param action
     * @param timestamp
     * @param uid
     * @return
     */
    @GET("ClientNews")
    Observable<List<NewsBean>> getMoreData(@Query("id") String id,
                                           @Query("action") String action,
                                           @Query("timestamp") String timestamp,
                                           @Query("screen") String screen,
                                           @Query("uid") String uid);

    //http://api.iclient.ifeng.com/ClientNews?id=YL53,FOCUSYL53&page=3&uid=864394010382135&screen=720x1280
    //娱乐
    //http://api.iclient.ifeng.com/ClientNews?id=CJ33,FOCUSCJ33,HNCJ33&page=1&uid=864394010382135&screen=720x1280
    //财经
    //http://api.iclient.ifeng.com/ClientNews?id=KJ123,FOCUSKJ123&page=1&gv=5.4.1&av=5.4.1&uid=864394010382135&deviceid=864394010382135&proid=ifengnews&os=android_19&df=androidphone&vt=5&screen=720x1280&publishid=2011&nw=disconnected
    //科技
    /**
     * 列表栏目新闻
     * @param id
     * @param page
     * @param uid
     * @param screen
     * @return
     */
    @GET("ClientNews")
    Observable<List<NewsBean>> getColumnData(@Query("id") String id,
                                             @Query("page") int page,
                                             @Query("uid") String uid,
                                             @Query("screen") String screen);

    /**
     * Doc新闻详情
     * http://api.3g.ifeng.com/ipadtestdoc?aid=cmpp_030180050996690&os=android_23&gv=5.4.1
     */
    @GET("ipadtestdoc")
    Observable<DocNewsBean> getDocNewsData(@Query("aid")String aid,@Query("os")String os,@Query("gv")String gv);

    /**
     * 新闻评论url
     * http://icomment.ifeng.com/geti.php?pagesize=20&p=0&docurl=http%3A%2F%2Fent.ifeng.com%2Fa%2F20170425%2F42913555_0.shtml&type=all
     */
    @GET("geti.php")
    Observable<NewsCommentBean> getCommentData(@Query("docurl")String formatDocUrl,@Query("type")String type);

    //用户等级
    //http://cdn.user.iclient.ifeng.com/api_user_exp/list/10xbtq-17fezz-whn4a-tkfu4-13jo48.v2.html

    //专题
    //http://api.iclient.ifeng.com/TopicApiForCmpp?topicid=391&json=y&ad=y&gv=5.4.1&av=5.4.1&uid=864394010382135&deviceid=864394010382135&proid=ifengnews&os=android_19&df=androidphone&vt=5&screen=720x1280&publishid=2011&nw=wifi


}
