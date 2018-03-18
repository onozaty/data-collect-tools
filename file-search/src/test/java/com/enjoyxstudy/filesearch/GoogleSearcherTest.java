package com.enjoyxstudy.filesearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class GoogleSearcherTest {

    @Test
    public void search() {

        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");

        List<String> results = new GoogleSearcher().search("filetype:pdf lombok java テスト");

        assertThat(results)
                .isNotEmpty();
    }

    @Test
    public void download() throws IOException {

        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");

        Path tempDirectoryPath = Files.createTempDirectory(null);

        try {

            List<DownloadResult> results = new GoogleSearcher().download(
                    "filetype:pdf lombok java テスト", tempDirectoryPath);

            assertThat(results)
                    .isNotEmpty()
                    .anyMatch(DownloadResult::isSuccess);

        } finally {
            FileUtils.deleteDirectory(tempDirectoryPath.toFile());
        }
    }
}
