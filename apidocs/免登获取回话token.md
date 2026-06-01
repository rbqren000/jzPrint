## 免登获取回话token
**接口地址** `https://school.k12m.cn/v1/third/code/thirdCodeLoginOrg`

**请求方式** `POST`

**consumes** `["application/json"]`

**produces** `["*/*"]`

**接口描述** `基于授权code获取回话token`

**请求参数**

| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型             | 
| -------- | -------- | -------- | -------- | -------------------- |
| creditCode| 授权code    | body     | true     | string |
| time| 时间戳（毫秒）    | body     | true     | long|
| sign| 签名    | body     | true     | string |


**请求示例**


```json
{
  "creditCode": "bbc15146dbf6",
  "time": 1748250831868,
  "sign": "315a4eed7bb6eb789608b936bd3a74ccf8513220c8704dce890a9fba6979cd32"
}
```



**响应状态**

| 状态码 | 说明         |
| ------ | ------------ |
| 200    | OK           |
| 400    | Bad Request   |
| 401    | Unauthorized |
| 404    | Not Found    |



**响应参数**

| 参数名称 | 参数说明                                                     | 类型           |
| -------- | ------------------------------------------------------------ | -------------- | 
|code|`'200'`为执行成功，其他都是失败  |string|
|message| 执行情况，`操作成功` 表示执行成功，其他均为异常  |string|
|data|响应内容  |object|

 

 **响应data参数**

| 字段名 | 说明 | 类型 | 示例 |
|--------|------|------|------|
| token| 回话token | string | eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9 |
| type| 用户标识符：2--普通用户 | number | 2|
| userId| 用户唯一标识符 | string  | 1920651595023712257|
| platform| 平台标识符：1--校端 | number | 1|
| needPhone| 无效字段 | boolean| false|



**响应示例**


```json
{
    "code": "'200'",
    "message": "操作成功",
    "data": {
        "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNTQ5NTU4NjQ1MDQ0NjEzMTIxIiwiaXNzIjoic2hhbmctc3R1ZHkiLCJleHAiOjE3NDgzMzcyMzIsImp0aSI6IjExMjQ0MjczMzIyNDE3MjMzOTIifQ.Qr9V9vw-TC3jm96pzwbnEBaf21Zs_bPMO3EkMmFbwtc",
        "type": 1,
        "userId": "1549558645044613121",
        "platform": null,
        "needPhone": false
    }
}

```

**响应code异常说明**

| code | 参数说明                                                     | 类型           |
| -------- | ------------------------------------------------------------ | -------------- | 
|ACCESS_THIRD_SIGN| 签名非法：`sign生成异常`  |string|
|ACCESS_THIRD_CODE|登录code非法：`code异常`  |string|


**sign生成规则说明**

**正式参数**
creditCode：`7618e66e78d1`
access_key：`DcqCiWbEDO3IdpBxwZa999rtBHGTwrGo`
access_secret：`mfbyfTMr7A10uHne80Oya8cOc3EbR9OO`
regionalismCode：`2133006025`
functionCode：`offlinePrint`

**HmacSHA256签名**
将请求参数按照`属性名=参数值`拼接得到字符串，然后拼接好后的字符串按照`Unicode码`从小到大依次进行拼接，将拼接好后的字符串前面追加`accessKey=DcqCiWbEDO3IdpBxwZa999rtBHGTwrGo`
拼接结果示例如下：`accessKey=DcqCiWbEDO3IdpBxwZa999rtBHGTwrGocreditCode=7618e66e78d1functionCode=offlinePrintregionalismCode=2133006025time={当前时间戳毫秒}`
在对拼接好后的字符串进行`md5加密`，再用HmacSHA256签名加密（key为access_secret `mfbyfTMr7A10uHne80Oya8cOc3EbR9OO`），将结果转换全小写的16进制后，得到sign

**客户端代码**

**请求代码**

```java
    public static void main(String[] args){
        OrgSignCommand command = new OrgSignCommand();
        command.setCreditCode("7618e66e78d1");
        command.setTime(System.currentTimeMillis());
        //业务标签
        command.setFunctionCode("offlinePrint");

        List<String> paramList = new ArrayList<>();
        paramList.add("creditCode=" + command.getCreditCode());
        paramList.add("time=" + command.getTime());
        //行政区划代码：简作平台提供
        paramList.add("regionalismCode=" + "2133006025");
        paramList.add("functionCode=" + command.getFunctionCode());
        paramList.sort(Comparator.comparing(String::valueOf));
        //组装
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("accessKey=").append("DcqCiWbEDO3IdpBxwZa999rtBHGTwrGo");
        keyBuilder.append(String.join("", paramList));
        log.info("请求参数：{}", keyBuilder);
        String paramSign = SignUtil.paramSign(keyBuilder.toString(), "mfbyfTMr7A10uHne80Oya8cOc3EbR9OO");
        log.info("参数签名：{}", paramSign);
        command.setSign(paramSign);
        //该测试为正式地址
        String url = "https://school.k12m.cn/v1/third/code/thirdCodeLoginOrg";
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(url, command, String.class);
        System.out.println(result);
    }

```

**SignUtil工具类**
```java
   public static String paramSign(String paramStr, String salt){
        String md5 = MD5StrUtils.encryptPass(paramStr);
        log.info("参数加密后的md5：{}", md5);
        return hmacHex(md5, salt);
    }

    /**
     * HmacSHA256 加密
     * @param data
     * @param priKey
     * @return
     */
    public static String hmacHex(String data, String priKey){
        try {
            // 创建SecretKeySpec对象
            SecretKeySpec secretKeySpec = new SecretKeySpec(priKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            // 获取Mac实例
            Mac mac = Mac.getInstance("HmacSHA256");
            // 初始化Mac对象
            mac.init(secretKeySpec);
            // 执行加密操作
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // 将结果转换为十六进制字符串
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            log.error("异常：", e);
            throw ApplicationException.build(ApplicationErrorCode.COLLATED_EDITION_INFO);
        }
    }


    // 将字节数组转换为十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

```

