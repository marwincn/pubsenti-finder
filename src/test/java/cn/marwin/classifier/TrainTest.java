package cn.marwin.classifier;

import com.hankcs.hanlp.HanLP;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

class TrainTest {

    /**
     * 测试模型准确率
     * @throws IOException
     */
    @Test
    void test() throws IOException {
        // 测试文档路径
        String posPath = "/Users/mdah/Playground/Senti-Corpus/test/pos.txt";
        String negPath = "/Users/mdah/Playground/Senti-Corpus/test/neg.txt";
        ArrayList<String> posComments = new ArrayList<>();
        ArrayList<String> negComments = new ArrayList<>();

        FileReader fileReader = new FileReader(posPath);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        while ((line = reader.readLine()) != null) {
            posComments.add(line);
        }

        fileReader = new FileReader(negPath);
        reader = new BufferedReader(fileReader);
        while ((line = reader.readLine()) != null) {
            negComments.add(line);
        }

        MyClassifier.init();
        int count = 0;
        for (String comment: posComments) {
            System.out.println(comment);
            // 将结果转换为
            double p = MyClassifier.getScore(comment);
            System.out.println("情感分析结果为：" + p);
            if (p > 0) { count++; }
        }

        for (String comment: negComments) {
            System.out.println(comment);
            double p = MyClassifier.getScore(comment);
            System.out.println("情感分析结果为：" + p);
            if (p < 0) { count++; }
        }

        double result = 1.0 * count / (posComments.size() + negComments.size());
        System.out.println("模型在测试集上的准确率为：" + result);
    }

    /**
     * 测试对否定词的分词情况
     */
    void segement() throws IOException {
        Train.loadAuxWords();
        System.out.println(HanLP.segment("我今天不高兴，我们不要做坏事"));
        System.out.println(Train.segment("我今天不高兴，我们不要做坏事"));
        System.out.println(HanLP.segment("我不酸，我不是不喜欢你"));
        System.out.println(Train.segment("我不酸，我不是不喜欢你"));
        System.out.println(HanLP.segment("这是非人道的，非常低级且不要脸，我没兴趣"));
        System.out.println(Train.segment("这是非人道的，非常低级且不要脸，我没兴趣"));
        System.out.println(HanLP.segment("天呐，我无法赞同这个观点，不能学习他，不然不会得到别人的认可，不要相信"));
        System.out.println(Train.segment("天呐，我无法赞同这个观点，不能学习他，不然不会得到别人的认可，不要相信"));
    }
}