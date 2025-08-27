package media.toloka.rfa.banner.model.enumerate;

public enum EBannerStyle {
    BANNER_TYPE_0_0px("--- Відсутній ---",0, 0),
    BANNER_TYPE_300_250px("Medium Rectangle",300, 250),
    BANNER_TYPE_336_280px("Large Rectangle",300 , 250),
    BANNER_TYPE_728_90px("Leaderboard",728 , 90),
    BANNER_TYPE_970_90px("Large Leaderboard",970, 90),
    BANNER_TYPE_970_250px("Billboard",970 , 250),
    BANNER_TYPE_120_600px("Skyscraper",120 , 600),
    BANNER_TYPE_160_600px("Wide Skyscraper",160 , 600),
    BANNER_TYPE_300_600px("Half-Page",300 , 600),
    BANNER_TYPE_250_250px("Square",250 ,250),
    BANNER_TYPE_200_200px("Small Square",200 , 200),
    BANNER_TYPE_468_60px("Small Banner",468 , 60),
    BANNER_TYPE_240_400px("Vertical Rectangle",240 , 400),
    BANNER_TYPE_980_120px("Panorama",980 , 120),
    BANNER_TYPE_320_50px("Mobile Banner",320 , 50),
    BANNER_TYPE_320_100px("Mobile Leaderboard",320 , 100),
    BANNER_TYPE_320_250px("Large Mobile Banner",320 , 250),
    BANNER_TYPE_AUDIO("AUDIO",0,0),
    BANNER_TYPE_IMAGE("IMAGE",0,0);

    public final String label;
    public final Integer h;
    public final Integer v;

    private EBannerStyle(String label,Integer h, Integer v) {
        this.label = label;
        this.h = h;
        this.v = v;
    }
}
