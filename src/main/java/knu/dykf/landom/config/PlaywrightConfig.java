package knu.dykf.landom.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaywrightConfig {

    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        // headless: true (화면 없음), false로 설정하면 브라우저가 뜨는 것을 볼 수 있습니다.
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
    }
}