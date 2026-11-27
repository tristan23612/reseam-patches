dependencies {
    compileOnly(project(":apps:dcinside:extensions:customInterceptor"))
    compileOnly(project(":apps:dcinside:extensions:managementUi"))
    compileOnly("com.squareup.okhttp3:okhttp:5.1.0")
    compileOnly("org.jsoup:jsoup:1.23.2")
    compileOnly(project(":shared:jsoup"))
}
