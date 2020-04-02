package cn.marwin.classifier;

import com.hankcs.hanlp.classification.classifiers.IClassifier;
import com.hankcs.hanlp.classification.classifiers.NaiveBayesClassifier;
import com.hankcs.hanlp.classification.corpus.FileDataSet;
import com.hankcs.hanlp.classification.corpus.IDataSet;
import com.hankcs.hanlp.classification.models.NaiveBayesModel;
import com.hankcs.hanlp.classification.tokenizers.HanLPTokenizer;

import java.io.*;
import java.util.Map;

public class HanLPClassifier {
    private static final String CORPUS_PATH = "";

    private static final String MODEL_PATH = "";

    private static IClassifier classifier;

    static {
        NaiveBayesModel model = (NaiveBayesModel) readObject(MODEL_PATH);

        if (model == null) {
            System.out.println("找不到模型文件，开始从语料库训练模型。");
            IDataSet trainingCorpus = null;
            try {
                trainingCorpus = new FileDataSet()
                        .setTokenizer(new HanLPTokenizer()) // 分词器
                        .load(CORPUS_PATH, "UTF-8");
            } catch (IOException e) {
                System.err.println("找不到语料库文件！");
                System.err.println(e.getMessage());
            }

            classifier = new NaiveBayesClassifier(); // 创建分类器
            classifier.train(trainingCorpus); // 内部使用朴素叶贝斯法

            saveObject(classifier.getModel(), MODEL_PATH);
        } else {
            System.out.println("已读取文本分类模型。");
            classifier = new NaiveBayesClassifier(model);
        }

    }

    public static Double getScore(String text) {
        Map<String, Double> map = classifier.predict(text);
        return map.get("pos") - map.get("neg");
    }

    /*
     * 序列化模型
     */
    private static boolean saveObject(Object o, String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(o);
        } catch (FileNotFoundException e) {
            System.err.println("文件不存在");
            System.err.println(e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    /*
     * 反序列化模型
     */
    private static Object readObject(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            Object o = ois.readObject();
            return o;
        } catch (FileNotFoundException e) {
            System.err.println("文件不存在");
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

}
