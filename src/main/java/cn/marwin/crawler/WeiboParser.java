package cn.marwin.crawler;

import cn.marwin.entity.Weibo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.marwin.entity.Comment;
import cn.marwin.entity.Hot;
import cn.marwin.util.HttpUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeiboParser {

    /**
     * 请求获取热搜的链接，从返回的json中解析出每条热搜的信息
     * @param url 获取热搜列表api
     * @param size 获取热搜列表的条数
     * @return 热搜列表
     */
    public static List<Hot> getHotList(String url, int size) {
        JsonNode rootNode;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = HttpUtil.request(url);
            rootNode = mapper.readTree(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        JsonNode groups = rootNode.get("data").get("cards").get(0).get("card_group");

        Iterator<JsonNode> iterator = groups.elements();
        List<Hot> hotList = new ArrayList<>();
        int count = 0;
        while (iterator.hasNext()) {
            // 跳过置顶热搜
            if (count == 0) {
                count++;
                iterator.next();
                continue;
            }

            // 取前size条热搜，由于跳过了置顶热搜，count从1开始计算
            if (++count > size + 1) {break;}

            JsonNode node = iterator.next();
            String desc = node.get("desc").asText();
            String scheme = node.get("scheme").asText();
            Hot hot = new Hot(desc, scheme);
            hotList.add(hot);
        }

        return hotList;
    }

    /**
     * 请求热搜详情，返回单条热搜页面的相关热门微博
     * @param hot 从热搜列表获取的热搜信息
     * @param size 获取热门微博列表的条数
     * @return 该热搜下热门微博列表，3条
     */
    public static List<Weibo> getWeiboList(Hot hot, int size) {
        // 热搜页面数据为js动态渲染，需要将页面url替换为获取数据的api
        String api = hot.getScheme().replace("https://m.weibo.cn/search",
                "https://m.weibo.cn/api/container/getIndex") + "&page_type=searchall";

        List<Weibo> weiboList = new ArrayList<>();
        try {
            String json = HttpUtil.request(api);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);
            JsonNode cards = rootNode.get("data").get("cards");

            Iterator<JsonNode> iterator = cards.elements();
            int count = 0;
            while (iterator.hasNext()) {
                JsonNode card = iterator.next();

                // card_type = 9时才是微博
                int card_type = card.get("card_type").asInt();
                if (card_type != 9) {continue;}
                // 取前size条热门微博
                if (++count > size) {break;}

                String id = card.get("mblog").get("id").asText(); // id为每条微博的唯一标识
                String user = card.get("mblog").get("user").get("screen_name").asText();       // 该weibo博主的昵称
                String pic = card.get("mblog").get("user").get("profile_image_url").asText();  // 该weibo博主的头像
                String url = "https://m.weibo.cn/status/" + id;

                // 通过正则从html中获取相应的数据
                String html = HttpUtil.request(url); // 请求微博的详情页，获取微博全文和详情信息需要
                String time = getTime(html);
                String content = getContent(html);

                Weibo weibo = new Weibo(id, user, pic, time, url, content);
                weiboList.add(weibo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return weiboList;
    }

    /**
     * 请求一条微博的评论列表
     * @param weibo
     * @param pageSize 获取的评论页数目，至少取一页，一页20条评论
     * @return 评论列表
     */
    public static List<Comment> getCommentList(Weibo weibo, int pageSize) throws InterruptedException {
        List<Comment> commentList = new ArrayList<>();
        // 请求第一页评论，获取后续页面的参数
        String next = WeiboParser.parseComment(weibo, commentList, "0", "0");
        int count = 1;
        while (next != null && !next.equals("00")) {

            if (++count > pageSize) { break; }

            // 拆分 max_id 和 max_id_type
            String max_id_type = next.substring(0, 1);
            String max_id = next.substring(1);
            next = WeiboParser.parseComment(weibo, commentList, max_id, max_id_type);
        }

        return commentList;
    }

    /**
     * 解析微博评论列表，由于需要返回下一页参数且Java不支持元组，评论列表作为参数传入
     * @param weibo 微博
     * @param commentList 评论列表
     * @param max_id 初始页为"0"
     * @param max_id_type 初始页为"0"
     * @return 请求下一页评论的参数，返回null或"00"时表示已经到底
     */
    private static String parseComment(Weibo weibo, List<Comment> commentList, String max_id, String max_id_type) {
        String url = "https://m.weibo.cn/comments/hotflow?id=" + weibo.getId() + "&mid=" + weibo.getId() +
                "&max_id=" + max_id + "&max_id_type=" + max_id_type;

        JsonNode rootNode;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = HttpUtil.request(url);
            rootNode = mapper.readTree(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // 已经无评论
        if (!rootNode.has("data")) { return null; }

        // 获取下一页的请求参数
        String maxid = rootNode.get("data").get("max_id").toString();
        String maxidtype = rootNode.get("data").get("max_id_type").toString();

        JsonNode data = rootNode.get("data").get("data");
        Iterator<JsonNode> iterator = data.elements();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String time = node.get("created_at").asText();
            Integer like = node.get("like_count").asInt();
            String text = node.get("text").asText();

            text = processText(text); // 过滤html标签
            if (text.trim().equals("")) {continue;} // 跳过空评论
            if (text.equals("图片评论") || text.equals("转发微博")) {continue;} // 跳过无效评论

            Comment comment = new Comment(text, time, like);
            commentList.add(comment);
        }

        return maxidtype + maxid;
    }

    private static String getTime(String html) {
        Pattern pattern = Pattern.compile("\"created_at\": \".*\",");
        Matcher m = pattern.matcher(html);
        if (m.find()) {
            String create_at = m.group();
            return create_at.substring(15, create_at.length() - 13);
        }
        return "获取时间失败";
    }

    private static String getContent(String html) {
        Pattern pattern = Pattern.compile("\"text\": \".*\",");
        Matcher m = pattern.matcher(html);
        if (m.find()) {
            String text = m.group();
            String content = text.substring(9, text.length() - 2);

            // 过滤html标签
            return processText(content);
        }
        return "获取正文失败";
    }

    // todo: 去掉两个标签中间的@信息
    private static String processText(String text) {
        Pattern pattern = Pattern.compile("<[^>]+>");
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("");
    }
}
