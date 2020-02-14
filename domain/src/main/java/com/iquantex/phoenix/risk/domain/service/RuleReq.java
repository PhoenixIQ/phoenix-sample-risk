package com.iquantex.phoenix.risk.domain.service;

import com.iquantex.phoenix.risk.coreapi.constant.TradeType;
import com.iquantex.phoenix.risk.domain.entity.Position;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author baozi
 * @date 2020/2/5 10:38 AM
 */
@Getter
@Builder
public class RuleReq {

	// （（在途数量 + 持仓数量） * 证券行情报价 + 指令金额 ） / 净资产 > 阈值
	public static final String STOCK_RULE = "tradeType() == 1 ? (-1) :  (calcRatio(calcAssets(positionAllQty() * quote() + instAmt()) / netAssets()) > 0.3 ? 0 : 1)";

	/** 表达式 */
	private String exps;

	/** 指令金额 */
	private double instAmt;

	/** 交易类型 */
	private TradeType tradeType;

	/** 产品净资产 */
	private double fundNetAssets;

	/** 产品持仓信息 */
	private Position position;

	/** 因子结果 */
	private Map<String/* 因子名 */, String/* 因子值 */> ruleResult;

	/**
	 * 增加因子结果
	 * @param key
	 * @param value
	 */
	public void addRuleResult(String key, Object value) {
		if (ruleResult == null) {
			ruleResult = new HashMap<>();
		}
		ruleResult.put(key, String.valueOf(value));
	}

	/**
	 * 获取因子检查结果
	 * @return
	 */
	public String getRuleResult() {
		return ruleResult.toString();
	}

}
