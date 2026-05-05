package knu.dykf.landom.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

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

        try {
            // 2. 페이지 접속
            driver.get(url);

            // 3. 자바스크립트가 실행되어 #root가 채워질 때까지 대기 (최대 10초)
            Thread.sleep(5000);

            // 4. 렌더링된 전체 HTML 가져오기
            return driver.getPageSource();
        } catch (InterruptedException e) {
            throw new RuntimeException("Crawling failed", e);
        } finally {
            driver.quit(); // 브라우저 종료
        }
    }
}