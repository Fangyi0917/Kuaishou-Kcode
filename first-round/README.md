# 初赛赛题
**在开始coding前请仔细阅读以下内容**

## 赛题背景
在做系统设计的时候往往将一个复杂系统切分为几个至几十个微服务，随着服务粒度的细化，微服务数量极速上涨。在快手，服务间调用量最高峰可以达到每秒千万次，每天高达数百亿次。为了对各个服务的监控状况进行监控和分析，需要收集服务间调用信息，这些调用信息包括：主调服务名、主调方IP、被调服务名、被调方IP、耗时、结果，调用发生时间等等。根据这些调用信息我们可以分析出服务的健康状况，例如：成功率、延迟情况、QPS等等。

## 题目内容
实现进程内对微服务调用信息的分析和查询系统，要求包含的功能如下：
- 接收和分析调用信息的接口
- 根据给定的条件进行查询和分析，描述如下：
  - 输入主调服务名、被调服务名和时间（分钟粒度），返回在这一分钟内主被调按ip聚合的成功率和P99；
  - 输入被调服务名和一个时间范围（分钟粒度, **==闭区间==**），返回在区间内被调的平均成功率


## 数据和输入输出说明：
输入数据格式：

```
每个调用记录存储在一行，数据直接以逗号分割(,), 换行符号(\n)

主调服务名,主调方IP,被调服务名,被调方IP,结果,耗时(ms), 调用发生的时间戳(ms)
commentService,172.17.60.2,userServie,172.17.60.3,true,89,1590975771020
commentService,172.17.60.3,userServie,172.17.60.4,false,70,1590975771025
commentService,172.17.60.2,userServie,172.17.60.3,true,103,1590975771030
commentService,172.17.60.3,userServie,172.17.60.4,true,79,1590975771031
commentService2,172.17.60.3,userServie,172.17.60.4,false,88,1590975771031
commentService2,172.17.60.3,userServie,172.17.60.4,true,91,1590975771032

```

成功率定义： 
```
单位时间内被调服务成功率 =  （单位时间内被调用结果为ture的次数）/ 单位时间内总被调用次数  【结果换算成百分比，小数点后保留两位数字,不足两位补0，其余位数直接舍弃】

以上示例数据计算一分钟的成功率
 - （2020-06-01 09:42）userServie在被调成功率: 4/6= 66.66%
 - （2020-06-01 09:42）userServie在172.17.60.4机器上成功率: 2 / 4 = 50.00%
 如无特殊说明，赛题中说的成功率以单位时间为一分钟计算
```
如果计算结果是0，请表示成 **.00%**

查询输出格式说明：

```
查询1（checkPair）：
输入：主调服务名、被调服务名和时间（分钟粒度）
输出：返回在这一分钟内主被调按ip聚合的成功率和P99, 无调用返回空list （不要求顺序）
输入示例：commentService，userServie，2020-06-01 09:42
输出示例：
172.17.60.3 172.17.60.4 50.00% 79
172.17.60.2 172.17.60.3 100.00% 103

查询2（checkResponder）：
输入： 被调服务名、开始时间和结束时间
输出：平均成功率，无调用结果返回 -1.00%
平均成功率 = （被调服务在区间内各分钟成功率总和）/ (存在调用的分钟数）【结果换算成百分比，小数点后保留两位数字,不足两位补0，其他位直接舍弃，不进位】

输入示例：userServie, 2020-06-01 09:42, 2020-06-01 09:44
输出示例：66.66% 
计算过程示例：
（2020-06-01 09:42）userServie被调成功率: 4/6= 66.66%
（2020-06-01 09:43）userServie无调用
（2020-06-01 09:44）userServie无调用
 那么userServie在2020-06-01 09:42到2020-06-01 09:44的平均成功率为（66.66%）/ 1 = 66.66%【示例数据仅仅在2020-06-01 09:42有调用】
```
如果计算结果是0，请表示成 **.00%**


## 操作说明
- 报名成功之后，参赛 git 信息会以短信形式发送给选手(已参过热身赛同学，直接跳到第二步)
- 登录 https://kcode-git.kuaishou.com/ 或直接下载 [初赛初始代码](https://kcode-git.kuaishou.com/kcode/KcodeRpcMonitor/-/archive/master/KcodeRpcMonitor-master.zip)
- 将初赛初始代码相关文件提交到自己的参赛项目，项目名称为 KcodeRpcMonitor_xxxxxxx，登录 https://kcode-git.kuaishou.com/ 可以获取
- pom.xml 中指定了打包插件(无需修改)
- KcodeRpcMonitorImpl 中需要选手实现两个 prepare() 、checkPair()和checkResponder方法
- KcodeRpcMonitorTest 用于选手本地测试，在参赛项目中无需提交
- ==相关的实现必须在 package com.kuaishou.kcode 下面，否则评测程序会发现不了你实现==

## 评测环境&启动参数
- JDK 版本： 1.8
- jvm内存设置 : -XX:+UseG1GC -XX:MaxGCPauseMillis=500  -Xss256k -Xms6G -Xmx6G -XX:MaxDirectMemorySize=1G
- 评测机器硬件信息（docker）：
    - 操作系统 CentOS 7.3 64位
    - CPU	16核 3.00GHz
    - 硬盘：容量 100GB， 吞吐量 > 100MB/S
- 如果需要输出文件，请使用 /tmp/ 目录，**禁止使用其他目录**
  
## 评测标准&排名
- 系统会默认拉取每个参赛队伍git项目的master代码作为评测程序执行
- 评测数据规模和提供的本地评测数据集规模一致
- 成绩评测过程
  - 接收和分析：读取评测文件中全部记录N1条，接受和分析过程耗时T1
  - 查询阶段（checkPair）：执行查询次数N2次，总耗时T2 (N2 > 10w)
  - 查询阶段（checkResponder）：执行查询次数N3次，总耗时T3 (N3 > 1w)
- 如果查询结果都正确，成绩为 N1/T1*系数1 + N2/T2 + N3/T3
- T1，T2，T3 均不能超过1200秒，否则无成绩


## 要求和限制
- 题目语言限定为 **==Java==** 
- 不允许引入任何外部依赖，JavaSE 8 包含的lib除外
- 排名靠前的代码会被review，如发现大量copy代码或引入不符合要求的三方库，将扣分或取消成绩
- 禁止在代码中输出任何日志输出或是控制台输出，否则无成绩
- 上传与题目无关的代码，视为作弊，无成绩

## 本地测试数据集

为方便下载，将数据集切分为 12文件(要全部下载，才能正常解压)：

- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z01
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z02
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z03
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z04
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z05
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z06
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z07
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z08
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z09
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z10
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.z11
- http://static.yximgs.com/kos/nlav10305/kcode1/kcodeRpcMonitor.zip

测试数据集对应的结果：

- http://static.yximgs.com/kos/nlav10305/kcode1/checkResponder.result
- http://static.yximgs.com/kos/nlav10305/kcode1/checkPair.result

测试程序实现可参考本项目的 test 目录中

## 代码提交
需要将自己完成的代码push到  https://kcode-git.kuaishou.com/kcode/KcodeRpcMonitor_xxxxxxx 项目下的master分支

## 评测问题说明

- **问题1、代码分支错误：**

  有部分队伍未将代码 push 到 master 分支，导致拉取不到代码，编译失败。


- **问题2、构造方法问题：**

  有部分队伍将代码中的 KcodeRpcMonitorImpl 类的构造方法设置成非 public 导致评测失败。

- **问3、代码逻辑问题：**

  有部分队伍的代码存在问题，例如：

  - 出现死循环，导致评测超时
  - 出现 NullPointerException 异常，导致测评失败

- **问题4、返回数据格式问题**：

  有部分队伍的返回结果中携带多余空格或是错误的逗号分隔符，导致测评失败，
  需要返回值收尾无空格