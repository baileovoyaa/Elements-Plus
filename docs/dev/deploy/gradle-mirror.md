---
title: "本地配置 Gradle 发行版镜像"
category: "开发文档"
---

`gradlew` / `gradlew.bat` 会依次读取下面的配置来覆盖 `gradle/wrapper/gradle-wrapper.properties`
里的 `distributionUrl`（不配置则使用文件中的官方地址）：

1. 环境变量 `GRADLE_DISTRIBUTION_URL`
2. 仓库根目录 `local.properties`（已被 `.gitignore` 忽略）中的 `gradle.distributionUrl`

```properties
# local.properties
gradle.distributionUrl=https://mirrors.aliyun.com/gradle/distributions/v9.5.1/gradle-9.5.1-bin.zip
```

不修改 `gradle-wrapper.properties`、不影响 CI 和其他人，本地想用镜像时只改 `local.properties` 即可。
