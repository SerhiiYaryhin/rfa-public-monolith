package media.toloka.rfa.podcast;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import dev.stalla.model.Episode;
import dev.stalla.model.Podcast;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.model.PodcastImage;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.client.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
//@RequestMapping(path = "/rss", produces = "application/xml")
public class RSSController {

    @Autowired
    private PodcastService podcastService;
//    @Autowired
//    private ClientService clientService;

    final Logger logger = LoggerFactory.getLogger(PodcastController.class);

    /* Формуємо RSS для подкасту */
    /* Що це таке і де воно використовується - взагалі не зрозуміло */
    @GetMapping(value = "/rss/{puuid}", produces = "application/xml")
    public String PodcastChanel(
            @PathVariable String puuid,
            Model model ) {
        PodcastChannel podcastChannel = podcastService.GetChanelByUUID(puuid);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); //inputStream,"UTF-8"
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {

            docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
//            OutputStream outputStream = new OutputStream(); //inputStream,"UTF-8"
            try {
                writeXml(doc, outputStream);
            } catch (TransformerException e) {
                logger.warn("Щось пішло не так при System.out ");
                return "";
            }

            String sss =  outputStream.toString(StandardCharsets.UTF_8);// toString();//.getBytes(StandardCharsets.UTF_8);
            return sss;
        } catch (ParserConfigurationException e) {
            logger.warn("Щось пішло не так при формуванні XML :( ");
            return "";
        }

        /* цикл по епізодам */
    }

    private static void writeXml(Document doc,
                                 OutputStream output)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//        transformer.setOutputProperty(OutputKeys.INDENT, "no");
//        transformer.transform(doc, new StreamResult(output));

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
////        transformer.
//        output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8"));
        try {
//            byte[] ttt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8");
            output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.out.println ("Щось пішло не так при перекодуванні XML :( ");
        } catch (IOException exception) {
            System.out.println ("Щось пішло не так при запису потоку :( ");
        }

//        transformer.transform(source, result);
        transformer.transform(new DOMSource(doc), new StreamResult(output));
//        String ttt = new String();
//        transformer.transform(new DOMSource(doc), new StreamResult(ttt));
//        System.out.println (ttt);

    }

    String decodeUTF8(String str) {
        String sss;
        try {
            sss = new String(str.getBytes("UTF-8"), Charset.forName("UTF-8"));
            return sss;
        } catch (UnsupportedEncodingException e) {
            logger.warn("Якась фігня при перетворенні стока->масив->строка");
        }
        return null;


    }
}
