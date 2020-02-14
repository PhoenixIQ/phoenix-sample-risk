# 事中风控(2): Phoenix工程搭建
## 前言
本篇是使用phoenix开发高性能事中风控服务系列第一篇，该系列一共分为五篇文章介绍，本篇主要介绍如何搭建一个phoenix工程，以及工程内各个模块和包的介绍。

- 第一篇：[背景和业务介绍](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-1)
- 第二篇：[phoenix工程搭建](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-2)
- 第三篇：[领域设计与消息定义](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-3)
- 第四篇：[领域对象定义](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-4)
- 第五篇：[客户端代码编写](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-5)

## 工程搭建
搭建Phoenix工程十分容易，可以使用下述命令即可生成一个完整的phoenix的maven工程。

``` shell
mvn -X archetype:generate \
 -DarchetypeGroupId=com.iquantex \
 -DarchetypeArtifactId=phoenix-archetype \
 -DarchetypeVersion=dev-SNAPSHOT \
 -Dversion=1.0-SNAPSHOT \
 -DgroupId=com.iquantex.phoenix.risk \
 -DartifactId=phoenix-risk \
 -DinteractiveMode=false
```

生成成功后效果图如下
``` shell
Initialized empty Git repository in /Users/baozi/workspace/quantex/back/phoenix-risk/phoenix-risk/.git/
       _                       _
      | |                     (_)
 ____ | |__   ___  _____ ____  _ _   _
|  _ \|  _ \ / _ \| ___ |  _ \| ( \ / )
| |_| | | | | |_| | ____| | | | |) X (
|  __/|_| |_|\___/|_____)_| |_|_(_/ \_)
|_|         Phoenix:(2.1.1)
[INFO] Project created from Archetype in dir: /Users/baozi/workspace/quantex/back/phoenix-risk/phoenix-risk
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 3.965 s
[INFO] Finished at: 2020-02-12T14:24:26+08:00
[INFO] ------------------------------------------------------------------------

```

## 模块介绍
Phoenix开发工程奔着模块自治的思想，把分为了三个子Module，依赖关系如下:
```shell
                   +----------------+
                   |   application  |
                   +-----+----+-----+
                         |    |
                 +-------+    +------+
                 |                   |
          +------v-----+     +-------v-------+
          |    domain  <-----+  coreapi      |
          +------+-----+     +-------+-------+
```

### application - 启动模块
应用的顶层模块，启动模块，入口模块，包括：
- SpringBoot启动类，启动配置等
- 用户交互层（Web、RESTFul API）

``` shell
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com.iquantex.phoenix.risk
    │   │       ├── PhoenixriskApplication.java   # spring启动类
    │   │       ├── controller
    │   │       │   ├── HelloController.java      # 交互层类 
    │   │       └── runner
    │   │           └── Runner.java               # phoenix启动类
    │   └── resources
    │       ├── application.yaml                  # 配置文件
    │       ├── logback.xml                       # 日志配置
```

### coreapi - 消息定义模块
应用的消息定义模块，包括：
- cmd:   聚合根入口命令
- event: 聚合根处理后事件

```shell
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com.iquantex.phoenix.risk.coreapi
    │   │       ├── Hello.java     # 消息定义(命令和事件)
    │   │       └── description.md
    │   └── resources
    │   │       └── Hello.proto    # protobuf定义

```


### domain - 领域模块
phoenix业务领域核心模块，包括：
- 聚合根： 核心业务领域聚合根，处理core中的命令并返回事件
- 聚合根测试：针对聚合根的完整测试
- 依赖服务： 聚合根计算过程中依赖的服务逻辑

``` shell
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com.iquantex.phoenix.risk
    │   │     └── domain
    │   │         ├── entity                       # 聚合实体定义包
    │   │         │   ├── HelloAggregate.java      # 聚合根定义(特殊的实体)
    │   │         │   └── description.md          
    │   │         └── service
    │   │             └── description.md
    │   └── resources
    └── test
        ├── java.com.iquantex.phoenix.risk
        │    └── domain
        │        └── HelloAggregateTest.java       # 聚合根测试
```

### tools - 工具包
包含常用的工具脚本

```shell 
.
├── build-restart  # 便捷打包重启脚本
├── gen_proto      # protobuf生成脚本
└── maven-deploy   # 便捷发布coreapi脚本
```

## 运行演示
1. 运行：`sh tools/build-restart`

2. 执行请求
``` shell
➜  ~ curl -X PUT http://127.0.0.1:8080/hello/H001
success
```

3. 查看日志
``` shell
INFO 80357 --- [t-dispatcher-17] [c.i.p.b.domain.entity.HelloAggregate       33] : Hello World Phoenix...
```

## 结尾
本篇演示了如何如何构建一个phoenix工程，并详细介绍了每个Module和每个包的定义。现在我们有了一个一个可运行的phoenix工程，下篇我们将使用该工程从领域设计到代码落地完善工程。
