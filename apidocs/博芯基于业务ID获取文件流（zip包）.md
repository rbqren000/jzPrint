

## 博芯基于业务ID获取文件流（zip包）


**接口地址**:`/v1/boXin/editionStudentZip`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**: 基于业务ID获取统编素材zip包。businessId有两个来源：
1. 从 `boXinRosterList` 接口响应中获取（StudentInfo.businessId 或 StudentDetail.businessId）
2. 由小程序分享传入（DeepLink启动时携带）


**请求参数**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|businessId|业务ID，来源：boXinRosterList响应 或 小程序分享传入|query|true|string||
|editionType|editionType|query|true|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


暂无


**响应示例**:
```javascript

```

**客户端代码**

**请求代码**

```java
public static void main(String[] args) throws IOException {
    String token = "通过：免登获取回话token接口获取token";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", token);

    // 方式一：GET + 字节数组（5-10M 可以用，简单）
    String url = "https://school.k12m.cn/v1/boXin/editionStudentZip?schoolId=1771835848643026945&businessId=69ddf8b14588c26b3f6ffa89&editionType=1";

    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<byte[]> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

    byte[] bytes = response.getBody();
    if (bytes != null) {
        try (FileOutputStream fos = new FileOutputStream("editionStudentZip.zip")) {
            fos.write(bytes);
        }
        System.out.println("文件下载完成，大小：" + bytes.length / 1024 / 1024 + "MB");
    }
}

```