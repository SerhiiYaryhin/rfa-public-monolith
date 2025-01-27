package media.toloka.rfa.service;


import java.io.InputStream;

/**
 * Клас для представлення результату завантаження: вхідний потік та ім'я файлу.
 */
//    public static class DownloadResult {
public class DownloadFileResult {
    public final InputStream inputStream;
    public final String fileName;

    public DownloadFileResult(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        this.fileName = fileName;
    }
}

