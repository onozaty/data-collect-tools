package com.enjoyxstudy.filesearch.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.enjoyxstudy.filesearch.download.DownloadResult;
import com.enjoyxstudy.filesearch.download.Downloader;

import lombok.SneakyThrows;

public class GoogleSearcher {

    @SneakyThrows(InterruptedException.class)
    public List<String> search(List<String> queries) {

        WebDriver driver = new ChromeDriver();

        try {
            List<String> resultUrls = new ArrayList<>();

            for (String query : queries) {
                resultUrls.addAll(search(driver, query));
            }

            // 複数クエリの場合、重複するURLが存在する可能性があるため
            return resultUrls.stream()
                        .distinct()
                        .collect(Collectors.toList());

        } finally {
            driver.quit();
        }
    }

    public List<String> search(String... queries) {

        return search(Arrays.asList(queries));
    }

    private List<String> search(WebDriver driver, String query) throws InterruptedException {

        List<String> resultUrls = new ArrayList<>();

        driver.get("http://www.google.com/");

        WebElement inputElement = driver.findElement(By.cssSelector("input[name=q]"));
        inputElement.sendKeys(query);

        // 補完が出てくる場合があるので、ESCキーで非表示へ
        // (検索ボタンが隠れると押せなくなるので)
        inputElement.sendKeys(Keys.chord(Keys.ESCAPE));

        driver.findElement(By.cssSelector("input[name=btnK]")).click();

        long startTime = System.currentTimeMillis();
        while (isRobotUrl(driver.getCurrentUrl())) {
            // ロボット確認のURLになった場合は、画面入力を待ち合わせる
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));

            if (System.currentTimeMillis() - startTime > TimeUnit.MINUTES.toMillis(5)) {
                // 5分以上たってもそのままの場合はエラー
                throw new IllegalStateException();
            }
        }

        resultUrls.addAll(collectResultUrls(driver));

        // 次ページのリンクがなくなるまで繰り返し
        while (!driver.findElements(By.cssSelector("a#pnnext")).isEmpty()) {

            driver.findElement(By.cssSelector("a#pnnext")).click();
            resultUrls.addAll(collectResultUrls(driver));
        }

        return resultUrls;
    }

    public List<DownloadResult> download(String query, Path outputDirectoryPath) {

        List<String> resultUrls = search(query);

        return new Downloader().download(resultUrls, outputDirectoryPath);
    }

    private List<String> collectResultUrls(WebDriver driver) {

        return driver.findElements(By.xpath("//h3[@class=\"r\"]/a")).stream()
                .map(x -> x.getAttribute("href"))
                .collect(Collectors.toList());
    }

    private boolean isRobotUrl(String url) {
        return url.contains("google.com/sorry/");
    }
}
