package cn.marwin.classifier;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Train {

    // 停用词表
    public static final String STOPWORDS_PATH = "/Users/mdah/Playground/Senti-Corpus/cn_stopwords.txt";
    public static Set<String> stopWords = new HashSet<>();
    // 否定词表
    public static final String NONWORDS_PATH = "/Users/mdah/Playground/Senti-Corpus/cn_nonwords.txt";
    public static Set<String> nonWords = new HashSet<>();

    // 文档列表
    public static final String POS_DOCS_PATH = "/Users/mdah/Playground/Senti-Corpus/weibo/pos.txt";
    public static ArrayList<String> posDocs = new ArrayList<>();
    public static final String NEG_DOCS_PATH = "/Users/mdah/Playground/Senti-Corpus/weibo/neg.txt";
    public static ArrayList<String> negDocs = new ArrayList<>();

    // 文档中每个词，及在文档中出现的次数
    public static HashMap<String, Integer> posWordsMap = new HashMap<>();
    public static HashMap<String, Integer> negWordsMap = new HashMap<>();

    // 从词中选出的特征及其卡方值
    public static HashMap<String, BigDecimal> featureMap = new HashMap<>();

    /**
     * 获取模型
     */
    public static Model getModel() {
        Model model = new Model(posDocs.size(), negDocs.size(), posWordsMap, negWordsMap, featureMap);
        return model;
    }

    /**
     * 加载语料库。
     */
    public static void loadCorpus() throws IOException {
        Long start = System.currentTimeMillis();

        FileReader fileReader = new FileReader(POS_DOCS_PATH);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        while ((line = reader.readLine()) != null) {
            posDocs.add(line);
        }

        fileReader = new FileReader(NEG_DOCS_PATH);
        reader = new BufferedReader(fileReader);
        while ((line = reader.readLine()) != null) {
            negDocs.add(line);
        }

        System.out.println("语料库加载成功，耗时：" + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * 加载辅助词表，包括停用词和否定词
     */
    public static void loadAuxWords() throws IOException {
        Long start = System.currentTimeMillis();

        FileReader fileReader = new FileReader(STOPWORDS_PATH);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        while ((line = reader.readLine()) != null) {
            stopWords.add(line);
        }

        fileReader = new FileReader(NONWORDS_PATH);
        reader = new BufferedReader(fileReader);
        while ((line = reader.readLine()) != null) {
            nonWords.add(line);
        }

        System.out.println("停用词库加载成功，耗时：" + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * 对每个文档进行分词，计算词在所有文档里出现的次数，忽略单个文档里的词频
     */
    public static void segmentDocs() {
        Long start = System.currentTimeMillis();

        posDocs.stream().map(l -> segment(l)).forEach(words -> {
            for (String word: words) {
                if (posWordsMap.containsKey(word)) {
                    // 忽略了同步问题
                    posWordsMap.put(word, posWordsMap.get(word) + 1);
                } else {
                    posWordsMap.put(word, 1);
                }
            }
        });

        negDocs.stream().map(l -> segment(l)).forEach(words -> {
            for (String word: words) {
                if (negWordsMap.containsKey(word)) {
                    // 忽略了同步问题
                    negWordsMap.put(word, negWordsMap.get(word) + 1);
                } else {
                    negWordsMap.put(word, 1);
                }
            }
        });

        System.out.println("文档分词完成，耗时：" + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * 计算每个词的卡方值，确定特征词
     */
    public static void extractFeatures() {
        Long start = System.currentTimeMillis();

        int posDocsSize = posDocs.size(); // pos文档数
        int negDocsSize = negDocs.size(); // neg文档数

        Set<String> posWords = posWordsMap.keySet();
        Set<String> negWords = negWordsMap.keySet();
        Set<String> words = new HashSet<>();
        // 取pos和neg词语集合的并集，得到语料库里所有的非停用词
        words.addAll(posWords);
        words.addAll(negWords);

        words.stream().forEach(word -> {
            // 包含word的pos文档数
            int posTimes = posWordsMap.getOrDefault(word, 0);
            // 包含word的neg文档数
            int negTimes = negWordsMap.getOrDefault(word, 0);

            // 计算2x2表格的卡方值
            // todo: improve this arithmetic
            BigDecimal a = new BigDecimal(posTimes);
            BigDecimal b = new BigDecimal(negTimes);
            BigDecimal c = new BigDecimal(posDocsSize - posTimes);
            BigDecimal d = new BigDecimal(negDocsSize - negTimes);

            BigDecimal x = a.multiply(d).subtract(b.multiply(c)).pow(2).multiply(new BigDecimal(posDocsSize + negDocsSize));
            BigDecimal y = a.add(b).multiply(c.add(d)).multiply(a.add(c)).multiply(b.add(d));
            BigDecimal chi = x.divide(y, 6);

            // 选择95%置信度下的特征词
            if (chi.compareTo(new BigDecimal(3.84)) > 0) {
                featureMap.put(word, chi);
            }
        });

        System.out.println("计算卡方值成功，耗时：" + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * 重写的分词方法，去除停用词，转换否定词
     */
    public static Set<String> segment(String text) {
        Set<String> splitWords = Stream.of(",", ".", "!", "?", "，", "。", "！", "？", " ").collect(Collectors.toSet());
        HashSet<String> words = new HashSet<>(); // 使用set忽略了词频
        int tag = 1; // 标记，遇到否定词标记取反

        List<Term> termList = HanLP.segment(text);
        for (Term t: termList) {
            String word = t.word;
            // 遇到否定词
            if (nonWords.contains(word)) {
                tag = -tag;
                continue;
            }
            // 遇到分隔符，中断否定
            if (tag == -1 && splitWords.contains(word)) {
                tag = -tag;
                continue;
            }
            // 剔除停用词
            if (stopWords.contains(word)) {
                continue;
            }
            // 转换否定词
            if (tag == -1) {
                word = "N" + word;
            }
            words.add(word);
        }
        return words;
    }

}
