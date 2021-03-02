package com.enjoyxstudy.filesearch.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.enjoyxstudy.filesearch.download.DownloadResult;
import com.enjoyxstudy.filesearch.download.Downloader;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Searcher {

    @Getter
    private final boolean headless;

    public Searcher(boolean headless) {
        this.headless = headless;

        WebDriverManager.chromedriver().setup();
    }

    public List<String> search(List<String> queries) {

        ChromeOptions options = new ChromeOptions().setHeadless(headless);
        WebDriver driver = new ChromeDriver(options);

        try {
            List<String> resultUrls = new ArrayList<>();

            for (int i = 0; i < queries.size(); i++) {
                String query = queries.get(i);

                try {
                    log.info("検索を開始します。 検索クエリ({}/{}): {}", i + 1, queries.size(), query);
                    List<String> urls = search(driver, query);
                    log.info("検索を終了しました。 件数: {}", urls.size());

                    resultUrls.addAll(urls);
                } catch (Exception e) {
                    log.error("検索でエラーが発生しました。 ", e);
                }
            }

            // 複数クエリの場合、重複するURLが存在する可能性があるため
            return resultUrls.stream()
                    .distinct()
                    .collect(Collectors.toList());

        } finally {
            driver.quit();
        }
    }

    public List<String> search(String... queries) {

        return search(Arrays.asList(queries));
    }

    public List<DownloadResult> download(String query, Path outputDirectoryPath) throws IOException {

        List<String> resultUrls = search(query);

        return new Downloader().download(resultUrls, outputDirectoryPath);
    }

    protected abstract List<String> search(WebDriver driver, String query);

    public abstract String getName();
}
