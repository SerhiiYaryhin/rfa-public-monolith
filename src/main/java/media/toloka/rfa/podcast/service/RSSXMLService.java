package media.toloka.rfa.podcast.service;
// https://github.com/Podcast-Standards-Project/PSP-1-Podcast-RSS-Specification

// https://support.google.com/podcast-publishers/answer/9889544?hl=en

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.model.PodcastItunesCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
public class RSSXMLService {
    // Сервіс формування XML для подкасту
    /*
    Пробуємо знайти структуру програми, яка створює RSS для подкасту.

     */

    final Logger logger = LoggerFactory.getLogger(RSSXMLService.class);

    @Autowired
    private PodcastService podcastService;

    private Document document = null;
    private PodcastChannel podcastChannel;


    public String MakeRSSXMLService(PodcastChannel podcastChannel) {

        MakeXMLrss(podcastChannel);

        return XMLTOString();
    }

    private void MakeXMLrss(PodcastChannel podcastChannel) {
        MakeXML();
        // Формуємо простір імен для RSS
        Element rootRSS = document.createElement("rss");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:content", "http://purl.org/rss/1.0/modules/content/");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:dc", "http://purl.org/dc/elements/1.1/");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:wfw", "http://wellformedweb.org/CommentAPI/");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:atom", "http://www.w3.org/2005/Atom");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:sy", "http://purl.org/rss/1.0/modules/syndication/");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:slash", "http://purl.org/rss/1.0/modules/slash/");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:podcast", "https://podcastindex.org/namespace/1.0");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:rawvoice", "http://www.rawvoice.com/rawvoiceRssModule/");
        rootRSS.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:googleplay", "http://www.google.com/schemas/play-podcasts/1.0");
//
        rootRSS.setAttribute("version", "2.0");
        MakeXMLChanel(rootRSS,podcastChannel);
        document.appendChild(rootRSS);
        return;
    }

    private void MakeXMLChanel(Element rootRSS, PodcastChannel podcastChannel) {
        // Додаємо Chanel до RSS
        Element channel = document.createElement("channel");


//        Element title = document.createElement("title");
//        title.setTextContent(podcastChannel.getTitle());
        channel.appendChild(EChannelTitle(podcastChannel));
        channel.appendChild(EChannelAtomLink(podcastChannel));
        channel.appendChild(EChannelLink(podcastChannel));
        channel.appendChild(EChannelDescription(podcastChannel));
//         channel.appendChild(EChannelLastBuildDate(podcastChannel)); // Доопрацювати
        channel.appendChild(EChannelLanguage(podcastChannel));
        channel.appendChild(EChannelSy_updatePeriod(podcastChannel));
        channel.appendChild(EChannelSy_updateFrequency(podcastChannel));
        channel.appendChild(EChannelGenerator(podcastChannel));
        channel.appendChild(EChannelImage(podcastChannel));
        channel.appendChild(EChannel_Itunes_Summary(podcastChannel));
        channel.appendChild(EChannel_Itunes_Autor(podcastChannel));
        channel.appendChild(EChannel_Itunes_Explicit(podcastChannel)); // чутливий вміст
        channel.appendChild(EChannel_Itunes_Image(podcastChannel));
        channel.appendChild(EChannel_Itunes_Type(podcastChannel));
        channel.appendChild(EChannel_Itunes_Owner(podcastChannel));
        channel.appendChild(EChannelCopyright(podcastChannel));

        EChannel_Itunes_Category(podcastChannel,channel);



        List<PodcastItem> podcastItems = podcastChannel.getItem();
        // заповнюємо епізоди
        for (PodcastItem item : podcastItems) {
            channel.appendChild(AddItem(item));
        }

        rootRSS.appendChild(channel);
        return;
    }

    private void EChannel_Itunes_Category(PodcastChannel podcastChannel, Element channel) {
        for (PodcastItunesCategory podcastItunesCategory : podcastChannel.getItunescategory()) {
//            String first = podcastItunesCategory.getFirstlevel();
//            String second = podcastItunesCategory.getSecondlevel();
            if (podcastItunesCategory.getFirstlevel() != null && podcastItunesCategory.getFirstlevel().length() > 1) {
                Element elementFirst = document.createElement("itunes:category");
                elementFirst.setAttribute("text",podcastItunesCategory.getFirstlevel());
                if (podcastItunesCategory.getSecondlevel() != null && podcastItunesCategory.getSecondlevel().length() > 1) {
                    Element elementSecond = document.createElement("itunes:category");
                    elementSecond.setAttribute("text",podcastItunesCategory.getSecondlevel());
                    elementFirst.appendChild(elementSecond);
                }
                channel.appendChild(elementFirst);
            }
        }
    }

    private Node AddItem(PodcastItem item) {
        Element element = document.createElement("item");

//        Element title = document.createElement("title");
//        title.setTextContent(item.getTitle());
        element.appendChild(EItemTitle(item));

//        Element link = document.createElement("link");
//        link.setTextContent(item.getTitle());
        element.appendChild(EItemLink(item));
        element.appendChild(EItemGuid(item));
        element.appendChild(EItemComments(item));
        element.appendChild(EItemWfw_CommentRss(item));
        element.appendChild(EItemSlash_Comment(item));
//        element.appendChild(EItemCategory(item)); // https://github.com/ListenNotes/podcast-categories?tab=readme-ov-file
        element.appendChild(EItemDescription(item));
        element.appendChild(EItemContent_encoded(item));
        element.appendChild(EItemEnclosure(item));
        element.appendChild(EItemItunes_Duration(item));




        return element;
    }

    private Node EItemItunes_Duration(PodcastItem item) {
        Element element = document.createElement("itunes:duration");
        element.setTextContent(podcastService.GetTimeTrack(item.getStoreitem().getUuid()));
        return element;
    }

    private Node EItemEnclosure(PodcastItem item) {
        Element element = document.createElement("enclosure");
//        element.appendChild(document.createCDATASection(item.getDescription()));
        element.setAttribute("url","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
        element.setAttribute("length",item.getStoreitem().getFilelength().toString());
        element.setAttribute("type",item.getStoreitem().getContentMimeType());
//        element.setTextContent(item.getDescription());
        return element;
    }

    private Node EItemContent_encoded(PodcastItem item) {
        Element element = document.createElement("content:encoded");
        element.appendChild(document.createCDATASection(item.getDescription()));
//        element.setAttribute("isPermaLink","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
//        element.setTextContent(item.getDescription());
        return element;

    }

    private Node EItemDescription(PodcastItem item) {
        Element element = document.createElement("description");
//        element.appendChild(document.createCDATASection(item.getCategory()));
//        element.setAttribute("isPermaLink","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
        element.setTextContent(item.getDescription());
        return element;
    }

    private Node EItemCategory(PodcastItem item) {
        Element element = document.createElement("category");
//        List<PodcastItunesCategory> podcastItunesCategories = item.getChanel().getItunescategory();
//        String ttt = item.getCategory();
//        if (ttt != null) {
//            element.appendChild(document.createCDATASection(ttt));
//        } else {
//            element.appendChild(document.createCDATASection("Toloka"));
//        }
//        element.appendChild(document.createCDATASection(ttt));
        return element;
    }

    private Node EItemSlash_Comment(PodcastItem item) {
        // podcastService
        Element element = document.createElement("slash:comments");
//        element.setAttribute("isPermaLink","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
        element.setTextContent(podcastService.GetEpisodeNumberComments(item));
        return element;
    }

    private Node EItemWfw_CommentRss(PodcastItem item) {
        Element element = document.createElement("wfw:commentRss");
//        element.setAttribute("isPermaLink","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
        element.setTextContent("https://rfa.toloka.media/podcast/view/"+item.getUuid());
        return element;
    }

    private Node EItemComments(PodcastItem item) {
        Element element = document.createElement("guid");
//        element.setAttribute("isPermaLink","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
        element.setTextContent("https://rfa.toloka.media/podcast/view/"+item.getUuid());
        return element;
    }

    private Node EItemGuid(PodcastItem item) {
        Element element = document.createElement("guid");
        element.setAttribute("isPermaLink","https://rfa.toloka.media/podcast/rss/"+item.getUuid());
        element.setTextContent(item.getUuid());
        return element;
    }

    private Node EItemLink(PodcastItem item) {
        Element element = document.createElement("link");
        element.setTextContent(item.getTitle());
        return element;
    }

    private Node EItemTitle(PodcastItem item) {
        Element element = document.createElement("title");
        element.setTextContent(item.getTitle());
        return element;
    }


    private Node EChannelCopyright(PodcastChannel podcastChannel) {
        Element element = document.createElement("copyright");
        element.setTextContent("CC BY");
        return element;
    }

    private Node EChannel_Itunes_Owner(PodcastChannel podcastChannel) {
        Element element = document.createElement("itunes:owner");
        Element name = document.createElement("itunes:name");
        if (podcastChannel.getClientdetail().getFirmname() != null) {
            // чи правильно я перевіряю чи заповнено поле назви фірми?
            name.setTextContent(podcastChannel.getClientdetail().getFirmname());
        } else {
            name.setTextContent(podcastChannel.getClientdetail().getCustname()+" "+podcastChannel.getClientdetail().getCustsurname());
        }
        element.appendChild(name);
        Element email = document.createElement("itunes:email");
        email.setTextContent(podcastChannel.getClientdetail().getUser().getEmail());
        element.appendChild(email);
        return element;
    }

    private Node EChannel_Itunes_Type(PodcastChannel podcastChannel) {
        Element element = document.createElement("itunes:type");
        element.setTextContent("episodic");
        return element;
    }

    private Node EChannel_Itunes_Image(PodcastChannel podcastChannel) {
        Element element = document.createElement("itunes:image");
        element.setAttribute("href","https://rfa.toloka.media/store/thrumbal/w/300/"
                +podcastChannel.getImage().getStoreidimage().getUuid()+"/"
                +podcastChannel.getImage().getStoreidimage().getFilename());
        return element;
    }

    private Node EChannel_Itunes_Explicit(PodcastChannel podcastChannel) {
        Element element = document.createElement("itunes:explicit");
        if (podcastChannel.getExplicit() == false) {
            element.setTextContent("false");
        } else {
        element.setTextContent("true");
        }

        return element;
    }

    private Node EChannel_Itunes_Autor(PodcastChannel podcastChannel) {
        Element element = document.createElement("itunes:author");
        if (podcastChannel.getClientdetail().getFirmname() != null) {
            // чи правильно я перевіряю чи заповнено поле назви фірми?
            element.setTextContent(podcastChannel.getClientdetail().getFirmname());
        } else {
            element.setTextContent(podcastChannel.getClientdetail().getCustname()+" "+podcastChannel.getClientdetail().getCustsurname());
        }
        return element;
    }

    private Node EChannel_Itunes_Summary(PodcastChannel podcastChannel) {
        Element element = document.createElement("itunes:summary");
        element.appendChild(document.createCDATASection(podcastChannel.getDescription()));
        return element;
    }

    private Node EChannelImage(PodcastChannel podcastChannel) {
        Element element = document.createElement("image");
        Element url = document.createElement("url");

        if (podcastChannel.getImage() != null) {

            url.setTextContent("https://rfa.toloka.media/store/thrumbal/w/300/"
                    +podcastChannel.getImage().getStoreidimage().getUuid()+"/"
                    +podcastChannel.getImage().getStoreidimage().getFilename());
        }
//        else {
        element.appendChild(url);
        Element title = document.createElement("title");
        title.setTextContent(podcastChannel.getTitle());
        element.appendChild(title);

        Element link = document.createElement("link");
        link.setTextContent("https://rfa.toloka.media/podcast/view/"+podcastChannel.getUuid());
        element.appendChild(link);

//        th:if="${podcast.image != null}" th:src="'/store/thrumbal/w/300/' + ${podcast.image.storeidimage.uuid} + '/' + ${podcast.image.storeidimage.filename}"
        return element;
    }

    private Node EChannelGenerator(PodcastChannel podcastChannel) {
        Element element = document.createElement("generator");
        element.setTextContent("https://rfa.toloka.media/");
        return element;
    }

    private Node EChannelSy_updateFrequency(PodcastChannel podcastChannel) {
        Element element = document.createElement("sy:updateFrequency");
        element.setTextContent("1");
        return element;
    }

    private Node EChannelSy_updatePeriod(PodcastChannel podcastChannel) {
        Element element = document.createElement("sy:updatePeriod");
        element.setTextContent("hourly");
        return element;
    }

    private Node EChannelLanguage(PodcastChannel podcastChannel) {
        Element element = document.createElement("description");
        element.setTextContent(podcastChannel.getDescription());
        return element;
    }

    private Node EChannelDescription(PodcastChannel podcastChannel) {
        Element element = document.createElement("language");
        element.setTextContent(podcastChannel.getLanguage());
        return element;
    }

    private Node EChannelLastBuildDate(PodcastChannel podcastChannel) {
        Element element = document.createElement("lastBuildDate");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy Z");
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
//        Instant instant = podcastChannel.getLastbuilddate().toInstant();
//        LocalDateTime ldt = podcastChannel.getLastbuilddate().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime ldt = podcastChannel.getLastbuilddate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
        String sd = ldt.format(formatter);
        element.setTextContent(sd);
        return element;
    }

    private Node EChannelLink(PodcastChannel podcastChannel) {
        Element element = document.createElement("link");
        element.setTextContent("https://rfa.toloka.media/podcast/view/"+podcastChannel.getUuid());
        return element;
    }

    private Node EChannelAtomLink(PodcastChannel podcastChannel) {
        Element element = document.createElement("atom:link");
        element.setAttribute("href","https://rfa.toloka.media/podcast/rss/"+podcastChannel.getUuid());
        element.setAttribute("rel","self");
        element.setAttribute("type","application/rss+xml");
        return element;
    }

    private Element EChannelTitle(PodcastChannel podcastChannel) {
        Element title = document.createElement("title");
        title.setTextContent(podcastChannel.getTitle());
//        title.setTextContent(podcastChannel.getTitle());
        return title;
    }

    private void MakeXML() {
        // створюємо пустий XML з визначенням простору імен.
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true); // встановили використання простору імен
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            // root elements
            document = docBuilder.newDocument();

        } catch (ParserConfigurationException e) {
                logger.warn("Щось пішло не так при створенні XML :( ");
//                return ;
        }
    }

    private String XMLTOString() {
        // Трансформуємо створений RSS XML в рядок

        Transformer transformer;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); //inputStream,"UTF-8"

        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException transformerConfigurationException) {
            logger.warn("Щось пішло не так TransformerConfigurationException ");
            return "";
        }
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(outputStream);

        try {
            outputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.out.println ("Щось пішло не так при перекодуванні XML :( ");
        } catch (IOException exception) {
            System.out.println ("Щось пішло не так при запису потоку :( ");
        }

        try {
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (TransformerException e) {
            logger.warn("Щось пішло не так TransformerException");
            return "";
        }

        return outputStream.toString(StandardCharsets.UTF_8);
//        return "<?xml version='1.0' standalone='yes'?><TEST_XML><T>yyyyyyyyyyyyyyy</T><T>xxxxxxxxxxx</T></TEST_XML>";
    }
}
