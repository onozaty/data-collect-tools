package com.enjoyxstudy.filesearch.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.enjoyxstudy.filesearch.download.DownloadResult;
import com.enjoyxstudy.filesearch.download.Downloader;

public interface Searcher {

    public default List<String> search(List<String> queries) {

        WebDriver driver = new ChromeDriver();

        try {
            List<String> resultUrls = new ArrayList<>();

            for (String query : queries) {
                resultUrls.addAll(search(driver, query));
            }

            // 複数クエリの場合、重複するURLが存在する可能性があるため
            return resultUrls.stream()
                    .distinct()
                    .collect(Collectors.toList());

        } finally {
            driver.quit();
        }
    }

    public default List<String> search(String... queries) {

        return search(Arrays.asList(queries));
    }

    public default List<DownloadResult> download(String query, Path outputDirectoryPath) throws IOException {

        List<String> resultUrls = search(query);

        return new Downloader().download(resultUrls, outputDirectoryPath);
    }

    List<String> search(WebDriver driver, String query);

    String getName();
}
