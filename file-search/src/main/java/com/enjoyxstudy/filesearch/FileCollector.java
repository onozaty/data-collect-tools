package com.enjoyxstudy.filesearch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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

        options.addOption(
                Option.builder("u")
                        .desc("URLs")
                        .hasArg()
                        .argName("urls")
                        .build());

        CommandLine line = parser.parse(options, args);

        Path outputBaseDirectoryPath = Paths.get(line.getOptionValue("o"));
        List<String> queries = line.getArgList();

        Searcher searcher;
        String engineName = line.getOptionValue("e");
        if (engineName == null || engineName.equalsIgnoreCase("google")) {
            searcher = new GoogleSearcher();
        } else if (engineName.equalsIgnoreCase("bing")) {
            searcher = new BingSearcher();
        } else {
            throw new InvalidArgumentException(engineName + " は対応していない検索エンジンです。 ");
        }

        List<String> urls = null;
        if (line.hasOption("u")) {
            urls = Files.lines(Paths.get(line.getOptionValue("u")), StandardCharsets.UTF_8)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        new FileCollector().collect(searcher, queries, urls, outputBaseDirectoryPath);
    }

    public void collect(Searcher searcher, List<String> queries, List<String> urls, Path outputDirectoryPath)
            throws IOException {

        if (Files.notExists(outputDirectoryPath)) {
            Files.createDirectories(outputDirectoryPath);
        }

        if (urls == null) {

            log.info("検索エンジン: " + searcher.getName());
            log.info("検索クエリ: " + queries);

            urls = searcher.search(queries).stream()
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        log.info("検索結果件数: " + urls.size());

        Files.write(
                outputDirectoryPath.resolve("urls.txt"),
                urls.stream()
                        .collect(Collectors.joining("\n"))
                        .getBytes(StandardCharsets.UTF_8));

        List<DownloadResult> downloadResults = new Downloader().download(
                urls, outputDirectoryPath.resolve("files"));

        log.info("ダウンロード件数: " + downloadResults.stream().filter(DownloadResult::isSuccess).count());

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputDirectoryPath.resolve("download-result.csv"), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL);) {

            writer.write('\uFEFF'); // BOM

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
}
