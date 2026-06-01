# 简作 (jzPrint)

## 项目概述

简作是为魔方谈定制的校本作业打印 App，基于 TaxTicketPrinter 项目复制而来。
设备选择、AP 连接及配网逻辑已从 inksi 中提取并独立，脱离了 inksi 的复杂体系。

## 项目信息

| 属性 | 值 |
|------|-----|
| 项目名称 | 简作 |
| 包名 | com.org.jzprinter |
| 原 TaxTicketPrinter 包名 | com.rbq.taxticketprinter |
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 36 |
| Java 版本 | 17 |
| Gradle 插件版本 | 8.10.1 |

## 模块结构

| 模块 | 类型 | 说明 |
|------|------|------|
| app | application | 主应用模块 |
| mxSdk | library | 打印 SDK（蓝牙BLE/SPP/WiFi通信、OpenCV、串口等） |
| serialport | library | 串口通信库 |
| opencv | library | OpenCV 库 |
| NanoHttpd | library | 嵌入式 Web 服务器 |

## 业务流程

### 1. 小程序分享

小程序分享学校信息，格式为 JSON：

```json
{
  "schoolId": "1234567890abcd"
}
```

### 2. 查询校本作业列表

根据 `schoolId` 请求接口，返回学期及校本列表：

```json
[
  {
    "semesterId": 123,
    "semesterName": "学期名称",
    "editionList": [
      {
        "editionId": 1234,
        "editionName": "校本名称",
        "editionType": 1
      }
    ]
  }
]
```

- `editionType = 1`：指定学生铺码
- `editionType = 2`：指定预铺码

### 3. 获取班级/学生信息（editionType = 1）

根据 `schoolId` + `editionId` 查询，返回班级-学生结构：

```json
[
  {
    "classId": "1234567890qwerc",
    "className": "一班",
    "studentList": [
      {
        "studentId": "1234567890qwerc",
        "studentName": "张三"
      }
    ]
  }
]
```

### 4. 获取预铺码编码（editionType = 2）

根据 `schoolId` + `editionId` 查询，返回预铺码列表：

```json
[
  {
    "prepareCode": "EC100000007"
  }
]
```

### 5. 下载打印文件

- **指定学生铺码**：根据 `schoolId` + `editionId` + `studentId` 获取文件
- **指定预铺码**：根据 `schoolId` + `editionId` + `prepareCode` 获取文件

文件内容可以是二进制流、文件地址或其他形式，具体结构以实际对接为准。

## 打印内容说明

客户提供的打印内容示例存放于 `抠图拼接无间隔` 目录中：
- 外层图片 `page_页码.png` 为合成后的完整页面
- 对应的 `page_页码` 文件夹内存放该页拆分后的单张图片
# jzPrint
