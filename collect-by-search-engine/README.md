# collect-by-search-engine

```
gradlew shadowJar
java -jar build/libs/collect-by-search-engine-all.jar -e Google -o output "filetype:pdf test1" "filetype:pdf test2"
```

* `-e <engine>`<br>
Search engine name (Google or Bing)
* `-o <output>`<br>
Output directory
