package me.unariginal.dexrewards.utils;

import me.unariginal.dexrewards.DexRewards;

import java.io.*;

public class ConfigUtils {
    public static void create(File file, String path) throws IOException {
        file.createNewFile();

        InputStream stream = DexRewards.class.getResourceAsStream(path);
        assert stream != null;
        OutputStream out = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = stream.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }

        stream.close();
        out.close();
    }
}
