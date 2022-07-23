# BiliRec

一个能够录制B站直播的工具。

## 特点

- 自动监控并录制直播

  相比传统使用屏幕录制的方式，在更小的资源占用情况下能获得更好的录制效果

- 纯 Kotlin 编写，没有原生依赖，不需要任何第三方软件依赖

- 能够同时录制视频和弹幕，包括上舰/礼物/SuperChat记录

- 能自动修复视频时间戳，防止出现视频时长异常/播放时跳帧/不能拖动进度条的问题

## 使用

### 先决条件

Java17 运行环境，[下载地址](https://jdk.java.net/archive/)

**本项目不支持 Java8**，请使用 Java17 运行

### 开始使用

完整的启动命令

```shell
java --add-opens java.xml/com.sun.xml.internal.stream.writers=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED -Dlog4j.skipJansi=false -jar <Jar名称> -c /work/dir/path
```

#### 命令说明

1. 因 Java9 及之后对反射操作有限制，所以需要添加 JVM 启动参数

   `--add-opens java.xml/com.sun.xml.internal.stream.writers=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED`

2. 控制台会打印日志，如果需要开启颜色显示。则还需要添加 JVM 参数

   `-Dlog4j.skipJansi=false`

3. **必须**指定工作目录，方式是使用 `-c` 参数

4. 可使用下面的命令查看程序参数

   `java -jar <jar>`

### 配置

> 当前配置文件版本为 `1`

程序运行时，会从工作路径下寻找 `config.yaml` 文件，如果运行时没有，则会生成一个空的配置文件

配置好后重新运行即可

#### 配置文件示例

以下是一个典型的配置文件

```yaml
roomConfigs: 
-  roomId: 8040093 # 直播间ID，长短号均可
   title: "HelloWorld" # 直播间标题，可不填，自动获取
   enableAutoRecord: true # 是否启用自动录制，默认为true
   recordRawDanmakuData: true # 是否记录原始弹幕数据，关闭可节省磁盘空间，默认为true
   recordGuardByData: true # 是否记录上舰记录，默认为true
   recordSendGiftData: true #是否记录礼物记录，默认为true
   recordSuperChatData: true #是否记录SC，默认为true
   filterLotteryDanmaku: true # 是否屏蔽抽奖弹幕，默认为true
   danmakuFilterRegex: # 需要屏蔽掉弹幕的正则表达式，和 filterLotteryDanmaku 相互独立，留空则表示不屏蔽任何弹幕
     - "红包"
   # 视频录制模式，默认为0
   # 0 标准模式，此时会自动修复时间戳
   # 1 原始录制模式，不会对视频数据进行任何处理
   recordMode: 0 

# 如果有多个直播间，可按照上面的格式进行填写，每个直播间的配置相互独立
version: 1
```

简略版本配置文件

```yaml
roomConfigs: 
-  roomId: 8040093
version: 1
```

#### 备注

程序运行后会自动处理配置文件，包括：

- 自动删除里面的注释，因此请不要在里面留下任何注释

- 自动写入直播间标题

- 如果直播间ID是短号，将会变为长号

## 存在的问题

本项目目前存在的问题

- 无法录制付费直播

- 无法选择录制画质

另外，因为每个地区的CDN状态不同，可能会出现但不限于以下情况

- 无法自动修复视频文件

- IP被限制，无法录制

并且，该项目随时可能由于包括但不限于以下原因中的的一个或多个而终止

- 高考

- 考研

- 电脑坏了

- 历史遗留问题太多，不想解决

- 官方封锁

## 鸣谢

本项目中的部分代码和逻辑参考了 [BililiveRecorder/BililiveRecorder](https://github.com/BililiveRecorder/BililiveRecorder)

在此表示感谢

## License

Copyright (C) 2022 PeanutMelonSeedBigAlmond

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License Version 3 as published by the Free Software Foundation.

You should have received a copy of the GNU Affero General Public License along with this program. If not,
see <https://www.gnu.org/licenses/>.