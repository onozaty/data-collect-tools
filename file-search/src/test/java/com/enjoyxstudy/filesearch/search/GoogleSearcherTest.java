package com.enjoyxstudy.filesearch.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.enjoyxstudy.filesearch.download.DownloadResult;

public class GoogleSearcherTest {

    private Searcher targetSeacher = new GoogleSearcher(false);

    @Test
    public void search() {

        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");

        List<String> results = targetSeacher.search("filetype:pdf lombok java テスト");

        assertThat(results)
                .isNotEmpty();
    }

    @Test
    public void search_複数クエリ() {

        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");

        List<String> results = targetSeacher.search(
                "filetype:pdf lombok java テスト",
                "filetype:pdf lombok java デバッグ");

        assertThat(results)
                .isNotEmpty();
    }

    @Test
    public void download() throws IOException {

        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");

        Path tempDirectoryPath = Files.createTempDirectory(null);

        try {

            List<DownloadResult> results = targetSeacher.download(
                    "filetype:pdf lombok java テスト test", tempDirectoryPath);

            assertThat(results)
                    .isNotEmpty()
                    .anyMatch(DownloadResult::isSuccess);

        } finally {
            FileUtils.deleteDirectory(tempDirectoryPath.toFile());
        }
    }
}
