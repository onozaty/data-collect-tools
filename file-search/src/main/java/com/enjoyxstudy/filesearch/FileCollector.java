package com.enjoyxstudy.filesearch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.openqa.selenium.InvalidArgumentException;

import com.enjoyxstudy.filesearch.download.DownloadResult;
import com.enjoyxstudy.filesearch.download.Downloader;
import com.enjoyxstudy.filesearch.search.BingSearcher;
import com.enjoyxstudy.filesearch.search.GoogleSearcher;
import com.enjoyxstudy.filesearch.search.Searcher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileCollector {

    public static void main(String[] args) throws IOException, ParseException {

        if (System.getProperty("webdriver.chrome.driver") == null) {
            System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");
        }

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(
                Option.builder("o")
                        .desc("Output folder")
                        .hasArg()
                        .argName("output")
                        .required()
                        .build());
        options.addOption(
                Option.builder("e")
                        .desc("Search engine (google or bing)")
                        .hasArg()
                        .argName("engine")
                        .build());

        CommandLine line = parser.parse(options, args);

        Path outputBaseDirectoryPath = Paths.get(line.getOptionValue("o"));
        List<String> queries = line.getArgList();

        Path fileOutputDirectoryPath = outputBaseDirectoryPath.resolve("files");
        Path resultFilePath = outputBaseDirectoryPath.resolve("file-result.csv");

        Searcher searcher;
        String engineName = line.getOptionValue("e");
        if (engineName == null || engineName.equalsIgnoreCase("google")) {
            searcher = new GoogleSearcher();
        } else if (engineName.equalsIgnoreCase("bing")) {
            searcher = new BingSearcher();
        } else {
            throw new InvalidArgumentException(engineName + " は対応していない検索エンジンです。 ");
        }

        List<DownloadResult> downloadResults = new FileCollector().collect(searcher, queries, fileOutputDirectoryPath);

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

    public List<DownloadResult> collect(Searcher searcher, List<String> queries, Path outputDirectoryPath)
            throws IOException {

        log.info("検索エンジン: " + searcher.getName());
        log.info("検索クエリ: " + queries);

        List<String> urls = searcher.search(queries);
        log.info("検索結果件数: " + urls.size());

        List<DownloadResult> downloadResults = new Downloader().download(urls, outputDirectoryPath);
        log.info("ダウンロード件数: " + downloadResults.stream().filter(DownloadResult::isSuccess).count());

        return downloadResults;
    }
}
