

## 博芯基于学校id获取统编列表


**接口地址**:`/v1/boXin/boXinEditionList`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|schoolId|schoolId|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|RestResult«List«BoXinSemesterDTO»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||string||
|data||array|BoXinSemesterDTO|
|&emsp;&emsp;editionList||array|BoXinEditionDTO|
|&emsp;&emsp;&emsp;&emsp;editionId||integer||
|&emsp;&emsp;&emsp;&emsp;editionName||string||
|&emsp;&emsp;&emsp;&emsp;editionType||string||
|&emsp;&emsp;semesterId||integer(int32)||
|&emsp;&emsp;semesterName||string||
|message||string||
|success||boolean||


**响应示例**:
```javascript
{
	"code": "",
	"data": [
		{
			"editionList": [
				{
					"editionId": 0,
					"editionName": "",
					"editionType": ""
				}
			],
			"semesterId": 0,
			"semesterName": ""
		}
	],
	"message": "",
	"success": true
}
```

**客户端代码**

**请求代码**

```java
    public static void main(String[] args){
        String token ="通过：免登获取回话token 接口 获取token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        //封装成一个请求对象
        HttpEntity<Object> entity = new HttpEntity<>(headers);

        //该测试为正式地址
        String url = "https://school.k12m.cn/v1/boXin/boXinEditionList?schoolId=1771835848643026945";
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(url, entity, String.class);
        System.out.println(result);
    }

```