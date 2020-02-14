package com.iquantex.phoenix.risk.domain.entity;

import com.iquantex.phoenix.risk.domain.facade.MarketFacade;
import lombok.Data;
import org.apache.commons.math3.util.Precision;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 2:51 PM 持仓
 */
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
