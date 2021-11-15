# JavaMavenGithubActionDemo

## 介绍
JavaMavenGithubActionDemo主要是为了测试使用Github Action来发布java maven项目。

**当前发布主要是发布在github的packages上，只要配置了github maven仓库地址即可使用已发布的资源**

## 前期配置

### maven配置
本地主机需配置maven的setting.xml文件

1. github服务器登陆信息

```xml
<servers>
   <server>
      <!-- 为了后面的同意，server.id值为github,后续有使用的地方 -->
      <id>github</id>
      <username>你的github用户名</username>
      <password>你的github personal token</password>
   </server>
</servers>
```

2. maven profiles配置

```xml
<profiles>
   <profile>
      <!-- 这一步配置是为了能从github上下载jar包 -->
      <!-- profile.id与server.id一致 -->
      <id>github</id>
      <repositories>
         <repository>
            <id>central</id>
            <url>https://repo1.maven.org/maven2</url>
         </repository>
         <repository>
            <id>github</id>
            <url>https://maven.pkg.github.com/你的github用户名/你将要发布的jar包所在仓库名</url>
            <snapshots>
               <enabled>true</enabled>
            </snapshots>
         </repository>
      </repositories>
   </profile>
</profiles>

```
如果存在多个profile需要制定活动的profile

```xml
<activeProfiles>
   <!-- actionProfile 与profile.id一致 -->
   <activeProfile>github</activeProfile>
</activeProfiles>
```

### 当前项目配置
项目的pom.xml
```xml
<distributionManagement>
   <repository>
      <!-- repository.id与server.id一致 -->
      <id>github</id>
      <name>自定义名称</name>
      <!-- repository.url与profile.repositories.repository.url一致 -->
      <url>https://maven.pkg.github.com/你的github用户名/你将要发布的jar包所在仓库名</url>
   </repository>
</distributionManagement>
```

**以上配置好就可以使用`mvn deploy`发布了,发布后可以看到对应的仓库packages有数据,会有延迟,需要多刷新几次**

### 发布脚本配置

```yaml
# 脚本名称

name: Deploy

# 触发时机
on:
  # Triggers the workflow on push or pull request events but only for the master branch
  push:
    branches: [ master ]

  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

# A workflow run is made up of one or more jobs that can run sequentially or in parallel
jobs:
  # This workflow contains a single job called "build"
  build:
    # The type of runner that the job will run on
    runs-on: ubuntu-latest

    # Steps represent a sequence of tasks that will be executed as part of the job
    steps:
      # Checks-out your repository under $GITHUB_WORKSPACE, so your job can access it
      - uses: actions/checkout@v2
      
      # 这里使用的是java 1.8版本      
      - name: Set up JDK 1.8
        uses: actions/setup-java@v1
        with:
          java-version: 1.8
      # 这里用来打包并发布且跳过单元测试
      - name: Publish package
        run: mvn clean deploy -Dmaven.test.skip=true
        env:
          # 变量github.token为github本身内含变量
          GITHUB_TOKEN: ${{ github.token }}
```

