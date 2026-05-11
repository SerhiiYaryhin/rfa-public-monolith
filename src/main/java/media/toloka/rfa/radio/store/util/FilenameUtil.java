package media.toloka.rfa.radio.store.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class FilenameUtil {

    private static final Map<Character, String> cyrillicToLatinMap = new HashMap<>();

    static {
        cyrillicToLatinMap.put('а', "a"); cyrillicToLatinMap.put('б', "b"); cyrillicToLatinMap.put('в', "v");
        cyrillicToLatinMap.put('г', "h"); cyrillicToLatinMap.put('ґ', "g"); cyrillicToLatinMap.put('д', "d");
        cyrillicToLatinMap.put('е', "e"); cyrillicToLatinMap.put('є', "ye"); cyrillicToLatinMap.put('ж', "zh");
        cyrillicToLatinMap.put('з', "z"); cyrillicToLatinMap.put('и', "y"); cyrillicToLatinMap.put('і', "i");
        cyrillicToLatinMap.put('ї', "yi"); cyrillicToLatinMap.put('й', "y"); cyrillicToLatinMap.put('к', "k");
        cyrillicToLatinMap.put('л', "l"); cyrillicToLatinMap.put('м', "m"); cyrillicToLatinMap.put('н', "n");
        cyrillicToLatinMap.put('о', "o"); cyrillicToLatinMap.put('п', "p"); cyrillicToLatinMap.put('р', "r");
        cyrillicToLatinMap.put('с', "s"); cyrillicToLatinMap.put('т', "t"); cyrillicToLatinMap.put('у', "u");
        cyrillicToLatinMap.put('ф', "f"); cyrillicToLatinMap.put('х', "kh"); cyrillicToLatinMap.put('ц', "ts");
        cyrillicToLatinMap.put('ч', "ch"); cyrillicToLatinMap.put('ш', "sh"); cyrillicToLatinMap.put('щ', "shch");
        cyrillicToLatinMap.put('ь', ""); cyrillicToLatinMap.put('ю', "yu"); cyrillicToLatinMap.put('я', "ya");
        
        cyrillicToLatinMap.put('А', "A"); cyrillicToLatinMap.put('Б', "B"); cyrillicToLatinMap.put('В', "V");
        cyrillicToLatinMap.put('Г', "H"); cyrillicToLatinMap.put('Ґ', "G"); cyrillicToLatinMap.put('Д', "D");
        cyrillicToLatinMap.put('Е', "E"); cyrillicToLatinMap.put('Є', "Ye"); cyrillicToLatinMap.put('Ж', "Zh");
        cyrillicToLatinMap.put('З', "Z"); cyrillicToLatinMap.put('И', "Y"); cyrillicToLatinMap.put('І', "I");
        cyrillicToLatinMap.put('Ї', "Yi"); cyrillicToLatinMap.put('Й', "Y"); cyrillicToLatinMap.put('К', "K");
        cyrillicToLatinMap.put('Л', "L"); cyrillicToLatinMap.put('М', "M"); cyrillicToLatinMap.put('Н', "N");
        cyrillicToLatinMap.put('О', "O"); cyrillicToLatinMap.put('П', "P"); cyrillicToLatinMap.put('Р', "R");
        cyrillicToLatinMap.put('С', "S"); cyrillicToLatinMap.put('Т', "T"); cyrillicToLatinMap.put('У', "U");
        cyrillicToLatinMap.put('Ф', "F"); cyrillicToLatinMap.put('Х', "Kh"); cyrillicToLatinMap.put('Ц', "Ts");
        cyrillicToLatinMap.put('Ч', "Ch"); cyrillicToLatinMap.put('Ш', "Sh"); cyrillicToLatinMap.put('Щ', "Shch");
        cyrillicToLatinMap.put('Ь', ""); cyrillicToLatinMap.put('Ю', "Yu"); cyrillicToLatinMap.put('Я', "Ya");
        
        // Russian specific
        cyrillicToLatinMap.put('ы', "y"); cyrillicToLatinMap.put('э', "e"); cyrillicToLatinMap.put('ё', "yo");
        cyrillicToLatinMap.put('Ы', "Y"); cyrillicToLatinMap.put('Э', "E"); cyrillicToLatinMap.put('Ё', "Yo");
        cyrillicToLatinMap.put('ъ', ""); cyrillicToLatinMap.put('Ъ', "");
    }

    public static String transliterate(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String replacement = cyrillicToLatinMap.get(c);
            if (replacement != null) {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String sanitize(String filename) {
        if (filename == null) return null;
        
        // Transliterate cyrillic
        String result = transliterate(filename);
        
        // Replace everything else that's not Latin, digit, dot, underscore or dash
        result = result.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Avoid multiple underscores
        result = result.replaceAll("__+", "_");
        
        return result;
    }

    public static boolean isLatin(String filename) {
        if (filename == null) return true;
        return filename.matches("^[a-zA-Z0-9._-]+$");
    }
}
