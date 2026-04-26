package knu.dykf.landom.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
@Service
public class CrawlingService {

    public String crawlLandingPage(String url) {
        try {
            log.info("[Jsoup Crawling Start] URL: {}", url);

            // 실제 브라우저(Chrome)인 것처럼 헤더를 설정하여 차단을 방지합니다.
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(10000) // 10초 타임아웃
                    .get();

            String html = doc.html();
            log.info("[Jsoup Crawling Success] HTML Length: {}", html.length());

            return html;

        } catch (IOException e) {
            log.error("[Jsoup Crawling Failed] Error: {}", e.getMessage());
            throw new RuntimeException("페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}