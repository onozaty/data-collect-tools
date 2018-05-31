package com.enjoyxstudy.filesearch.download;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

public class Downloader {

    public List<DownloadResult> download(List<String> urls, Path outputDirectoryPath) {

        return IntStream.range(0, urls.size())
                .parallel() // 並列でダウンロード
                .mapToObj(index -> {
                    String url = urls.get(index);
                    return download(
                            url,
                            // 通番は1から
                            createOutputFilePath(url, index + 1, outputDirectoryPath));
                })
                .collect(Collectors.toList());
    }

    private static final int OUTPUT_FILE_NAME_LIMIT = 40;

    private Path createOutputFilePath(String url, int sequence, Path outputDirectoryPath) {

        // TODO: 本来ならば、レスポンスのContent-Dispositionなどからファイル名を取得し、それを利用した方が良い

        // URL内のファイル名部分を取り出し
        try {
            // クエリパラメータ部分を除外して、最後の"/"以降を取り出し
            String path = new URL(url).getPath();
            int lastSeparatorIndex = path.lastIndexOf("/");
            String urlFileName = URLDecoder.decode(path.substring(lastSeparatorIndex + 1), "UTF-8");

            // 長すぎるファイル名となった場合には短縮
            if (urlFileName.length() > OUTPUT_FILE_NAME_LIMIT) {
                // 拡張子は残したいので、先頭部分を消す
                urlFileName = urlFileName.substring(urlFileName.length() - OUTPUT_FILE_NAME_LIMIT);
            }

            // 出力ディレクトリ + 通番_URLのファイル名
            return outputDirectoryPath.resolve(
                    String.format("%d_%s", sequence, urlFileName));

        } catch (Exception e) {
            // ファイル名作成に失敗した場合、通番のみのファイル名へ
            return outputDirectoryPath.resolve(String.valueOf(sequence));
        }
    }

    private final static int TIMEOUT = (int) TimeUnit.MINUTES.toMillis(5);

    private DownloadResult download(String url, Path outputFilePath) {

        try {
            FileUtils.copyURLToFile(new URL(url), outputFilePath.toFile(), TIMEOUT, TIMEOUT);
            return DownloadResult.success(url, outputFilePath);

        } catch (IOException e) {
            try {
                // ダウンロード途中のファイルが残っている場合を考慮して削除
                Files.deleteIfExists(outputFilePath);
            } catch (IOException e1) {
                e.addSuppressed(e1);
            }

            return DownloadResult.failure(url, e);
        }
    }
}
