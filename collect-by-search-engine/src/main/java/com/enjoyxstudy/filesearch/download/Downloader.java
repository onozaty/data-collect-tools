package com.enjoyxstudy.filesearch.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Downloader {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build();

    public List<DownloadResult> download(List<String> urls, Path outputDirectoryPath) throws IOException {

        if (Files.notExists(outputDirectoryPath)) {
            Files.createDirectories(outputDirectoryPath);
        }

        return IntStream.range(0, urls.size())
                .parallel() // 並列でダウンロード
                .mapToObj(index -> {

                    String url = urls.get(index);

                    try {
                        // 通番は1から
                        Path outputFilePath = download(url, index + 1, outputDirectoryPath);
                        return DownloadResult.success(url, outputFilePath);

                    } catch (Exception e) {
                        return DownloadResult.failure(url, e);
                    }
                })
                .collect(Collectors.toList());
    }

    private static final int OUTPUT_FILE_NAME_LIMIT = 40;

    private Path download(String url, int sequence, Path outputDirectoryPath) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException("Request failed. " + response);
            }

            Path outputFilePath = createOutputFilePath(response, sequence, outputDirectoryPath);

            InputStream downloadFileStream = response.body().byteStream();
            try {
                Files.copy(downloadFileStream, outputFilePath);
            } catch (IOException e) {
                try {
                    //  ダウンロード途中のファイルが残っている場合を考慮して削除
                    Files.deleteIfExists(outputFilePath);
                } catch (IOException e1) {
                    e.addSuppressed(e1);
                }

                throw e;
            }

            return outputFilePath;
        }
    }

    private static final Pattern INVALID_WINDOWS_FILENAME_CHARS_PATTERN = Pattern.compile("[\\/:\\*\\?\"<>|]");

    private Path createOutputFilePath(Response response, int sequence, Path outputDirectoryPath) {

        try {

            String filename = getFilenameByHeader(response);
            if (StringUtils.isEmpty(filename)) {
                filename = getFilenameByUrl(response.request().url().toString());
            }

            if (!StringUtils.isEmpty(filename)) {
                filename = INVALID_WINDOWS_FILENAME_CHARS_PATTERN.matcher(filename).replaceAll("");
            }

            if (StringUtils.isEmpty(filename)) {
                // ファイル名が取得できなかった場合、通番のみのファイル名へ
                return outputDirectoryPath.resolve(String.valueOf(sequence));
            }

            // 長すぎるファイル名となった場合には短縮
            if (filename.length() > OUTPUT_FILE_NAME_LIMIT) {
                // 拡張子は残したいので、先頭部分を消す
                filename = filename.substring(filename.length() - OUTPUT_FILE_NAME_LIMIT);
            }

            // 出力ディレクトリ + 通番_URLのファイル名
            return outputDirectoryPath.resolve(
                    String.format("%d_%s", sequence, filename));

        } catch (Exception e) {
            // ファイル名作成に失敗した場合、通番のみのファイル名へ
            return outputDirectoryPath.resolve(String.valueOf(sequence));
        }
    }

    private static final Pattern CONTENT_DISPOSITION_FILENAME_PATTERN = Pattern
            .compile("filename[\\s]*=[\\s]*['\"]?([^'\";]+)['\";]?");

    private static final Pattern CONTENT_DISPOSITION_FILENAME_WITH_ENCODING_PATTERN = Pattern
            .compile("filename\\*[\\s]*=[\\s]*([^'']+)''([^'\";]+)");

    private String getFilenameByHeader(Response response) {

        String contentDisposition = response.header("Content-Disposition");
        if (contentDisposition == null) {
            return null;
        }

        Matcher matcher = CONTENT_DISPOSITION_FILENAME_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String value = matcher.group(1);
            try {
                String filename;
                try {
                    filename = DecoderUtil.decodeEncodedWords(value, DecodeMonitor.STRICT);
                } catch (Throwable e) { // DecoderUtil#decodeEncodedWordsがErrorを返すことがあるので
                    filename = value;
                }

                try {
                    return URLDecoder.decode(filename, "UTF-8");
                } catch (Exception e) {
                    return filename;
                }
            } catch (Exception e) {
                return value;
            }
        }

        matcher = CONTENT_DISPOSITION_FILENAME_WITH_ENCODING_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String encoding = matcher.group(1);
            String value = matcher.group(2);

            try {
                return new URLCodec(encoding).decode(value);
            } catch (Exception e) {
                // おかしなエンコーディングの場合、デコード前の文字を返却
                return value;
            }
        }

        return null;
    }

    private String getFilenameByUrl(String url) throws MalformedURLException {

        try {

            // クエリパラメータ部分を除外して、最後の"/"以降を取り出し
            String path = new URL(url).getPath();
            int lastSeparatorIndex = path.lastIndexOf("/");

            String filename = path.substring(lastSeparatorIndex + 1);
            int semicolonIndex = filename.lastIndexOf(";");
            if (semicolonIndex != -1) {
                filename = filename.substring(0, semicolonIndex);
            }

            return URLDecoder.decode(filename, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
