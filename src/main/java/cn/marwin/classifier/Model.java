package cn.marwin.classifier;

import cn.marwin.util.SegmentUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class Model implements Serializable {

    private static final long serialVersionUID = 1L;

    // 训练时用到的文档数，计算概率时需要
    public double posDocsSize;
    public double negDocsSize;

    // 文档中每个词，及在文档中出现的次数
    public HashMap<String, Integer> posWordsMap;
    public HashMap<String, Integer> negWordsMap;

    // 从词中选出的特征及其卡方值
    public HashMap<String, BigDecimal> featureMap;

    public Model(double posDocsSize, double negDocsSize, HashMap<String, Integer> posWordsMap, HashMap<String, Integer> negWordsMap, HashMap<String, BigDecimal> featureMap) {
        this.posDocsSize = posDocsSize;
        this.negDocsSize = negDocsSize;
        this.posWordsMap = posWordsMap;
        this.negWordsMap = negWordsMap;
        this.featureMap = featureMap;
    }

    /**
     * 朴素贝叶斯算法
     * @return pos概率和neg概率的差值
     */
    public double nb(String text) {
        Set<String> features = getFeatures(text);

        // 文档里无特征无法判断
        if (features.size() == 0) {
            return 0;
        }

        // 先验概率
        double priorOfPos = posDocsSize / (posDocsSize + negDocsSize);
        double priorOfNeg = negDocsSize / (posDocsSize + negDocsSize);

        double probabilityOfPos = priorOfPos;
        for (String f: features) {
            int posTimes = posWordsMap.containsKey(f) ? posWordsMap.get(f) : 0;
            // 拉普拉斯平滑后的条件概率
            double evidence = 1.0 * (posTimes + 1) / (posDocsSize + 2);
            probabilityOfPos *= evidence;
        }
        double probabilityOfNeg = priorOfNeg;
        for (String f: features) {
            int negTimes = negWordsMap.containsKey(f) ? negWordsMap.get(f) : 0;
            // 拉普拉斯平滑后的条件概率
            double evidence = 1.0 * (negTimes + 1) / (negDocsSize + 2);
            probabilityOfNeg *= evidence;
        }

        return probabilityOfPos - probabilityOfNeg;
    }

    /**
     * 打印模型里所有特征
     */
    public void printFeatures() {
        Set<String> keys = featureMap.keySet();
        System.out.println("特征词  卡方值  pos文档次数  neg文档次数");
        for (String key: keys) {
            System.out.println(key + "  " + featureMap.get(key) + "  " + posWordsMap.getOrDefault(key, 0) +
                    "  " + negWordsMap.getOrDefault(key, 0));
        }
    }

    /**
     * 提取输入文本的特征词
     */
    private Set<String> getFeatures(String text) {
        Set<String> words = SegmentUtil.segment(text);
        HashSet<String> features = new HashSet<>();
        for (String word: words) {
            if (featureMap.containsKey(word)) {
                features.add(word);
            }
        }
        return features;
    }
}
