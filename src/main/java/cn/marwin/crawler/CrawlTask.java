package cn.marwin.crawler;

import cn.marwin.classifier.MyClassifier;
import cn.marwin.entity.Weibo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.marwin.entity.Comment;
import cn.marwin.entity.Hot;
import redis.clients.jedis.Jedis;
import cn.marwin.util.RedisUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class CrawlTask extends TimerTask {
    public static final int HOT_LIST_SIZE = 10;          // 从热搜榜上获取的热搜数量
    public static final int WB_LIST_SIZE = 1;            // 每条热搜获取的weibo数量
    public static final int CM_LIST_SIZE = 5;            // 每条weibo获取的评论页数，一页最多20条评论
    public static final int KEY_EXPIRE_TIME = 60 * 60;   // redis里key的过期时间，单位为秒

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();
            System.out.println("### 开始爬取微博评论 ###");
            crawl();
            long end = System.currentTimeMillis();
            System.out.println("### 爬取微博结束，耗时：" + ((end - start) % 1000.0) + "s ###");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void crawl() throws InterruptedException{
        long timestamp = System.currentTimeMillis();
        Jedis jedis = RedisUtil.getJedis();

        // 从热搜榜的请求里获取xhr的链接
        String url = "https://m.weibo.cn/api/container/getIndex?containerid=106003type%3D25%26t%3D3%26disable_hot%3D1%26filter_type%3Drealtimehot&title=%E5%BE%AE%E5%8D%9A%E7%83%AD%E6%90%9C";
        List<Hot> hotList = WeiboParser.getHotList(url, HOT_LIST_SIZE);
        int i = 1;
        for (Hot hot: hotList) {
            String hot_key = timestamp + ":hot:" + (i++); // timestamp:hot:{index}
            insertHot(jedis, hot_key, hot);

            List<Weibo> weiboList = WeiboParser.getWeiboList(hot, WB_LIST_SIZE);
            // 汇总该热搜下所有weibo的评论情况
            int allPosCount = 0;
            int allNegCount = 0;
            int j = 1;
            for (Weibo weibo : weiboList) {
                String wb_key = hot_key + ":wb:" + (j++); // timestamp:hot:{index}:wb:{index}
                insertWeibo(jedis, wb_key, weibo);

                List<Comment> commentList = WeiboParser.getCommentList(weibo, CM_LIST_SIZE);
                String cm_key = wb_key + ":cm";           // timestamp:hot:{index}:wb:{index}:cm
                // 统计该weibo下评论的情况
                int posCount = 0;
                int negCount = 0;
                int otherCount = 0;
                for (Comment comment: commentList) {
                    // 使用分类器评估评论的得分
                    double score = MyClassifier.getScore(comment.getText());
                    comment.setScore(score);
                    insertComment(jedis, cm_key, comment);

                    if (score > 0) {
                        posCount++;
                        allPosCount++;
                    } else if (score < 0) {
                        negCount++;
                        allNegCount++;
                    } else {
                        otherCount++;
                    }
                }
                jedis.expire(cm_key, KEY_EXPIRE_TIME);

                // 追加设置weibo的加工信息
                jedis.hsetnx(wb_key, "posCount", "" + posCount);
                jedis.hsetnx(wb_key, "negCount", "" + negCount);
                jedis.hsetnx(wb_key, "otherCount", "" + otherCount);
                System.out.println("posCount: " + posCount + ", negCount: " + negCount + ", otherCount: " + otherCount);

                //为了防止爬取过快导致403或触发反爬，每个微博爬完暂停3s
                Thread.sleep(3000);
            }
            // 追加设置hot的加工信息
            double status = (allPosCount + allNegCount) == 0 ? 0 : 1.0 * (allPosCount - allNegCount) / (allPosCount + allNegCount);
            jedis.hsetnx(hot_key, "status", "" + status);
        }

        jedis.lpush("timestamp", "" + timestamp); // 逆序存储，最新的时间在最左侧
        RedisUtil.returnResource(jedis);
    }

    private void insertHot(Jedis jedis, String key, Hot hot) {
        Map<String, String> hot_value = new HashMap<>();
        hot_value.put("desc", hot.getDesc());
        hot_value.put("scheme", hot.getScheme());
        jedis.hmset(key, hot_value);
        jedis.expire(key, KEY_EXPIRE_TIME);
        System.out.println("Hot Insert: " + hot.getDesc());
    }

    private void insertWeibo(Jedis jedis, String key, Weibo weibo) {
        Map<String, String> wb_value = new HashMap<>();
        wb_value.put("id", weibo.getId());
        wb_value.put("url", weibo.getUrl());
        wb_value.put("user", weibo.getUser());
        wb_value.put("pic", weibo.getPic());
        wb_value.put("time", weibo.getTime());
        wb_value.put("content", weibo.getContent());
        jedis.hmset(key, wb_value);
        jedis.expire(key, KEY_EXPIRE_TIME);
        System.out.println("Weibo Insert: " + weibo.getContent());
    }

    private void insertComment(Jedis jedis, String key, Comment comment) {
        // 序列化为Json存储
        ObjectMapper mapper = new ObjectMapper();
        String cm_json = null;
        try {
            cm_json = mapper.writeValueAsString(comment);
            jedis.rpush(key, cm_json);
        } catch (JsonProcessingException e) {
            System.out.println("Comment序列化失败！");
            e.printStackTrace();
        }
    }
}
