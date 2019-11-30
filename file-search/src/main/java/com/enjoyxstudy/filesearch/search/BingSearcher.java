package com.enjoyxstudy.filesearch.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import lombok.Getter;
import lombok.SneakyThrows;

public class BingSearcher implements Searcher {

    @Getter
    private final String name = "Bing";

    @SneakyThrows
    public List<String> search(WebDriver driver, String query) {

        List<String> resultUrls = new ArrayList<>();

        driver.get("https://www.bing.com/?FORM=&setmkt=en-us&setlang=en-us");
        String startUrl = driver.getCurrentUrl();

        WebElement inputElement = driver.findElement(By.cssSelector("#sb_form_q"));
        inputElement.sendKeys(query);
        inputElement.submit();
        if (startUrl.equals(driver.getCurrentUrl())) {
            // submitが空振りする時があるので
            Thread.sleep(1000);
            inputElement.submit();
        }

        new WebDriverWait(driver, 10)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#b_results")));

        resultUrls.addAll(collectResultUrls(driver));
        int lastPageNo = 1;

        // 次ページのリンクがなくなるまで繰り返し
        while (!driver.findElements(By.cssSelector("a.sb_pagN")).isEmpty()) {

            driver.findElement(By.cssSelector("a.sb_pagN")).click();
            new WebDriverWait(driver, 10)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#b_results")));

            // ページ番号取得
            int currentPageNo = getCurrnetPageNo(driver);
            if (currentPageNo <= lastPageNo) {
                // 前回のページ番号以下の場合は終了
                // (次ページを押していくとページが戻る場合があるため)
                break;
            }

            resultUrls.addAll(collectResultUrls(driver));
            lastPageNo = currentPageNo;
        }

        return resultUrls;
    }

    private int getCurrnetPageNo(WebDriver driver) {

        if (driver.findElements(By.cssSelector("a.sb_pagS")).isEmpty()) {
            return 0;
        }

        return Integer.valueOf(driver.findElement(By.cssSelector("a.sb_pagS")).getText());
    }

    private List<String> collectResultUrls(WebDriver driver) {

        return driver.findElements(By.cssSelector(".b_algo .b_title a")).stream()
                .map(x -> x.getAttribute("href"))
                .collect(Collectors.toList());
    }
}
