package cn.marwin.entity;

public class Comment {
    // 元数据
    private String text;
    private String time;
    private Integer like;

    // 加工信息
    private Double score;

    public Comment() {}

    public Comment(String text, String time, Integer like) {
        this.text = text;
        this.time = time;
        this.like = like;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getLike() {
        return like;
    }

    public void setLike(Integer like) {
        this.like = like;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
