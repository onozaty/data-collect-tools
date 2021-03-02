package com.enjoyxstudy.filesearch.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.enjoyxstudy.filesearch.download.DownloadResult;

public class BingSearcherTest {

    private Searcher targetSeacher = new BingSearcher(false);

    @Test
    public void search() {

        List<String> results = targetSeacher.search("filetype:pdf テスト selenium java");

        assertThat(results)
                .isNotEmpty();
    }

    @Test
    public void search_複数クエリ() {

        List<String> results = targetSeacher.search(
                "filetype:pdf テスト selenium java",
                "filetype:pdf lombok java コード");

        assertThat(results)
                .isNotEmpty();
    }

    @Test
    public void download() throws IOException {

        Path tempDirectoryPath = Files.createTempDirectory(null);

        try {

            List<DownloadResult> results = targetSeacher.download(
                    "filetype:pdf lombok java コード", tempDirectoryPath);

            assertThat(results)
                    .isNotEmpty()
                    .anyMatch(DownloadResult::isSuccess);

        } finally {
            FileUtils.deleteDirectory(tempDirectoryPath.toFile());
        }
    }
}
