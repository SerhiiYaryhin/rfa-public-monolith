package media.toloka.rfa.radio.store.Interface;

import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.Store;

import java.io.InputStream;

public interface StoreInterface {

    public InputStream GetFileFromStore (String uuid);
    public String PutFileToStore (InputStream inputStream, String filepatch, Clientdetail cd, EStoreFileType storeFileType);
//    public Boolean DeleteFileInStore (String uuid);
    // Видаляємо те, що зберігалося у сховищі
    public Boolean DeleteInStore (Store store);

}
