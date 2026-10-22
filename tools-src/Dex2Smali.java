import com.android.tools.smali.baksmali.Baksmali;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;

import java.io.File;
import java.nio.file.Files;

/**
 * Дизассемблирует classes.dex в smali, используя baksmali из apktool.jar.
 * Запуск: java -cp apktool.jar Dex2Smali.java <classes.dex> <out-dir> <api>
 */
public class Dex2Smali {
    public static void main(String[] args) throws Exception {
        byte[] dex = Files.readAllBytes(new File(args[0]).toPath());
        int api = Integer.parseInt(args[2]);
        DexBackedDexFile file = new DexBackedDexFile(new Opcodes(api, -1), dex, 0);
        BaksmaliOptions options = new BaksmaliOptions();
        options.sequentialLabels = true;
        Baksmali.disassembleDexFile(file, new File(args[1]), 1, options);
    }
}
