package com.enjoyxstudy.filesearch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    private Path createOutputFilePath(String url, int sequence, Path outputDirectoryPath) {

        // TODO: 本来ならば、レスポンスのContent-Dispositionなどからファイル名を取得し、それを利用した方が良い
        
        // URL内のファイル名部分を取り出し
        String urlFileName;
        try {
            // クエリパラメータ部分を除外して、最後の"/"以降を取り出し
            String path = new URL(url).getPath();
            int lastSeparatorIndex = path.lastIndexOf("/");
            urlFileName = path.substring(lastSeparatorIndex + 1);
        } catch (MalformedURLException e) {
            urlFileName = "";
        }

        // 出力ディレクトリ + 通番_URLのファイル名
        return outputDirectoryPath.resolve(
                String.format("%d_%s", sequence, urlFileName));
    }

    private DownloadResult download(String url, Path outputFilePath) {

        try {
            FileUtils.copyURLToFile(new URL(url), outputFilePath.toFile());
            return DownloadResult.success(url, outputFilePath);

        } catch (IOException e) {
            try {
                // ダウンロード途中のファイルが残っている場合を考慮して削除
                Files.deleteIfExists(outputFilePath);
            } catch (IOException e1) {
                e.addSuppressed(e);
            }

            return DownloadResult.failure(url, e);
        }
    }
}
