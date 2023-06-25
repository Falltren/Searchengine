package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "jsoup-settings")
public class JsoupConnection {

    private int timeout;

    private String userAgent;

    private boolean ignoreHttpErrors;

    private boolean ignoreContentType;

    private boolean followRedirects;

    private String referrer;

    public Document getDocument(Connection connection) throws IOException {
        return connection
                .timeout(timeout)
                .userAgent(userAgent)
                .ignoreHttpErrors(ignoreHttpErrors)
                .ignoreContentType(ignoreContentType)
                .followRedirects(followRedirects)
                .referrer(referrer)
                .get();
    }

    public Connection getConnection(String url){
        return Jsoup.connect(url).timeout(timeout);
    }
}
