package com.enjoyxstudy.filesearch.download;

import java.nio.file.Path;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DownloadResult {

    private String url;

    private boolean isSuccess;

    private Path outputFilePath;

    private Exception failedCause;

    private DownloadResult(String url, Path outputFilePath) {
        this.url = url;
        this.isSuccess = true;
        this.outputFilePath = outputFilePath;
    }

    private DownloadResult(String url, Exception failedCause) {
        this.url = url;
        this.isSuccess = false;
        this.failedCause = failedCause;
    }

    public static DownloadResult success(String url, Path outputFilePath) {

        return new DownloadResult(url, outputFilePath);
    }

    public static DownloadResult failure(String url, Exception failedCause) {

        return new DownloadResult(url, failedCause);
    }
}
