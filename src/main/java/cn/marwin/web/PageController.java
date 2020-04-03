package cn.marwin.web;

import cn.marwin.crawler.CrawlTask;
import cn.marwin.entity.Comment;
import cn.marwin.entity.Hot;
import cn.marwin.entity.Weibo;
import cn.marwin.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PageController {

    @RequestMapping("/")
    public String index(Model model) {
        Jedis jedis = RedisUtil.getJedis();
        String timestamp = jedis.lindex("timestamp", 0);

        List<Hot> hotList = new ArrayList<>();
        for (int i = 1; i <= CrawlTask.HOT_LIST_SIZE; i++) {
            String hot_key = timestamp + ":hot:" + i;
            List<String> fields = jedis.hmget(hot_key, "desc", "scheme", "status");
            Hot hot = new Hot(fields.get(0), fields.get(1));
            if (fields.get(2) != null) {
                hot.setStatus(Double.valueOf(fields.get(2)));
            }

            List<Weibo> weiboList = new ArrayList<>();
            for (int j = 1; j <= CrawlTask.WB_LIST_SIZE; j++) {
                String wb_key = hot_key + ":wb:" + 1;
                fields = jedis.hmget(wb_key, "id", "user", "pic", "time", "url", "content", "posCount", "negCount", "otherCount");
                Weibo weibo = new Weibo(fields.get(0), fields.get(1), fields.get(2), fields.get(3), fields.get(4), fields.get(5));
                weibo.setKey(wb_key);
                if (fields.get(6) != null && fields.get(7) != null && fields.get(8) != null) {
                    weibo.setPosCount(Integer.valueOf(fields.get(6)));
                    weibo.setNegCount(Integer.valueOf(fields.get(7)));
                    weibo.setOtherCount(Integer.valueOf(fields.get(8)));
                }

                String cm_key = wb_key + ":cm";
                List<String> cms = jedis.lrange(cm_key, 0, 10);
                List<Comment> comments = new ArrayList<>();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    for (String c: cms) {
                            Comment comment = mapper.readValue(c, Comment.class);
                            comments.add(comment);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                weibo.setComments(comments);
                weiboList.add(weibo);
            }
            hot.setWeiboList(weiboList);
            hotList.add(hot);
        }

        RedisUtil.returnResource(jedis);
        model.addAttribute("hotList", hotList);
        return "index";
    }

    @RequestMapping("/detail")
    public String detail(String key, Model model) {
        Jedis jedis = RedisUtil.getJedis();

        List<String> fields = jedis.hmget(key, "id", "user", "pic", "time", "url", "content", "posCount", "negCount", "otherCount");
        Weibo weibo = new Weibo(fields.get(0), fields.get(1), fields.get(2), fields.get(3), fields.get(4), fields.get(5));
        weibo.setKey(key);
        if (fields.get(6) != null && fields.get(7) != null && fields.get(8) != null) {
            weibo.setPosCount(Integer.valueOf(fields.get(6)));
            weibo.setNegCount(Integer.valueOf(fields.get(7)));
            weibo.setOtherCount(Integer.valueOf(fields.get(8)));
        }

        String cm_key = key + ":cm";
        List<String> cms = jedis.lrange(cm_key, 0, 100);
        List<Comment> comments = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            for (String c: cms) {
                Comment comment = mapper.readValue(c, Comment.class);
                comments.add(comment);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        RedisUtil.returnResource(jedis);
        model.addAttribute("weibo", weibo);
        model.addAttribute("comments", comments);
        return "detail";
    }

    @RequestMapping("/feedback")
    @ResponseBody
    public String feedback(String comment, String sentiment) {
        Jedis jedis = RedisUtil.getJedis();
        if (sentiment.equals("pos") || sentiment.equals("neg")) {
            jedis.rpush("feedback:" + sentiment, comment);
        }
        RedisUtil.returnResource(jedis);
        return "反馈成功！";
    }
}
