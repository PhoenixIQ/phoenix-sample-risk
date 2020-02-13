# 事中风控(4): 领域对象定义
## 前言
本篇是使用phoenix开发高性能事中风控服务系列第四篇，该系列一共分五篇文章介绍。本篇将根据第三篇提取的领域对象，补充完整`聚合根`、`实体`、`方法`等领域对象。

- 第一篇：背景和业务介绍
- 第二篇：phoenix工程搭建
- 第三篇：领域设计与消息定义
- 第四篇：领域对象定义
- 第五篇：客户端代码编写

上篇定义完了`命令`和`事件`，该篇主要定义下面领域对象

| 领域类型 | 领域对象 | 类名|
|------|----|----|
|聚合根|产品|FundAggregate|
|实体|持仓|Position|
|方法|计算风控|RuleService|

## 持仓实体 - Position
持仓实体管理了持仓的基本数据，和所有操作该数据的方法。定义如下：
```java
@Data
public class Position implements Serializable {

	/** 证券编码 */
	private String secuCode;

	/** 持仓数量(手) */
	private long qty;

	/** 持仓在途数量(手)(指令买入未成交) */
	private long transitQty;

	/**
	 * 增加持仓在途
	 * @param qty
	 */
	public void addTransitQty(long qty) {
		this.transitQty += qty;
	}

	/**
	 * 减少持仓在途
	 * @param qty
	 */
	public void subTransitQty(long qty) {
		long tmpQty = this.transitQty - qty;
		this.transitQty = tmpQty > 0 ? tmpQty : 0;
	}

	/**
	 * 增加持仓数量
	 * @param qty
	 */
	public void addQty(long qty) {
		this.qty += qty;
	}

	/**
	 * 减少持仓数量
	 * @param qty
	 */
	public void subQty(long qty) {
		long tmpQty = this.qty - qty;
		this.qty = tmpQty > 0 ? tmpQty : 0;
	}

	/**
	 * 计算持仓占比
	 * @param netAssets
	 * @return
	 */
	public double calcRatio(double netAssets) {
		return Precision.round((qty + transitQty) * 100 * MarketFacade.marketFacade.getQuote(secuCode) / netAssets, 4);
	}

}
```


## 聚合根实体 - FundAggregate

`产品聚合根`是整个产品的所有消息处理的统一入口，产品聚合根拥有`持仓实体`。根据领域事件风暴，`产品聚合根`的处理逻辑代码如下。

```java

@Data
@EntityAggregateAnnotation(aggregateRootType = "Risk")
public class FundAggregate implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 产品净资产 */
	private double netAssets;

	/** 产品持仓 */
	private Map<String/* 证券编码 */, Position> positions = new HashMap<>();

	/** 通过指令数量 */
	private int passInstNumber;

	/** 告警指令数量 */
	private int failInstNumber;

	/** 成交数量 */
	private int executionNumber;

	/**
	 * 处理产品命令
	 * @param cmd
	 * @return
	 */
	@AggregateRootIdAnnotation(aggregateRootId = "fundCode")
	public ActReturn act(FundAssetsCmd cmd) {
		return ActReturn
				.builder(RetCode.SUCCESS, "产品创建成功",
						FundAssetsEvent.builder().fundCode(cmd.getFundCode()).netAssets(cmd.getNetAssets()).build())
				.build();
	}

	/**
	 * 处理产品事件
	 * @param event
	 */
	public void on(FundAssetsEvent event) {
		this.fundCode = event.getFundCode();
		this.netAssets = event.getNetAssets();
	}

	/**
	 * 处理指令命令
	 * @param cmd
	 * @return
	 */
	@AggregateRootIdAnnotation(aggregateRootId = "fundCode")
	public ActReturn act(StockInstCmd cmd) {

		// 1. 检查风控
		StockInstInfo stockInstInfo = cmd.getStockInstInfo();
		Position position = positions.get(stockInstInfo.getSecuCode());
		if (position == null) {
			position = new Position();
			position.setSecuCode(stockInstInfo.getSecuCode());
			position.setQty(0);
			position.setTransitQty(0);
		}
		RuleReq context = RuleReq.builder().position(position).exps(RuleReq.STOCK_RULE).instAmt(stockInstInfo.getAmt())
				.fundNetAssets(netAssets).tradeType(stockInstInfo.getTradeTypeCode()).build();
		RuleResp result = RuleService.getRuleEngine().check(context);

		// 2. 构造风控结果
		if (result.getRuleResultCode() == RuleResp.RuleResultCode.FAIL) {
			return ActReturn.builder(RetCode.FAIL, "指令创建失败,因子详情" + result.getRuleResultMessage(),
					StockInstFailEvent.builder().fundCode(cmd.getFundCode()).stockInstInfo(cmd.getStockInstInfo())
							.riskResult(result.getRuleResultMessage()).build())
					.build();
		}
		else {
			return ActReturn.builder(RetCode.SUCCESS, "指令创建成功,因子详情" + result.getRuleResultMessage(),
					StockInstPassEvent.builder().fundCode(cmd.getFundCode()).stockInstInfo(cmd.getStockInstInfo())
							.riskResult(result.getRuleResultMessage()).build())
					.build();
		}
	}

	/**
	 * 处理指令通过事件
	 * @param event
	 * @return
	 */
	public void on(StockInstPassEvent event) {
		StockInstInfo stockInstInfo = event.getStockInstInfo();
		// 如果是买指令，增加在途
		if (stockInstInfo.getTradeTypeCode() == TradeType.BUY) {
			Position position = positions.get(stockInstInfo.getSecuCode());
			if (position == null) {
				position = new Position();
				position.setSecuCode(stockInstInfo.getSecuCode());
				positions.put(stockInstInfo.getSecuCode(), position);
			}
			position.addTransitQty(stockInstInfo.getQty());
		}
		passInstNumber++;
	}

	/**
	 * 处理指令失败事件
	 * @param event
	 * @return
	 */
	public void on(StockInstFailEvent event) {
	    failInstNumber++;
	}

	/**
	 * 处理成交命令
	 * @param cmd
	 * @return
	 */
	@AggregateRootIdAnnotation(aggregateRootId = "fundCode")
	public ActReturn act(StockExecutionCmd cmd) {
		return ActReturn.builder(RetCode.SUCCESS, "成交处理成功", StockExecutionEvent.builder().fundCode(cmd.getFundCode())
				.stockExecutionInfo(cmd.getStockExecutionInfo()).build()).build();
	}

	/**
	 * 处理成交事件
	 * @param event
	 */
	public void on(StockExecutionEvent event) {
		StockExecutionInfo stockExecutionInfo = event.getStockExecutionInfo();
		Position position = positions.get(stockExecutionInfo.getSecuCode());
		if (position == null) {
			position = new Position();
			position.setSecuCode(stockExecutionInfo.getSecuCode());
			positions.put(stockExecutionInfo.getSecuCode(), position);
		}
		// 买成交
		if (stockExecutionInfo.getTradeTypeCode() == TradeType.BUY) {
			position.subTransitQty(stockExecutionInfo.getQty());
			position.addQty(stockExecutionInfo.getQty());
		}
		// 卖成交
		else {
			position.subQty(stockExecutionInfo.getQty());
			if (position.getQty() <= 0 && position.getTransitQty() <= 0) {
				positions.remove(position.getSecuCode());
			}
		}
		executionNumber++;
	}
}

```

## 风控计算方法 - RuleService

回忆一下条款表达式：`（（在途数量 + 持仓数量） *   证券行情报价 + 指令金额 ） /  净资产  > 阈值    告警`

为了这里采用因子抽象，把每一个计算因子都抽象成方法方便复用。下面主要展示因子的代码，其他包装工具代码请查看`domain/service`包下内容。

```java
public class Rule {

	/**
	 * 交易方向
	 * @return
	 */
	public int tradeType() {
		context.addRuleResult("交易方向", context.getTradeType());
		return context.getTradeType().getType();
	}

	/**
	 * 获取持仓总量(在途 + 数量)
	 * @return
	 */
	public long positionAllQty() {
		Position position = context.getPosition();
		long positionAllQty = (position.getQty() + position.getTransitQty()) * 100;
		context.addRuleResult("持仓总量(股)", positionAllQty);
		return positionAllQty;
	}

	/**
	 * 证券行情报价
	 * @return
	 */
	public double quote() {
		Position position = context.getPosition();
		double quote = marketFacade.getQuote(position.getSecuCode());
		context.addRuleResult("行情最新价", quote);
		return quote;
	}

	/**
	 * 指令金额
	 * @return
	 */
	public double instAmt() {
		double instAmt = context.getInstAmt();
		context.addRuleResult("指令金额", instAmt);
		return instAmt;
	}

	/**
	 * 产品净资产
	 * @return
	 */
	public double netAssets() {
		double fundNetAssets = context.getFundNetAssets();
		context.addRuleResult("产品净资产", fundNetAssets);
		return fundNetAssets;
	}

	/**
	 * 计算净资产
	 * @return
	 */
	public double calcAssets(Object res) {
		if (null != res && res instanceof BigDecimal) {
			res = ((BigDecimal) res).doubleValue();
		}
		double calcAssets = (double) res;
		context.addRuleResult("计算净资产", calcAssets);
		return calcAssets;
	}

	/**
	 * 计算占比
	 * @return
	 */
	public double calcRatio(Object res) {
		if (null != res && res instanceof BigDecimal) {
			res = ((BigDecimal) res).doubleValue();
		}
		double calcRatio = (double) res;
		context.addRuleResult("投资占比", calcRatio);
		return calcRatio;
	}
}
```

## 单元测试 - FundAggregateTest
在phoenix中，`产品聚合根`是消息的统一处理，单元测试一般关注与`产品聚合根`的测试。根据前几张`事件风暴`场景可以很容易构建测试案例对`产品聚合根`进行测试，下面一个测试场景进行测试描述，更多的测试案例，可以查看源代码。
```java 

	/**
	 * 测试指令: 风控检查不通过
	 */
	@Test
	public void testAll() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();

		// 1. 初始化净资产
		FundAssetsCmd fundAssetsCmd = new FundAssetsCmd();
		fundAssetsCmd.setFundCode("F001");
		fundAssetsCmd.setNetAssets(10000000);
		fixture.when(fundAssetsCmd).expectRetSuccessCode().expectMessage(FundAssetsEvent.class);

		// 2. 第一笔指令过风控通过,增加在途
		StockInstInfo stockInstInfo = new StockInstInfo();
		stockInstInfo.setFundCode("F001");
		stockInstInfo.setInstCode("INST001");
		stockInstInfo.setQty(30);
		stockInstInfo.setAmt(3192000.0);
		stockInstInfo.setSecuCode("600519.SH");
		stockInstInfo.setTradeTypeCode(TradeType.BUY);
		StockInstCmd stockInstCmd = new StockInstCmd();
		stockInstCmd.setFundCode(stockInstInfo.getFundCode());
		stockInstCmd.setStockInstInfo(stockInstInfo);
		fixture.when(stockInstCmd).expectRetFailCode().expectMessage(StockInstFailEvent.class);
		fixture.printIdentify();
	}

```

## 结尾
本文完整的编写了领域对象中的聚合根、实体、风控计算等逻辑，遵循DDD的设计思想使用Phoenix面向内存对象建模非常容易。到此为止，事中风控的基本代码逻辑都编写完毕，通过单元测试可以按期望测试业务逻辑。下文将进行简单的客户端封装，开发简单前端页面，使整个服务的完整度和体验更高。
