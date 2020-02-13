package com.iquantex.phoenix.risk.coreapi.inst;

import com.iquantex.phoenix.risk.coreapi.constant.TradeType;
import lombok.Data;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 4:31 PM
 */
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
