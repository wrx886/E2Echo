# E2Echo Server

**简介**:E2Echo 后端项目以 ECC（采用 secp521r1 曲线）加密技术为核心，严格遵循“私钥不上服务端”和“用户间公钥线下交换”的安全原则，为用户提供了一种高度安全的端到端加密聊天解决方案。

**Version**:0.0.1-alpha

**备注**:发送手机验证码的逻辑（com.github.wrx886.e2echo.server.web.service.SmsService）没有实现，而是将信息打印到控制台。

[TOC]


# 用户登入相关接口


## 登入（或注册）并获取 access-token


**接口地址**:`/server/login/login`

**请求方式**:`POST`

**请求数据类型**:`application/x-www-form-urlencoded,application/json`

**响应数据类型**:`*/*`


**请求示例**:


```javascript
{
  "phone": "",
  "phoneCode": "",
  "signCodeKey": "",
  "signCodeValue": "",
  "publicKey": ""
}
```


**请求参数**:


| 参数名称                  | 参数说明                   | 请求类型 | 是否必须 | 数据类型 | schema  |
| ------------------------- | -------------------------- | -------- | -------- | -------- | ------- |
| loginVo                   | 登入参数                   | body     | true     | LoginVo  | LoginVo |
| &emsp;&emsp;phone         | 手机号                     |          | true     | string   |         |
| &emsp;&emsp;phoneCode     | 手机验证码                 |          | true     | string   |         |
| &emsp;&emsp;signCodeKey   | 签名码的key                |          | true     | string   |         |
| &emsp;&emsp;signCodeValue | 使用私钥加密后的签名码的值 |          | true     | string   |         |
| &emsp;&emsp;publicKey     | 公钥                       |          | true     | string   |         |


**响应状态**:


| 状态码 | 说明 | schema       |
| ------ | ---- | ------------ |
| 200    | OK   | ResultString |


**响应参数**:


| 参数名称 | 参数说明                  | 类型           | schema         |
| -------- | ------------------------- | -------------- | -------------- |
| code     | 返回码                    | integer(int32) | integer(int32) |
| message  | 返回消息                  | string         |                |
| data     | 返回数据，即 access-token | string         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": ""
}
```


## 获取签名码


**接口地址**:`/server/login/getSignCode`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema           |
| ------ | ---- | ---------------- |
| 200    | OK   | ResultSignCodeVo |


**响应参数**:


| 参数名称          | 参数说明                               | 类型           | schema         |
| ----------------- | -------------------------------------- | -------------- | -------------- |
| code              | 返回码                                 | integer(int32) | integer(int32) |
| message           | 返回消息                               | string         |                |
| data              |                                        | SignCodeVo     | SignCodeVo     |
| &emsp;&emsp;key   | 签名码的 key                           | string         |                |
| &emsp;&emsp;value | 签名码的值，需要使用私钥进行加密后返回 | string         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"key": "",
		"value": ""
	}
}
```


## 获取手机验证码


**接口地址**:`/server/login/getPhoneCode`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`

**响应数据类型**:`*/*`


**请求参数**:


| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
| -------- | -------- | -------- | -------- | -------- | ------ |
| phone    | 手机号   | query    | true     | string   |        |


**响应状态**:


| 状态码 | 说明 | schema     |
| ------ | ---- | ---------- |
| 200    | OK   | ResultVoid |


**响应参数**:


| 参数名称 | 参数说明 | 类型           | schema         |
| -------- | -------- | -------------- | -------------- |
| code     | 返回码   | integer(int32) | integer(int32) |
| message  | 返回消息 | string         |                |
| data     | 返回数据 | object         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


# 文件管理


## 上传文件


**接口地址**:`/server/file/upload`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`

**响应数据类型**:`*/*`


**请求参数**:


| 参数名称 | 参数说明   | 请求类型 | 是否必须 | 数据类型 | schema |
| -------- | ---------- | -------- | -------- | -------- | ------ |
| file     | 上传的文件 | query    | true     | file     |        |

**响应状态**:


| 状态码 | 说明 | schema       |
| ------ | ---- | ------------ |
| 200    | OK   | ResultString |


**响应参数**:


| 参数名称 | 参数说明 | 类型           | schema         |
| -------- | -------- | -------------- | -------------- |
| code     | 返回码   | integer(int32) | integer(int32) |
| message  | 返回消息 | string         |                |
| data     | 返回数据 | string         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": ""
}
```


# 消息收发


## 发送消息


**接口地址**:`/server/message/send`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`

**响应数据类型**:`*/*`


**请求示例**:


```javascript
{
  "id": 0,
  "fromPublicKey": "",
  "toPublicKey": "",
  "fromUserId": 0,
  "toUserId": 0,
  "sign": "",
  "data": "",
  "sendTime": "",
  "messageType": ""
}
```


**请求参数**:


| 参数名称                  | 参数说明                               | 请求类型 | 是否必须 | 数据类型          | schema  |
| ------------------------- | -------------------------------------- | -------- | -------- | ----------------- | ------- |
| message                   | 消息实体                               | body     | true     | Message           | Message |
| &emsp;&emsp;id            | 自增主键                               |          | false    | integer(int64)    |         |
| &emsp;&emsp;fromPublicKey | 发送者公钥                             |          | true     | string            |         |
| &emsp;&emsp;toPublicKey   | 接收者公钥                             |          | true     | string            |         |
| &emsp;&emsp;fromUserId    | 发送者用户id（根据公钥自动填充）       |          | false    | integer(int64)    |         |
| &emsp;&emsp;toUserId      | 接收者用户id（根据公钥自动填充）       |          | false    | integer(int64)    |         |
| &emsp;&emsp;sign          | 数据的签名（HEX 格式）                 |          | true     | string            |         |
| &emsp;&emsp;data          | 数据（使用接收者的公钥加密，HEX 格式） |          | true     | string            |         |
| &emsp;&emsp;sendTime      | 发送时间                               |          | true     | string(date-time) |         |
| &emsp;&emsp;messageType   | 消息类型枚举,可用值:USER,GROUP         |          | true     | string            |         |


**响应状态**:


| 状态码 | 说明 | schema     |
| ------ | ---- | ---------- |
| 200    | OK   | ResultVoid |


**响应参数**:


| 参数名称 | 参数说明 | 类型           | schema         |
| -------- | -------- | -------------- | -------------- |
| code     | 返回码   | integer(int32) | integer(int32) |
| message  | 返回消息 | string         |                |
| data     | 返回数据 | object         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 接收消息

**接口地址**:`/server/message/receive`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`

**响应数据类型**:`*/*`


**请求示例**:


```javascript
{
  "startTime": "",
  "messageType": ""
}
```


**请求参数**:


| 参数名称                | 参数说明                       | 请求类型 | 是否必须 | 数据类型          | schema    |
| ----------------------- | ------------------------------ | -------- | -------- | ----------------- | --------- |
| messageVo               | 获取消息的参数                 | body     | true     | MessageVo         | MessageVo |
| &emsp;&emsp;startTime   | 起始时间                       |          | true     | string(date-time) |           |
| &emsp;&emsp;messageType | 消息类型枚举,可用值:USER,GROUP |          | false    | string            |           |


**响应状态**:


| 状态码 | 说明 | schema            |
| ------ | ---- | ----------------- |
| 200    | OK   | ResultListMessage |


**响应参数**:


| 参数名称                  | 参数说明                               | 类型              | schema         |
| ------------------------- | -------------------------------------- | ----------------- | -------------- |
| code                      | 返回码                                 | integer(int32)    | integer(int32) |
| message                   | 返回消息                               | string            |                |
| data                      | 返回数据                               | array             | Message        |
| &emsp;&emsp;id            | 自增主键                               | integer(int64)    |                |
| &emsp;&emsp;fromPublicKey | 发送者公钥                             | string            |                |
| &emsp;&emsp;toPublicKey   | 接收者公钥                             | string            |                |
| &emsp;&emsp;fromUserId    | 发送者用户id（根据公钥自动填充）       | integer(int64)    |                |
| &emsp;&emsp;toUserId      | 接收者用户id（根据公钥自动填充）       | integer(int64)    |                |
| &emsp;&emsp;sign          | 数据的签名（HEX 格式）                 | string            |                |
| &emsp;&emsp;data          | 数据（使用接收者的公钥加密，HEX 格式） | string            |                |
| &emsp;&emsp;sendTime      | 发送时间                               | string(date-time) |                |
| &emsp;&emsp;messageType   | 消息类型枚举,可用值:USER,GROUP         | string            |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"id": 0,
			"fromPublicKey": "",
			"toPublicKey": "",
			"fromUserId": 0,
			"toUserId": 0,
			"sign": "",
			"data": "",
			"sendTime": "",
			"messageType": ""
		}
	]
}
```


# 用户管理


## 换绑业务


**接口地址**:`/server/user/change`


**请求方式**:`POST`

**请求数据类型**:`application/x-www-form-urlencoded,application/json`

**响应数据类型**:`*/*`

**请求示例**:


```javascript
{
  "oldCode": "",
  "newPhone": "",
  "newCode": "",
  "signCodeKey": "",
  "signCodeValue": ""
}
```


**请求参数**:


| 参数名称                  | 参数说明                   | 请求类型 | 是否必须 | 数据类型     | schema       |
| ------------------------- | -------------------------- | -------- | -------- | ------------ | ------------ |
| userChangeVo              | 换绑业务参数               | body     | true     | UserChangeVo | UserChangeVo |
| &emsp;&emsp;oldCode       | 旧手机号的验证码           |          | true     | string       |              |
| &emsp;&emsp;newPhone      | 新手机号                   |          | true     | string       |              |
| &emsp;&emsp;newCode       | 新手机号的验证码           |          | true     | string       |              |
| &emsp;&emsp;signCodeKey   | 签名码的 key               |          | true     | string       |              |
| &emsp;&emsp;signCodeValue | 使用私钥加密后的签名码的值 |          | true     | string       |              |


**响应状态**:


| 状态码 | 说明 | schema     |
| ------ | ---- | ---------- |
| 200    | OK   | ResultVoid |


**响应参数**:


| 参数名称 | 参数说明 | 类型           | schema         |
| -------- | -------- | -------------- | -------------- |
| code     | 返回码   | integer(int32) | integer(int32) |
| message  | 返回消息 | string         |                |
| data     | 返回数据 | object         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 获取登入用户信息


**接口地址**:`/server/user/info`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`

**响应数据类型**:`*/*`


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema     |
| ------ | ---- | ---------- |
| 200    | OK   | ResultUser |


**响应参数**:


| 参数名称              | 参数说明        | 类型           | schema         |
| --------------------- | --------------- | -------------- | -------------- |
| code                  | 返回码          | integer(int32) | integer(int32) |
| message               | 返回消息        | string         |                |
| data                  |                 | User           | User           |
| &emsp;&emsp;id        | 自增主键        | integer(int64) |                |
| &emsp;&emsp;phone     | 用户的手机号    | string         |                |
| &emsp;&emsp;publicKey | ECC521 HEX 公钥 | string         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"phone": "",
		"publicKey": ""
	}
}
```


## 换绑业务：获取签名码


**接口地址**:`/server/user/getSignCodeOfChange`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`

**响应数据类型**:`*/*`


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema           |
| ------ | ---- | ---------------- |
| 200    | OK   | ResultSignCodeVo |


**响应参数**:


| 参数名称          | 参数说明                               | 类型           | schema         |
| ----------------- | -------------------------------------- | -------------- | -------------- |
| code              | 返回码                                 | integer(int32) | integer(int32) |
| message           | 返回消息                               | string         |                |
| data              |                                        | SignCodeVo     | SignCodeVo     |
| &emsp;&emsp;key   | 签名码的 key                           | string         |                |
| &emsp;&emsp;value | 签名码的值，需要使用私钥进行加密后返回 | string         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"key": "",
		"value": ""
	}
}
```


## 换绑业务：获取原手机号的验证码


**接口地址**:`/server/user/getOldCodeOfChange`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema     |
| ------ | ---- | ---------- |
| 200    | OK   | ResultVoid |


**响应参数**:


| 参数名称 | 参数说明 | 类型           | schema         |
| -------- | -------- | -------------- | -------------- |
| code     | 返回码   | integer(int32) | integer(int32) |
| message  | 返回消息 | string         |                |
| data     | 返回数据 | object         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```


## 换绑业务：换取新手机验证码


**接口地址**:`/server/user/getNewCodeOfChange`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`

**响应数据类型**:`*/*`

**请求参数**:


| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
| -------- | -------- | -------- | -------- | -------- | ------ |
| phone    | 新手机号 | query    | true     | string   |        |

**响应状态**:


| 状态码 | 说明 | schema     |
| ------ | ---- | ---------- |
| 200    | OK   | ResultVoid |


**响应参数**:


| 参数名称 | 参数说明 | 类型           | schema         |
| -------- | -------- | -------------- | -------------- |
| code     | 返回码   | integer(int32) | integer(int32) |
| message  | 返回消息 | string         |                |
| data     | 返回数据 | object         |                |


**响应示例**:

```javascript
{
	"code": 0,
	"message": "",
	"data": {}
}
```

# 消息收发 WebSocket

**端点**:`/server/message`

**请求数据类型**:`json`

**响应数据类型**:`json`

## 登入

**请求示例**:

```json
{
    id: "",
    "command": "login",
    "data": ""
}
```

**请求参数**:

| 参数名称 | 参数说明               | 是否必须 | 数据类型 | schema |
| -------- | ---------------------- | -------- | -------- | ------ |
| id       | 请求 id                | true     | string   |        |
| command  | 请求命令，必须为 login | true     | string   |        |
| data     | 用户的 access-token    | true     | string   |        |

**响应状态**:


| 状态码 | 说明 | schema              |
| ------ | ---- | ------------------- |
| 200    | OK   | WebSocketResultVoid |

**响应参数**:


| 参数名称 | 参数说明     | 类型           | schema         |
| -------- | ------------ | -------------- | -------------- |
| id       | 请求/响应 id | string         |                |
| command  | 命令         | string         |                |
| code     | 返回码       | integer(int32) | integer(int32) |
| message  | 返回消息     | string         |                |
| data     | 返回数据     | object         |                |

**响应示例**:

```json
{
    "id": "",
    "command": "login",
    "code": 0,
    "message": "",
    "data": {}
}
```

## 发送消息

**请求示例**:

```json
{
    "id": "",
    "command": "send",
    "data": {
        "id": 0,
        "fromPublicKey": "",
        "toPublicKey": "",
        "fromUserId": 0,
        "toUserId": 0,
        "sign": "",
        "data": "",
        "sendTime": "",
        "messageType": ""
    }
}
```

**请求参数**:

| 参数名称                  | 参数说明                               | 是否必须 | 数据类型          | schema  |
| ------------------------- | -------------------------------------- | -------- | ----------------- | ------- |
| id                        | 请求 id                                | true     | truestring        |         |
| command                   | 请求命令，必须为 send                  | true     | string            |         |
| data                      | 消息数据                               | true     | Message           | Message |
| &emsp;&emsp;id            | 自增主键                               | false    | integer(int64)    |         |
| &emsp;&emsp;fromPublicKey | 发送者公钥                             | true     | string            |         |
| &emsp;&emsp;toPublicKey   | 接收者公钥                             | true     | string            |         |
| &emsp;&emsp;fromUserId    | 发送者用户id（根据公钥自动填充）       | false    | integer(int64)    |         |
| &emsp;&emsp;toUserId      | 接收者用户id（根据公钥自动填充）       | false    | integer(int64)    |         |
| &emsp;&emsp;sign          | 数据的签名（HEX 格式）                 | true     | string            |         |
| &emsp;&emsp;data          | 数据（使用接收者的公钥加密，HEX 格式） | true     | string            |         |
| &emsp;&emsp;sendTime      | 发送时间                               | true     | string(date-time) |         |
| &emsp;&emsp;messageType   | 消息类型枚举,可用值:USER,GROUP         | true     | string            |         |

**响应状态**:


| 状态码 | 说明 | schema              |
| ------ | ---- | ------------------- |
| 200    | OK   | WebSocketResultVoid |

**响应参数**:


| 参数名称 | 参数说明     | 类型           | schema         |
| -------- | ------------ | -------------- | -------------- |
| id       | 请求/响应 id | string         |                |
| command  | 命令         | string         |                |
| code     | 返回码       | integer(int32) | integer(int32) |
| message  | 返回消息     | string         |                |
| data     | 返回数据     | object         |                |

**响应示例**:

```json
{
    "id": "",
    "command": "send",
    "code": 0,
    "message": "",
    "data": {}
}
```

## 接收消息


**请求示例**:


```json
{
    "id": "",
    "command": "receive",
    "data": {
        "startTime": "",
        "messageType": ""
    }
}
```

**请求参数**:


| 参数名称                | 参数说明                       | 是否必须 | 数据类型          | schema    |
| ----------------------- | ------------------------------ | -------- | ----------------- | --------- |
| id                      | 请求 id                        | true     | string            |           |
| command                 | 请求命令，必须为 receive       | true     | string            |           |
| data                    | 获取消息的参数                 | true     | MessageVo         | MessageVo |
| &emsp;&emsp;startTime   | 起始时间                       | true     | string(date-time) |           |
| &emsp;&emsp;messageType | 消息类型枚举,可用值:USER,GROUP | false    | string            |           |


**响应状态**:


| 状态码 | 说明 | schema                 |
| ------ | ---- | ---------------------- |
| 200    | OK   | WebSocketResultMessage |

**响应参数**:


| 参数名称                  | 参数说明                               | 类型              | schema         |
| ------------------------- | -------------------------------------- | ----------------- | -------------- |
| id                        | 请求/响应 id                           | string            |                |
| command                   | 命令                                   | string            |                |
| code                      | 返回码                                 | integer(int32)    | integer(int32) |
| message                   | 返回消息                               | string            |                |
| data                      | 返回数据                               | array             | Message        |
| &emsp;&emsp;id            | 自增主键                               | integer(int64)    |                |
| &emsp;&emsp;fromPublicKey | 发送者公钥                             | string            |                |
| &emsp;&emsp;toPublicKey   | 接收者公钥                             | string            |                |
| &emsp;&emsp;fromUserId    | 发送者用户id（根据公钥自动填充）       | integer(int64)    |                |
| &emsp;&emsp;toUserId      | 接收者用户id（根据公钥自动填充）       | integer(int64)    |                |
| &emsp;&emsp;sign          | 数据的签名（HEX 格式）                 | string            |                |
| &emsp;&emsp;data          | 数据（使用接收者的公钥加密，HEX 格式） | string            |                |
| &emsp;&emsp;sendTime      | 发送时间                               | string(date-time) |                |
| &emsp;&emsp;messageType   | 消息类型枚举,可用值:USER,GROUP         | string            |                |


**响应示例**:

```json
{
    "id": "",
    "command": "receive",
    "code": 0,
    "message": "",
    "data": [
        {
            "id": 0,
            "fromPublicKey": "",
            "toPublicKey": "",
            "fromUserId": 0,
            "toUserId": 0,
            "sign": "",
            "data": "",
            "sendTime": "",
            "messageType": ""
        }
    ]
}
```

## 配置自动接收

**请求示例**:


```json
{
    "id": "",
    "command": "registryAutoReceive",
    "data": ""
}
```

**请求参数**:


| 参数名称 | 参数说明                             | 是否必须 | 数据类型 | schema      |
| -------- | ------------------------------------ | -------- | -------- | ----------- |
| id       | 请求 id                              | true     | string   |             |
| command  | 请求命令，必须为 registryAutoReceive | true     | string   |             |
| data     | 接收消息类型枚举,可用值:USER,GROUP   | true     | string   | MessageType |

**响应状态**:


| 状态码 | 说明 | schema              |
| ------ | ---- | ------------------- |
| 200    | OK   | WebSocketResultVoid |

**响应参数**:


| 参数名称 | 参数说明     | 类型           | schema         |
| -------- | ------------ | -------------- | -------------- |
| id       | 请求/响应 id | string         |                |
| command  | 命令         | string         |                |
| code     | 返回码       | integer(int32) | integer(int32) |
| message  | 返回消息     | string         |                |
| data     | 返回数据     | object         |                |

**响应示例**:

```json
{
    "id": "",
    "command": "registryAutoReceive",
    "code": 0,
    "message": "",
    "data": {}
}
```

## 取消配置自动接收

**请求示例**:


```json
{
    "id": "",
    "command": "cancelAutoReceive",
    "data": ""
}
```

**请求参数**:


| 参数名称 | 参数说明                           | 是否必须 | 数据类型 | schema |
| -------- | ---------------------------------- | -------- | -------- | ------ |
| id       | 请求 id                            | true     | string   |        |
| command  | 请求命令，必须为 cancelAutoReceive | true     | string   |        |
| data     | 请求数据                           | false    | string   |        |

**响应状态**:


| 状态码 | 说明 | schema              |
| ------ | ---- | ------------------- |
| 200    | OK   | WebSocketResultVoid |

**响应参数**:


| 参数名称 | 参数说明     | 类型           | schema         |
| -------- | ------------ | -------------- | -------------- |
| id       | 请求/响应 id | string         |                |
| command  | 命令         | string         |                |
| code     | 返回码       | integer(int32) | integer(int32) |
| message  | 返回消息     | string         |                |
| data     | 返回数据     | object         |                |

**响应示例**:

```json
{
    "id": "",
    "command": "cancelAutoReceive",
    "code": 0,
    "message": "",
    "data": {}
}
```

## 自动接收

**响应状态**:


| 状态码 | 说明 | schema                 |
| ------ | ---- | ---------------------- |
| 200    | OK   | WebSocketResultMessage |

**响应参数**:


| 参数名称                  | 参数说明                               | 类型              | schema         |
| ------------------------- | -------------------------------------- | ----------------- | -------------- |
| id                        | 请求/响应 id                           | string            |                |
| command                   | 命令                                   | string            |                |
| code                      | 返回码                                 | integer(int32)    | integer(int32) |
| message                   | 返回消息                               | string            |                |
| data                      | 返回数据                               | Message           | Message        |
| &emsp;&emsp;id            | 自增主键                               | integer(int64)    |                |
| &emsp;&emsp;fromPublicKey | 发送者公钥                             | string            |                |
| &emsp;&emsp;toPublicKey   | 接收者公钥                             | string            |                |
| &emsp;&emsp;fromUserId    | 发送者用户id（根据公钥自动填充）       | integer(int64)    |                |
| &emsp;&emsp;toUserId      | 接收者用户id（根据公钥自动填充）       | integer(int64)    |                |
| &emsp;&emsp;sign          | 数据的签名（HEX 格式）                 | string            |                |
| &emsp;&emsp;data          | 数据（使用接收者的公钥加密，HEX 格式） | string            |                |
| &emsp;&emsp;sendTime      | 发送时间                               | string(date-time) |                |
| &emsp;&emsp;messageType   | 消息类型枚举,可用值:USER,GROUP         | string            |                |

**响应示例**:

```json
{
    "id": "",
    "command": "autoReceive",
    "code": 0,
    "message": "",
    "data": {
        "id": 0,
        "fromPublicKey": "",
        "toPublicKey": "",
        "fromUserId": 0,
        "toUserId": 0,s
        "sign": "",
        "data": "",
        "sendTime": "",
        "messageType": ""
    }
}
```