# 事中风控(3): 消息流分析与定义
## 前言
本篇是使用phoenix开发高性能事中风控服务系列第三篇，该系列一共分四篇文章介绍。本篇在现有工程的基础上对业务做消息流转的分析，并定义好消息。

- 第一篇：背景和业务介绍
- 第二篇：phoenix工程搭建
- 第三篇：消息流分析与定义
- 第四篇：业务代码编写与测试
- 第五篇：前端业务代码编写


## 消息流分析
在第一篇背景和业务介绍中，我们明确了交易流程，为了方便我重新贴过来。

>- 股票买指令： 持仓数量不变，在途数量+，计算风控
>- 股票卖指令： 持仓数量不变
>- 股票买成交： 在途数量-，持仓数量+
>- 股票卖成交： 持仓数量-
>- 净资产变更： 净资产覆盖变更

根据以上交易流程，分析消息流转如下：

- 股票买/卖指令

```shell
                                                  +--------------------+
                                           +----->| StockInstPassEvent | 
                                           |      +--------------------+
 +------v-------+     *---------------*    |      风控检查通过事件(修改持仓)
 | StockInstCmd |---->| FundAggregate |----+       
 +------+-------+     *---------------*    |      
   股票指令               产品聚合根          |      +--------------------+
                                           +----->| StockInstFailEvent | 
                                                  +--------------------+
                                                   风控检查失败事件(不修改持仓)
```

- 股票买/卖成交
```shell
 +------v------------+     *---------------*      +---------------------+
 | StockExecutionCmd |---->| FundAggregate |----->| StockExecutionEvent |
 +------+------------+     *---------------*      +---------------------+
   股票成交                    产品聚合根             成交成功事件(修改持仓)
```

- 净资产变更
```shell
 +------v--------+     *---------------*      +-----------------+
 | FundAssetsCmd |---->| FundAggregate |----->| FundAssetsEvent |
 +------+--------+     *---------------*      +-----------------+
   产品净资产                    产品聚合根         产品净资产变更事件 
```

## 消息流定义
根据上述消息流分析，我们可以再`coreapi`定义`cmd`和`event`。phoenix支持protobuf和java序列化协议，这里选用java序列化。下面分别展示指令、成交、净资产下消息内容。

- 指令信息
``` java
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

- 成交信息

``` java
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

- 净资产信息
``` java
@Data
public class FundAssetsCmd implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 净资产 */
	private double netAssets;

}

```


## 结尾
本文分析了事中风控系统所接受的消息流，根据消息流的我们可以看出系统的处理能力边界。下篇讲讲述怎样使用phoenix开发响应消息的逻辑。
