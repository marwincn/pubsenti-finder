package cn.marwin.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.common.Term;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SegmentUtil {
    // 自定义词典
    public static final String CUSTOMWORDS_PATH = "/Users/mdah/Playground/Senti-Corpus/customwords.txt";
    // 停用词表
    public static final String STOPWORDS_PATH = "/Users/mdah/Playground/Senti-Corpus/cn_stopwords.txt";
    public static Set<String> stopWords = new HashSet<>();
    // 否定词表
    public static final String NONWORDS_PATH = "/Users/mdah/Playground/Senti-Corpus/cn_nonwords.txt";
    public static Set<String> nonWords = new HashSet<>();
    // 分割符集合
    public static final Set<String> SPLITWORDS;
    // 不可见字符
    public static final String REGREX = "[\\u200b-\\u200f]|[\\u200e-\\u200f]|[\\u202a-\\u202e]|[\\u2066-\\u2069]|\ufeff|\u06ec";

    /**
     * 加载辅助词表，辅助词表只会影响最后分析的准确度，不会导致发生错误
     */
    static {
        try {
            stopWords.addAll(FileUtil.fileToList(STOPWORDS_PATH));
        } catch (IOException e) {
            System.err.println("停用词表加载失败：" + STOPWORDS_PATH);
        }

        try {
            nonWords.addAll(FileUtil.fileToList(NONWORDS_PATH));
        } catch (IOException e) {
            System.err.println("否定词表加载失败：" + NONWORDS_PATH);
        }

        try {
            FileUtil.fileToList(CUSTOMWORDS_PATH).forEach(CustomDictionary::add);
        } catch (IOException e) {
            System.err.println("自定义词表加载失败：" + CUSTOMWORDS_PATH);
        }

        SPLITWORDS = Stream.of(",", ".", "!", "?", "，", "。", "！", "？", "[").collect(Collectors.toSet());
    }

    /**
     * 重写的分词方法，去除停用词，转换否定词
     */
    public static Set<String> segment(String text) {
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
            if (SPLITWORDS.contains(word) || word.trim().isEmpty()) {
                if (tag == -1) {
                    tag = -tag;
                }
                continue;
            }
            // 剔除停用词，不可见字符和人名（nr词性表示人名）
            if (stopWords.contains(word) || word.matches(REGREX) || t.nature.startsWith("nr")) {
                continue;
            }
            // 转换否定词，添加N前缀
            if (tag == -1) {
                word = "N" + word;
            }
            // 添加词性后缀
            words.add(word + t.nature);
        }
        return words;
    }
}
