javac -cp gson-2.8.5.jar -sourcepath src -d bin src\Crawler.java
jar cvf crawler.jar -C bin\ .
