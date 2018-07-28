package com.enjoyxstudy.filesearch.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import lombok.Getter;
import lombok.SneakyThrows;

public class BingSearcher implements Searcher {

    @Getter
    private final String name = "Bing";

    @SneakyThrows
    public List<String> search(WebDriver driver, String query) {

        List<String> resultUrls = new ArrayList<>();

        driver.get("https://www.bing.com/?FORM=&setmkt=en-us&setlang=en-us");

        WebElement inputElement = driver.findElement(By.cssSelector("#sb_form_q"));
        inputElement.sendKeys(query);

        // 補完が出てくる場合があるので、ESCキーで非表示へ
        // (検索ボタンが隠れると押せなくなるので)
        inputElement.sendKeys(Keys.chord(Keys.ESCAPE));

        driver.findElement(By.cssSelector("#sb_form_go")).click();

        resultUrls.addAll(collectResultUrls(driver));
        int lastPageNo = 1;

        // 次ページのリンクがなくなるまで繰り返し
        while (!driver.findElements(By.cssSelector("a.sb_pagN")).isEmpty()) {

            driver.findElement(By.cssSelector("a.sb_pagN")).click();

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

        return driver.findElements(By.cssSelector(".b_algo a")).stream()
                .map(x -> x.getAttribute("href"))
                .collect(Collectors.toList());
    }
}
