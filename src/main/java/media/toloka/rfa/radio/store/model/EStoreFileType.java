package media.toloka.rfa.radio.store.model;

public enum EStoreFileType {

    STORE_FILE("Файл"), // 0
    STORE_DOCUMENT("Документ"), // 1
    STORE_TRACK("Трек"), // 2
    STORE_ALBUMCOVER("Обкладинка альбому"), // 3
    STORE_POSTCOVER("Обкладинка посту"), // 4
    STORE_PHOTO("Фото профайлу"), // 5
    STORE_PODCASTCOVER("Обкладинка подкасту"), // 6
    STORE_EPISODECOVER_FREE("Обкладинка епізоду"), // 7 // не використовую. Замісь неї використовую STORE_PODCASTCOVER
    STORE_EPISODETRACK("Трек епізоду"), // 8
    STORE_RESERV_1("Резерв"), // 9
    STORE_PHOTOALBUMS("Фото Альбому"), // 10
    STORE_TTS("Текст в голос"), // 11
    STORE_DUMMY("Невизначений тип у сховищі"), // 12
    STORE_BANNERIMAGE("Зображення баннера"), // 13
    STORE_BANNERAUDIO("Аудіо баннер"), // 14
    STORE_BANNERVIDEO("Відео баннер"); // 15
    public final String label;

    private EStoreFileType(String label) {
        this.label = label;
    }
}
