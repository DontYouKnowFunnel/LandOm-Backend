package knu.dykf.landom.service.project;

import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SectionSourceExtractor {

    public String extractSectionHtml(String html, String cssSelector) {
        try {
            Element sectionElement = Jsoup.parse(html).selectFirst(cssSelector);
            if (sectionElement == null) {
                throw new CustomException(ErrorCode.SECTION_HTML_NOT_FOUND);
            }

            return sectionElement.outerHtml();
        } catch (Selector.SelectorParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public String extractSectionCssRules(String html, String css, String cssSelector) {
        if (css == null || css.isBlank()) {
            return "";
        }

        try {
            // HTML과 selector를 기준으로 실제 섹션 DOM을 찾고, 그 DOM에 닿는 CSS rule만 남긴다.
            Document document = Jsoup.parse(html);
            Element sectionElement = document.selectFirst(cssSelector);
            if (sectionElement == null) {
                throw new CustomException(ErrorCode.SECTION_HTML_NOT_FOUND);
            }

            Set<String> sectionClassNames = collectClassNames(sectionElement);
            return filterMatchingRules(document, sectionElement, sectionClassNames, css).trim();
        } catch (Selector.SelectorParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String filterMatchingRules(
            Document document,
            Element sectionElement,
            Set<String> sectionClassNames,
            String css
    ) {
        List<String> rules = splitTopLevelRules(css);
        StringBuilder result = new StringBuilder();

        for (String rule : rules) {
            String trimmed = rule.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // media/supports 내부 rule은 섹션에 걸리는 rule만 남긴 뒤 wrapper를 보존한다.
            if (trimmed.startsWith("@media") || trimmed.startsWith("@supports")) {
                appendNestedRule(document, sectionElement, sectionClassNames, trimmed, result);
            } else if (trimmed.startsWith("@font-face") || trimmed.startsWith("@keyframes")) {
                result.append(trimmed).append("\n\n");
            } else if (matchesSection(document, sectionElement, sectionClassNames, trimmed)) {
                result.append(trimmed).append("\n\n");
            }
        }

        return result.toString();
    }

    private void appendNestedRule(
            Document document,
            Element sectionElement,
            Set<String> sectionClassNames,
            String rule,
            StringBuilder result
    ) {
        int openBraceIndex = rule.indexOf('{');
        int closeBraceIndex = rule.lastIndexOf('}');
        if (openBraceIndex < 0 || closeBraceIndex <= openBraceIndex) {
            return;
        }

        String header = rule.substring(0, openBraceIndex).trim();
        String nestedCss = rule.substring(openBraceIndex + 1, closeBraceIndex);
        String nestedRules = filterMatchingRules(document, sectionElement, sectionClassNames, nestedCss).trim();
        if (!nestedRules.isBlank()) {
            result.append(header)
                    .append(" {\n")
                    .append(nestedRules)
                    .append("\n}\n\n");
        }
    }

    private boolean matchesSection(
            Document document,
            Element sectionElement,
            Set<String> sectionClassNames,
            String rule
    ) {
        int openBraceIndex = rule.indexOf('{');
        if (openBraceIndex < 0) {
            return false;
        }

        String selectorGroup = rule.substring(0, openBraceIndex);
        if (containsSectionClassSelector(selectorGroup, sectionClassNames)) {
            return true;
        }

        for (String selector : splitSelectorGroup(selectorGroup)) {
            String normalizedSelector = normalizeSelector(selector);
            if (normalizedSelector.isBlank()) {
                continue;
            }

            if (isGlobalSelector(normalizedSelector)
                    || matchesElement(sectionElement, normalizedSelector)
                    || hasMatchingDescendant(sectionElement, normalizedSelector)
                    || selectedElementsContainSection(document, sectionElement, normalizedSelector)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> collectClassNames(Element sectionElement) {
        Set<String> classNames = new HashSet<>(sectionElement.classNames());
        for (Element child : sectionElement.getAllElements()) {
            classNames.addAll(child.classNames());
        }

        return classNames;
    }

    private boolean containsSectionClassSelector(String selectorGroup, Set<String> sectionClassNames) {
        for (String className : sectionClassNames) {
            if (className.isBlank()) {
                continue;
            }

            String escapedClassName = cssEscape(className);
            if (selectorGroup.contains("." + escapedClassName) || selectorGroup.contains("." + className)) {
                return true;
            }
        }

        return false;
    }

    private String cssEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            boolean safe = Character.isLetterOrDigit(current) || current == '_' || current == '-';
            if (safe) {
                escaped.append(current);
            } else {
                escaped.append('\\').append(current);
            }
        }

        return escaped.toString();
    }

    private boolean matchesElement(Element element, String selector) {
        try {
            return element.is(selector);
        } catch (Selector.SelectorParseException e) {
            return false;
        }
    }

    private boolean hasMatchingDescendant(Element sectionElement, String selector) {
        try {
            return !sectionElement.select(selector).isEmpty();
        } catch (Selector.SelectorParseException e) {
            return false;
        }
    }

    private boolean selectedElementsContainSection(Document document, Element sectionElement, String selector) {
        try {
            Elements selectedElements = document.select(selector);
            for (Element selectedElement : selectedElements) {
                // 섹션 자신, 섹션 내부 요소, 섹션의 부모에 걸린 rule까지 함께 포함한다.
                if (selectedElement == sectionElement
                        || selectedElement.parents().contains(sectionElement)
                        || sectionElement.parents().contains(selectedElement)) {
                    return true;
                }
            }
        } catch (Selector.SelectorParseException e) {
            return false;
        }

        return false;
    }

    private boolean isGlobalSelector(String selector) {
        return selector.equals(":root")
                || selector.equals("html")
                || selector.equals("body")
                || selector.equals("*")
                || selector.startsWith("html ")
                || selector.startsWith("body ");
    }

    private String normalizeSelector(String selector) {
        String trimmed = selector.trim();
        if (isGlobalSelector(trimmed)) {
            return trimmed;
        }

        // Jsoup이 처리하기 어려운 pseudo selector는 매칭 판단에서 제거한다.
        return selector
                .replaceAll("::?[a-zA-Z-]+(\\([^)]*\\))?", "")
                .trim();
    }

    private List<String> splitTopLevelRules(String css) {
        // 중첩 block을 깨지 않도록 brace depth가 0으로 돌아오는 지점에서만 rule을 자른다.
        List<String> rules = new ArrayList<>();
        int start = -1;
        int depth = 0;

        for (int i = 0; i < css.length(); i++) {
            char current = css.charAt(i);
            if (start < 0 && !Character.isWhitespace(current)) {
                start = i;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    rules.add(css.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return rules;
    }

    private List<String> splitSelectorGroup(String selectorGroup) {
        // :is(.a, .b), [data-x="a,b"]처럼 괄호/대괄호 안의 comma는 selector 구분자로 보지 않는다.
        List<String> selectors = new ArrayList<>();
        int start = 0;
        int depth = 0;

        for (int i = 0; i < selectorGroup.length(); i++) {
            char current = selectorGroup.charAt(i);
            if (current == '(' || current == '[') {
                depth++;
            } else if (current == ')' || current == ']') {
                depth--;
            } else if (current == ',' && depth == 0) {
                selectors.add(selectorGroup.substring(start, i));
                start = i + 1;
            }
        }

        selectors.add(selectorGroup.substring(start));
        return selectors;
    }
}
