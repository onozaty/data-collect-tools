package com.enjoyxstudy.filesearch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.enjoyxstudy.filesearch.download.DownloadResult;
import com.enjoyxstudy.filesearch.download.Downloader;
import com.enjoyxstudy.filesearch.search.GoogleSearcher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileCollector {

    public static void main(String[] args) throws IOException {

        String query = args[0];
        Path outputBaseDirectoryPath = Paths.get(args[1]);

        if (System.getProperty("webdriver.chrome.driver") == null) {
            System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");
        }

        Path fileOutputDirectoryPath = outputBaseDirectoryPath.resolve("files");
        Path resultFilePath = outputBaseDirectoryPath.resolve("file-result.csv");

        List<DownloadResult> downloadResults = new FileCollector().collect(query, fileOutputDirectoryPath);

        try (BufferedWriter writer = Files.newBufferedWriter(resultFilePath);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL);) {

            csvPrinter.printRecord("URL", "FILE");

            for (DownloadResult result : downloadResults) {
                csvPrinter.printRecord(
                        result.getUrl(),
                        result.isSuccess()
                                ? result.getOutputFilePath().getFileName()
                                : null);
            }
        }
    }

    public List<DownloadResult> collect(String query, Path outputDirectoryPath) {

        log.info("検索クエリ: " + query);

        List<String> urls = new GoogleSearcher().search(query);
        log.info("検索結果件数: " + urls.size());

        List<DownloadResult> downloadResults = new Downloader().download(urls, outputDirectoryPath);
        log.info("ダウンロード件数: " + downloadResults.stream().filter(DownloadResult::isSuccess).count());

        return downloadResults;
    }
}
