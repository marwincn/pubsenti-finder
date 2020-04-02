package cn.marwin.entity;

import java.util.List;

public class Hot {
    // 元数据
    private String desc;
    private String scheme;

    // 加工信息
    private double status;
    private List<Weibo> weiboList;

    public Hot() {}

    public Hot(String desc, String scheme) {
        this.desc = desc;
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    public List<Weibo> getWeiboList() {
        return weiboList;
    }

    public void setWeiboList(List<Weibo> weiboList) {
        this.weiboList = weiboList;
    }

    public double getStatus() {
        return status;
    }

    public void setStatus(double status) {
        this.status = status;
    }
}
