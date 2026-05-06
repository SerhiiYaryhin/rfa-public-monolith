package media.toloka.rfa.convertor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import media.toloka.rfa.convertor.dto.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EditorJsConverter {

    public EditorJsData convertHtmlToEditorJs(String rawHtml) {
        EditorJsData result = new EditorJsData();
        if (rawHtml == null || rawHtml.isBlank()) return result;

        // 1. Попереднє очищення: дозволяємо базове форматування, але видаляємо класи та стилі
        // Ми дозволяємо посилання (a), форматування (b, i, em, strong)
        Safelist safelist = Safelist.basic()
                .addTags("h1", "h2", "h3", "h4", "h5", "h6")
                .removeAttributes("a", "target", "rel"); // Editor.js зазвичай сам керує атрибутами

        String cleanHtml = Jsoup.clean(rawHtml, safelist);

        // 2. Парсимо для детальної обробки блоків
        // Використовуємо оригінальний HTML для галерей, бо Safelist може видалити DIV-и
        Document doc = Jsoup.parseBodyFragment(rawHtml);
        Element body = doc.body();

        for (Element el : body.children()) {
            processElement(el, result);
        }

        return result;
    }

    private void processElement(Element el, EditorJsData result) {
        String tag = el.tagName().toLowerCase();

        // ОБРОБКА ГАЛЕРЕЇ (Swiper / page-gallery)
        if (el.hasClass("page-gallery")) {
            processGallery(el, result);
            return;
        }

        // ОБРОБКА ЗАГОЛОВКІВ (H1-H6)
        if (tag.matches("h[1-6]")) {
            // Видаляємо всі внутрішні теги крім тексту, але зберігаємо логіку заголовка
            int level = Integer.parseInt(tag.substring(1));
            result.addBlock("header", Map.of(
                    "text", el.text(), // Використовуємо .text() щоб прибрати вкладені <span> від Word
                    "level", level
            ));
            return;
        }

        // ОБРОБКА СПИСКІВ (UL/OL)
        if (tag.equals("ul") || tag.equals("ol")) {
            List<String> items = el.select("li").stream()
                    .map(li -> Jsoup.clean(li.html(), Safelist.basic()))
                    .collect(Collectors.toList());

            result.addBlock("list", Map.of(
                    "style", tag.equals("ol") ? "ordered" : "unordered",
                    "items", items
            ));
            return;
        }

        // ОБРОБКА АВТОРА (Специфічний блок в кінці)
        // ОБРОБКА АВТОРА
        if (el.hasClass("c__author-name") || !el.select("a.c__author-name").isEmpty()) {
            Element authorLink = el.is("a") ? el : el.selectFirst("a.c__author-name");
            if (authorLink != null) {
                result.addBlock("author", Map.of(
                        "name", authorLink.text(),
                        "url", authorLink.attr("href")
                ));
            }
            return;
        }



//        if (el.hasClass("c__author-name") || el.select("a.c__author-name").isNotEmpty()) {
//            Element authorLink = el.is("a") ? el : el.selectFirst("a.c__author-name");
//            result.addBlock("author", Map.of(
//                    "name", authorLink.text(),
//                    "url", authorLink.attr("href")
//            ));
//            return;
//        }

        // ОБРОБКА ЗОБРАЖЕНЬ (якщо вони не в галереї)
        if (tag.equals("img") || (tag.equals("p") && el.selectFirst("img") != null)) {
            Element img = tag.equals("img") ? el : el.selectFirst("img");
            result.addBlock("image", Map.of(
                    "url", img.attr("src"),
                    "caption", img.attr("alt")
            ));
            return;
        }

        // ОБРОБКА ПАРАГРАФІВ (Default)
        if (tag.equals("p")) {
            // Очищуємо вміст параграфа, залишаючи тільки дозволені теги (b, i, a)
            String content = Jsoup.clean(el.html(), Safelist.basic());
            if (!content.isBlank() && !content.equals("&nbsp;")) {
                result.addBlock("paragraph", Map.of("text", content));
            }
        }
    }

    private void processGallery(Element galleryEl, EditorJsData result) {
        // Шукаємо всі оригінальні зображення в слайдері
        Elements images = galleryEl.select("img.pg-image");
        if (images.isEmpty()) images = galleryEl.select("img");

        List<Map<String, String>> galleryItems = new ArrayList<>();

        for (Element img : images) {
            // Шукаємо підпис у найближчому figcaption
            Element figure = img.closest("figure");
            String caption = (figure != null) ? figure.select(".pg-caption").text() : "";

            galleryItems.add(Map.of(
                    "url", img.attr("src"),
                    "caption", caption
            ));
        }

        if (!galleryItems.isEmpty()) {
            // Якщо картинка одна - створюємо блок image, якщо більше - gallery
            if (galleryItems.size() == 1) {
                result.addBlock("image", Map.of(
                        "url", galleryItems.get(0).get("url"),
                        "caption", galleryItems.get(0).get("caption")
                ));
            } else {
                result.addBlock("gallery", Map.of("files", galleryItems));
            }
        }
    }
}
