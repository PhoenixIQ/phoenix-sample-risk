package com.iquantex.phoenix.risk.coreapi.execution;

import com.iquantex.phoenix.risk.coreapi.constant.TradeType;
import lombok.Data;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 3:13 PM
 */
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
