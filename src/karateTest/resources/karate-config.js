function fn() {
    // baseUrl is injected as a JVM system property by Gradle (intTest/preprodTest tasks)
    // or set directly in DevKarateRunner before the suite runs (devTest)
    var baseUrl = karate.properties['baseUrl'] || 'http://localhost:8080';
    return { baseUrl: baseUrl };
}
