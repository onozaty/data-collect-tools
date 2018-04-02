package com.enjoyxstudy.filesearch.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
    public List<String> search(String query) {

        List<String> resultUrls = new ArrayList<>();

        WebDriver driver = new ChromeDriver();

        try {
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
                Thread.sleep(1000);

                if (System.currentTimeMillis() - startTime > 1000 * 60) {
                    // 1分以上たってもそのままの場合はエラー
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
        } finally {
            driver.quit();
        }
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
