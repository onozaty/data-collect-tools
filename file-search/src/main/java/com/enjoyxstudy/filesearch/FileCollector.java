package com.enjoyxstudy.filesearch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

        Path outputBaseDirectoryPath = Paths.get(args[0]);
        List<String> queries = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));

        if (System.getProperty("webdriver.chrome.driver") == null) {
            System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");
        }

        Path fileOutputDirectoryPath = outputBaseDirectoryPath.resolve("files");
        Path resultFilePath = outputBaseDirectoryPath.resolve("file-result.csv");

        List<DownloadResult> downloadResults = new FileCollector().collect(queries, fileOutputDirectoryPath);

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

    public List<DownloadResult> collect(List<String> queries, Path outputDirectoryPath) {

        log.info("検索クエリ: " + queries);

        List<String> urls = new GoogleSearcher().search(queries);
        log.info("検索結果件数: " + urls.size());

        List<DownloadResult> downloadResults = new Downloader().download(urls, outputDirectoryPath);
        log.info("ダウンロード件数: " + downloadResults.stream().filter(DownloadResult::isSuccess).count());

        return downloadResults;
    }
}
