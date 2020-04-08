package cn.marwin.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    public static List<String> fileToList(String path) throws IOException {
        List<String> list = new ArrayList<>();
        FileReader fileReader = new FileReader(path);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        while ((line = reader.readLine()) != null) {
            list.add(line);
        }
        return list;
    }
}
