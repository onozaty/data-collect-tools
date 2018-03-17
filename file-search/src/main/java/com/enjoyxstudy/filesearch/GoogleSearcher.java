package com.enjoyxstudy.filesearch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class GoogleSearcher {

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

            WebElement buttonElement = driver.findElement(By.cssSelector("input[name=btnK]"));
            buttonElement.click();

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

    private List<String> collectResultUrls(WebDriver driver) {

        return driver.findElements(By.xpath("//h3[@class=\"r\"]/a")).stream()
                .map(x -> x.getAttribute("href"))
                .collect(Collectors.toList());
    }
}
