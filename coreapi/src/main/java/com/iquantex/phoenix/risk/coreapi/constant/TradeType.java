package com.iquantex.phoenix.risk.coreapi.constant;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 4:53 PM
 */
public enum TradeType implements Serializable {

	/**
	 * 买
	 */
	BUY(0),
	/**
	 * 卖
	 */
	SELL(1);

	private int type;

	TradeType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

}
