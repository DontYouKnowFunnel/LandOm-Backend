package knu.dykf.landom.service.project;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CrawlingService {

    public String crawlLandingPage(String url) {
        // 1. 크롬 드라이버 설정 (헤드리스 모드: 창 안 띄움)
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        // 2. 페이지 접속
        driver.get(url);

        // 3. 자바스크립트가 실행되어 문서 로딩이 완료될 때까지 대기
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(webDriver -> "complete".equals(((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState")));

        // 4. 렌더링된 전체 HTML 가져오기
        String pageSource = driver.getPageSource();
        driver.quit(); // 브라우저 종료
        return pageSource;
    }
}
