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
        result.setTime(System.currentTimeMillis());
        if (rawHtml == null || rawHtml.isBlank()) return result;

        // Парсимо HTML. Використовуємо parseBodyFragment, щоб Jsoup не додавав <html><head>
        Document doc = Jsoup.parseBodyFragment(rawHtml);
        Element body = doc.body();

        // Обробляємо всі прямі нащадки body
        for (Element el : body.children()) {
            processElement(el, result);
        }

        return result;
    }

    private void processElement(Element el, EditorJsData result) {
        String tag = el.tagName().toLowerCase();

        // 1. ОБРОБКА ГАЛЕРЕЇ (Swiper / page-gallery)
        if (el.hasClass("page-gallery") || el.selectFirst(".page-gallery") != null) {
            processGallery(el.hasClass("page-gallery") ? el : el.selectFirst(".page-gallery"), result);
            return;
        }

        // 2. ОБРОБКА ЗАГОЛОВКІВ (H1-H6)
        if (tag.matches("h[1-6]")) {
            int level = Integer.parseInt(tag.substring(1));
            String text = cleanInlineHtml(el.html());
            if (!text.isEmpty()) {
                result.addBlock("header", Map.of(
                        "text", text,
                        "level", level
                ));
            }
            return;
        }

        // 3. ОБРОБКА СПИСКІВ (UL/OL)
        if (tag.equals("ul") || tag.equals("ol")) {
            List<String> items = el.select("> li").stream()
                    .map(li -> cleanInlineHtml(li.html()))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (!items.isEmpty()) {
                result.addBlock("list", Map.of(
                        "style", tag.equals("ol") ? "ordered" : "unordered",
                        "items", items
                ));
            }
            return;
        }

        // 4. ОБРОБКА ЦИТАТ (BLOCKQUOTE)
        if (tag.equals("blockquote")) {
            result.addBlock("quote", Map.of(
                    "text", cleanInlineHtml(el.html()),
                    "caption", "",
                    "alignment", "left"
            ));
            return;
        }

        // 5. ОБРОБКА РОЗДІЛЬНИКІВ (HR)
        if (tag.equals("hr")) {
            result.addBlock("delimiter", new HashMap<>());
            return;
        }

        // 6. ОБРОБКА ТАБЛИЦЬ
        if (tag.equals("table")) {
            List<List<String>> content = new ArrayList<>();
            for (Element tr : el.select("tr")) {
                List<String> row = tr.select("th, td").stream()
                        .map(cell -> cleanInlineHtml(cell.html()))
                        .collect(Collectors.toList());
                content.add(row);
            }
            if (!content.isEmpty()) {
                result.addBlock("table", Map.of("content", content));
            }
            return;
        }

        // 7. ОБРОБКА EMBEDS (YouTube, etc.)
        if (tag.equals("iframe") || el.selectFirst("iframe") != null) {
            Element iframe = tag.equals("iframe") ? el : el.selectFirst("iframe");
            String src = iframe.attr("src");
            if (src.contains("youtube.com") || src.contains("youtu.be")) {
                result.addBlock("embed", Map.of(
                        "service", "youtube",
                        "source", src,
                        "embed", src,
                        "width", iframe.attr("width"),
                        "height", iframe.attr("height"),
                        "caption", ""
                ));
                return;
            }
        }

        // 8. ОБРОБКА АВТОРА
        if (el.hasClass("c__author-name") || el.selectFirst("a.c__author-name") != null) {
            Element authorLink = el.hasClass("c__author-name") && tag.equals("a") ? el : el.selectFirst("a.c__author-name");
            if (authorLink != null) {
                result.addBlock("author", Map.of(
                        "name", authorLink.text(),
                        "url", authorLink.attr("href")
                ));
            }
            return;
        }

        // 9. ОБРОБКА ЗОБРАЖЕНЬ
        // Якщо в параграфі тільки одна картинка (або картинка + пробіли), робимо її блоком image
        Elements images = el.select("img");
        if (tag.equals("img") || (tag.equals("p") && images.size() == 1 && el.text().trim().isEmpty())) {
            Element img = images.first();
            result.addBlock("image", Map.of(
                    "file", Map.of("url", img.attr("src")),
                    "caption", img.attr("alt"),
                    "withBorder", false,
                    "stretched", false,
                    "withBackground", false
            ));
            return;
        }

        // 10. ОБРОБКА ПАРАГРАФІВ ТА ІНШИХ КОНТЕЙНЕРІВ
        // Якщо це параграф або просто текст, що залишився
        if (tag.equals("p") || tag.equals("div") || tag.equals("span")) {
            String content = cleanInlineHtml(el.html());
            if (!content.isBlank() && !content.equals("&nbsp;")) {
                result.addBlock("paragraph", Map.of("text", content));
            }
        } else if (!el.text().isBlank()) {
            // Фолбек для будь-якого іншого тегу з текстом
            result.addBlock("paragraph", Map.of("text", cleanInlineHtml(el.outerHtml())));
        }
    }

    /**
     * Очищує HTML для інлайнових елементів (всередині блоків Editor.js)
     */
    private String cleanInlineHtml(String html) {
        if (html == null) return "";
        // Editor.js параграфи підтримують b, i, a, code
        Safelist safelist = Safelist.simpleText()
                .addTags("a", "b", "strong", "i", "em", "code", "br")
                .addAttributes("a", "href");
        
        return Jsoup.clean(html, safelist);
    }

    private void processGallery(Element galleryEl, EditorJsData result) {
        Elements images = galleryEl.select("img");
        List<Map<String, Object>> galleryItems = new ArrayList<>();

        for (Element img : images) {
            Element figure = img.closest("figure");
            String caption = (figure != null) ? figure.select(".pg-caption, figcaption").text() : img.attr("alt");

            galleryItems.add(Map.of(
                    "url", img.attr("src"),
                    "caption", caption
            ));
        }

        if (!galleryItems.isEmpty()) {
            if (galleryItems.size() == 1) {
                result.addBlock("image", Map.of(
                        "file", Map.of("url", galleryItems.get(0).get("url")),
                        "caption", galleryItems.get(0).get("caption")
                ));
            } else {
                result.addBlock("gallery", Map.of("files", galleryItems));
            }
        }
    }
}
