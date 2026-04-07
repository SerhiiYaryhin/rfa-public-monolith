package media.toloka.rfa.config.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

/**
 * Service for sanitizing user-supplied HTML content (e.g. from TinyMCE editors).
 * Uses OWASP Java HTML Sanitizer to strip dangerous tags and attributes
 * while preserving safe formatting and structural elements.
 *
 * Usage in controllers:
 *   <pre>
 *   {@code
 *   @Autowired
 *   private HtmlSanitizerService sanitizer;
 *
 *   model.addAttribute("safeHtml", sanitizer.sanitize(userHtml));
 *   }
 *   </pre>
 *
 * Then in Thymeleaf templates use {@code th:utext="${safeHtml}"} instead of raw user content.
 */
@Service
public class HtmlSanitizerService {

    private final PolicyFactory policy;

    public HtmlSanitizerService() {
        this.policy = new HtmlPolicyBuilder()

                // === Text-level semantic elements ===
                .allowElements(
                        "p", "br", "b", "i", "u", "em", "strong", "small",
                        "sub", "sup", "mark", "del", "ins", "s", "strike"
                )

                // === Headings ===
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")

                // === Lists ===
                .allowElements("ul", "ol", "li", "dl", "dt", "dd")

                // === Blockquote ===
                .allowElements("blockquote")

                // === Preformatted / Code ===
                .allowElements("pre", "code")

                // === Tables ===
                .allowElements("table", "tr", "td", "th", "thead", "tbody", "tfoot", "caption", "col", "colgroup")
                .allowAttributes("border", "cellpadding", "cellspacing", "rowspan", "colspan",
                        "width", "height", "align", "valign", "scope", "headers")
                    .onElements("table", "tr", "td", "th", "thead", "tbody", "tfoot", "caption", "col", "colgroup")

                // === Images ===
                .allowElements("img")
                .allowAttributes("src", "alt", "title", "width", "height", "class", "style", "loading")
                    .onElements("img")
                .allowUrlProtocols("http", "https", "data")

                // === Links ===
                .allowElements("a")
                .allowAttributes("href", "title", "target", "rel", "class", "style")
                    .onElements("a")
                .allowUrlProtocols("http", "https", "mailto")
                .requireRelNofollowOnLinks()

                // === Div / Span / Sectioning ===
                .allowElements("div", "span", "section", "article", "aside", "main", "header", "footer", "nav", "details", "summary")
                .allowAttributes("class", "style", "id")
                    .globally()

                // === Horizontal rule ===
                .allowElements("hr")

                // === Paragraph alignment ===
                .allowAttributes("align")
                    .matching(true, "left", "center", "right", "justify")
                    .globally()

                // === Explicitly disallow dangerous elements ===
                // (these would be disallowed by default, but listing them for documentation)
                .disallowElements(
                        "script", "iframe", "object", "embed", "form", "input",
                        "button", "select", "textarea", "applet", "frame", "frameset",
                        "base", "link", "meta", "style", "title", "head", "body",
                        "marquee", "param", "bgsound", "noscript", "xml", "canvas",
                        "audio", "video", "source", "track"
                )

                // Note: Event handler attributes (onerror, onclick, onload, etc.)
                // are blocked by default since they are not explicitly allowed.

                .toFactory();
    }

    /**
     * Sanitize HTML content by applying the OWASP policy.
     *
     * @param html the raw HTML string (may be null or empty)
     * @return sanitized HTML safe for rendering with th:utext, or empty string if input was null
     */
    public String sanitize(String html) {
        if (html == null) {
            return "";
        }
        if (html.isBlank()) {
            return html;
        }
        return policy.sanitize(html);
    }
}
