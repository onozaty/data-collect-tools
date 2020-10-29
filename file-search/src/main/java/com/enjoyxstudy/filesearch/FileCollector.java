package com.enjoyxstudy.filesearch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
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

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(
                Option.builder("o")
                        .longOpt("output")
                        .desc("output folder")
                        .hasArg()
                        .argName("output folder")
                        .required()
                        .build());
        options.addOption(
                Option.builder("e")
                        .longOpt("engine")
                        .desc("search engine (google or bing)")
                        .hasArg()
                        .argName("engine name")
                        .build());

        options.addOption(
                Option.builder("u")
                        .longOpt("urls")
                        .desc("urls file")
                        .hasArg()
                        .argName("urls file")
                        .build());

        options.addOption(
                Option.builder("eu")
                        .longOpt("excludeurls")
                        .desc("exclude urls file")
                        .hasArg()
                        .argName("excludeurls file")
                        .build());

        options.addOption(
                Option.builder("sd")
                        .longOpt("skipdownload")
                        .desc("skip download")
                        .build());

        options.addOption(
                Option.builder("hl")
                        .longOpt("headless")
                        .desc("headless mode")
                        .build());

        options.addOption(
                Option.builder("qct")
                        .longOpt("qctype")
                        .desc("file type used in combination of queries (example: xls, doc)")
                        .hasArg()
                        .argName("file type (use query combinations)")
                        .build());

        options.addOption(
                Option.builder("qcf")
                        .longOpt("qcfile")
                        .desc("query combinations file")
                        .hasArg()
                        .argName("query combinations file")
                        .build());

        try {
            CommandLine line = parser.parse(options, args);

            Path outputBaseDirectoryPath = Paths.get(line.getOptionValue("o"));

            List<String> queries;

            if (line.hasOption("qct") && line.hasOption("qcf")) {
                Path queriesPath = Paths.get(line.getOptionValue("qcf"));
                // コンビネーションのクエリを生成
                queries = createCombinationQueries(
                        line.getOptionValue("qct"),
                        Files.readAllLines(queriesPath, StandardCharsets.UTF_8));
            } else {
                queries = line.getArgList();
            }

            boolean headless = line.hasOption("hl");

            Searcher searcher;
            String engineName = line.getOptionValue("e");
            if (engineName == null || engineName.equalsIgnoreCase("google")) {
                searcher = new GoogleSearcher(headless);
            } else if (engineName.equalsIgnoreCase("bing")) {
                searcher = new BingSearcher(headless);
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

            List<String> excludeUrls = null;
            if (line.hasOption("eu")) {
                excludeUrls = Files.lines(Paths.get(line.getOptionValue("eu")), StandardCharsets.UTF_8)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
            }

            boolean skipDownload = line.hasOption("sd");

            new FileCollector().collect(
                    searcher,
                    queries,
                    urls,
                    excludeUrls,
                    skipDownload,
                    outputBaseDirectoryPath);

        } catch (ParseException e) {
            System.out.println("Unexpected exception:" + e.getMessage());
            System.out.println();

            printUsage(options);
            return;
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(200);
        help.setOptionComparator(null); // 順番を変えない

        // ヘルプを出力
        help.printHelp("java -jar file-search-all.jar", options, true);
        System.exit(1);
    }

    private static List<String> createCombinationQueries(String fileType, List<String> words) {

        String fileTypeQuery = "filetype:" + fileType;

        List<String> queries = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            queries.add(String.join(" ", fileTypeQuery, word));

            // 2つのワードの組み合わせを作成
            for (int j = i + 1; j < words.size(); j++) {

                queries.add(String.join(" ", fileTypeQuery, word, words.get(j)));
            }
        }

        return queries;
    }

    public void collect(Searcher searcher, List<String> queries, List<String> urls, List<String> excludeUrls,
            boolean skipDownload, Path outputDirectoryPath)
            throws IOException {

        if (Files.notExists(outputDirectoryPath)) {
            Files.createDirectories(outputDirectoryPath);
        }

        if (urls != null) {

            log.info("URL件数: " + urls.size());

        } else {

            log.info("検索エンジン: " + searcher.getName());
            log.info("検索クエリ: " + queries);
            log.info("ヘッドレスモード: " + searcher.isHeadless());

            urls = searcher.search(queries).stream()
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            log.info("検索結果件数: " + urls.size());
        }

        if (excludeUrls != null) {

            log.info("除外対象URL件数: " + excludeUrls.size());

            urls.removeIf(excludeUrls::contains);

            log.info("除外後URL件数: " + urls.size());
        }

        Files.write(
                outputDirectoryPath.resolve("urls.txt"),
                (urls.stream().collect(Collectors.joining("\n")) + "\n").getBytes(StandardCharsets.UTF_8));

        if (skipDownload) {
            log.info("ダウンロードをスキップしました。");
            return;
        }

        log.info("ダウンロードを開始しました。");

        List<DownloadResult> downloadResults = new Downloader().download(
                urls, outputDirectoryPath.resolve("files"));

        log.info("ダウンロードが完了しました。 ダウンロード成功件数: "
                + downloadResults.stream().filter(DownloadResult::isSuccess).count());

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
