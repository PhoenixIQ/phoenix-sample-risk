# 事中风控(3): 领域设计与消息定义
## 前言
本篇是使用phoenix开发高性能事中风控服务系列第三篇，该系列一共分四篇文章介绍。本篇将使用领域驱动设计方法提取领域对象，同时定义完领域消息。

- 第一篇：[背景和业务介绍](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-1)
- 第二篇：[phoenix工程搭建](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-2)
- 第三篇：[领域设计与消息定义](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-3)
- 第四篇：[领域对象定义](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-4)
- 第五篇：[客户端代码编写](https://gitlab.iquantex.com/phoenix-public/phoenix-risk/tree/part-5)


## 领域设计
事件风暴是DDD设计里面的指导方法，每个产品的设计思路不一样，事中风控设计场景不太复杂，下面对风控业务场景做简单的事件风暴。

### 事件风暴
在第一篇背景和业务介绍中，我们明确了交易流程，为了方便我重新贴过来。

>- 股票买指令： 持仓数量不变，在途数量+，计算产品风控
>- 股票卖指令： 持仓数量不变
>- 股票买成交： 在途数量-，持仓数量+
>- 股票卖成交： 持仓数量-
>- 净资产变更： 净资产覆盖变更

根据以上交易流程，进行事件风暴如下:

- 股票买/卖指令

```shell
                                                 +----------------+        
                                           +---->| 风控检查通过事件  |--+
                                           |     +----------------+  |    
 +------v-------+     +---------------+    |                         |       +-------------+
 | 股票买卖指令   |---->| 产品计算风控    |----+                         +-----> | 持仓修改状态  |
 +------+-------+     +---------------+    |                         |       +-------------+       
                                           |     +----------------+  |
                                           +---->| 风控检查失败事件  |--+
                                                 +----------------+
```

- 股票买/卖成交
```shell
 +------v---------+     +---------------+      +------------+      +------------+
 | 股票买卖成交     |---->| 产品处理成交    |----->| 成交事件     |----->| 持仓修改状态 |
 +------+---------+     +---------------+      +------------+      +------------+
```

- 净资产变更
```shell
 +------v--------+     +---------------+      +-----------------+     +--------------+
 | 产品净资产变更  |---> |产品处理净资产变更 |---->| 产品变更事件      |---->| 产品修改状态   |
 +------+--------+     +---------------+      +-----------------+     +--------------+
```

### 领域故事
领域故事分析可以对事件中的详细点进行补充，得到更为确切的业务逻辑，为提出领域对象做铺垫。
1. 本篇的事中风控领域是一个旁路风控，所谓旁路，意味着不影响主交易流程的进行，这和主路径上的风控处理的业务有一些差别之处。
2. 风控检查成功或失败都需要修改持仓状态。
3. 持仓所属于产品，大多数情况下不同的产品下的持仓是没关系的。

### 领域对象
事件风暴和领域故事分析后，我们可以对涉及到的领域属性做归类，提取领域对象，为后面代码编写做铺垫。

| 领域类型 | 领域对象 | 类名|
|------|----|----|
|聚合根|产品|FundAggregate|
|实体|持仓|Position|
|命令|股票买卖指令|StockInstCmd|
|事件|风控检查通过事件|StockInstPassEvent|
|事件|风控检查失败事件|StockInstFailEvent|
|命令|股票买卖成交|StockExecutionCmd|
|事件|股票买卖成交事件|StockExecutionEvent|
|命令|产品净资产变更|FundAssetsCmd|
|事件|产品净资产变更事件|FundAssetsEvent|
|方法|计算风控|RuleService|

## 领域消息定义
上面的领域设计我们提取出了领域对象，我们可以再`coreapi`模块中定义`命令`和`事件`。phoenix支持protobuf和java序列化协议，这里选用java序列化。下面分别展示指令、成交、净资产下消息内容。

### 指令消息
``` 
@Data
public class StockInstCmd implements Serializable {
	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockInstInfo stockInstInfo;
}
@Getter
@Builder
public class StockInstFailEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockInstInfo stockInstInfo;

	/** 风控检查结果 */
	private String riskResult;

}
@Getter
@Builder
public class StockInstPassEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockInstInfo stockInstInfo;

	/** 风控检查结果 */
	private String riskResult;

}

@Data
public class StockInstInfo implements Serializable {

	/** 指令编码 */
	private String instCode;

	/** 产品编码 */
	private String fundCode;

	/** 证券编码 */
	private String secuCode;

	/** 指令数量 */
	private long qty;

	/** 指令金额 */
	private double amt;

	/** 委托方向 */
	private TradeType tradeTypeCode;

}
 
```

### 成交消息

``` 
@Data
public class StockExecutionCmd implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockExecutionInfo stockExecutionInfo;

}
@Getter
@Builder
public class StockExecutionEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockExecutionInfo stockExecutionInfo;

}
@Data
public class StockExecutionInfo implements Serializable {

	/** 指令编码 */
	private String instCode;

	/** 成交编码 */
	private String executionCode;

	/** 产品编码 */
	private String fundCode;

	/** 证券编码 */
	private String secuCode;

	/** 成交数量 */
	private long qty;

	/** 成交价格 */
	private long price;

	/** 委托方向 */
	private TradeType tradeTypeCode;

}

```

### 产品净资产消息
``` 
@Data
public class FundAssetsCmd implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 净资产 */
	private double netAssets;

}
@Getter
@Builder
public class FundAssetsEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 净资产 */
	private double netAssets;

}


```

## 结尾
本文分析了通过领域驱动设计的方式提取出了领域对象，同时定义了领域的消息。下篇讲述怎样使用phoenix开发聚合根和实体。
