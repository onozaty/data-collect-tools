package com.enjoyxstudy.filesearch.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class DownloaderTest {

    @Test
    public void test() throws IOException {

        Path tempDirectoryPath = Files.createTempDirectory(null);

        try {

            Downloader downloader = new Downloader();
            List<DownloadResult> results = downloader.download(
                    Arrays.asList(
                            "https://www.toshiba.co.jp/tech/review/2014/08/69_08pdf/a08.pdf",
                            "https://google.com"),
                    tempDirectoryPath);

            assertThat(results)
                    .hasSize(2);

        } finally {
            FileUtils.deleteDirectory(tempDirectoryPath.toFile());
        }
    }

}
