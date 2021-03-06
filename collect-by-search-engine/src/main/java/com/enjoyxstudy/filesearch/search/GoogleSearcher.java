package com.enjoyxstudy.filesearch.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import lombok.Getter;
import lombok.SneakyThrows;

public class GoogleSearcher extends Searcher {

    public GoogleSearcher(boolean headless) {
        super(headless);
    }

    @Getter
    private final String name = "Google";

    @SneakyThrows
    public List<String> search(WebDriver driver, String query) {

        List<String> resultUrls = new ArrayList<>();

        driver.get("http://www.google.com/");

        WebElement inputElement = driver.findElement(By.cssSelector("input[name=q]"));
        inputElement.sendKeys(query);
        inputElement.submit();

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

    private List<String> collectResultUrls(WebDriver driver) {

        return driver.findElements(By.xpath("//div[@class=\"yuRUbf\"]/a")).stream()
                .map(x -> x.getAttribute("href"))
                .collect(Collectors.toList());
    }

    private boolean isRobotUrl(String url) {
        return url.contains("google.com/sorry/");
    }
}
