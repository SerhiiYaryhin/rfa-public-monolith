package media.toloka.rfa.service;

//public class DownloadFileException {
public class DownloadFileException extends Exception {
    public DownloadFileException(String message) {
        super(message);
    }

    public DownloadFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

//}
