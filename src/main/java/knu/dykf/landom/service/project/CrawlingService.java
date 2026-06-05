package knu.dykf.landom.service.project;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CrawlingService {

    public record LandingPageSnapshot(String html, String css) {
    }

    public String crawlLandingPage(String url) {
        return crawlLandingPageSnapshot(url).html();
    }

    public LandingPageSnapshot crawlLandingPageSnapshot(String url) {
        // 1. 크롬 드라이버 설정 (헤드리스 모드: 창 안 띄움)
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        try {
            // 2. 페이지 접속
            driver.get(url);

            // 3. 자바스크립트가 실행되어 문서 로딩이 완료될 때까지 대기
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(webDriver -> "complete".equals(((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState")));

            // 4. 렌더링된 전체 HTML 가져오기
            String pageSource = driver.getPageSource();
            String css = crawlCssRules(driver);
            return new LandingPageSnapshot(pageSource, css);
        } finally {
            driver.quit(); // 브라우저 종료
        }
    }

    private String crawlCssRules(WebDriver driver) {
        return String.join("\n", readAccessibleCssRules(driver), fetchLinkedStylesheets(driver)).trim();
    }

    private String readAccessibleCssRules(WebDriver driver) {
        // Same-origin 스타일시트와 inline style 태그는 브라우저 CSSOM에서 바로 읽을 수 있다.
        String script = """
                return Array.from(document.styleSheets)
                    .map(function(sheet) {
                        try {
                            return Array.from(sheet.cssRules || [])
                                .map(function(rule) { return rule.cssText; })
                                .join('\\n');
                        } catch (e) {
                            return '';
                        }
                    })
                    .filter(Boolean)
                    .join('\\n');
                """;

        Object result = ((JavascriptExecutor) driver).executeScript(script);
        return result instanceof String css ? css : "";
    }

    private String fetchLinkedStylesheets(WebDriver driver) {
        // Cross-origin CSSOM 접근이 막히는 경우가 있어 link href를 직접 요청해 한 번 더 보강한다.
        String script = """
                return Array.from(document.querySelectorAll('link[rel~="stylesheet"][href]'))
                    .map(function(link) { return link.href; });
                """;
        Object result = ((JavascriptExecutor) driver).executeScript(script);
        if (!(result instanceof List<?> urls)) {
            return "";
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        StringBuilder css = new StringBuilder();

        for (Object value : urls) {
            if (!(value instanceof String url) || url.isBlank()) {
                continue;
            }

            fetchStylesheet(httpClient, url).ifPresent(stylesheet -> css.append(stylesheet).append("\n"));
        }

        return css.toString();
    }

    private Optional<String> fetchStylesheet(HttpClient httpClient, String url) {
        try {
            // 일부 CDN은 기본 Java User-Agent를 차단하므로 브라우저에 가까운 값을 사용한다.
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Optional.of(response.body());
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
