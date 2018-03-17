package com.enjoyxstudy.filesearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class GoogleSearcherTest {

    @Test
    public void search() {

        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe");

        List<String> results = new GoogleSearcher().search("filetype:pdf テスト");

        assertThat(results)
                .isNotEmpty();
    }

}
