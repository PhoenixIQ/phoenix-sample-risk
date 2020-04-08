package com.iquantex.phoenix.risk.domain.entity;

import com.iquantex.phoenix.risk.coreapi.constant.TradeType;
import com.iquantex.phoenix.risk.coreapi.execution.StockExecutionCmd;
import com.iquantex.phoenix.risk.coreapi.execution.StockExecutionEvent;
import com.iquantex.phoenix.risk.coreapi.execution.StockExecutionInfo;
import com.iquantex.phoenix.risk.coreapi.fund.FundAssetsCmd;
import com.iquantex.phoenix.risk.coreapi.fund.FundAssetsEvent;
import com.iquantex.phoenix.risk.coreapi.inst.StockInstCmd;
import com.iquantex.phoenix.risk.coreapi.inst.StockInstFailEvent;
import com.iquantex.phoenix.risk.coreapi.inst.StockInstInfo;
import com.iquantex.phoenix.risk.coreapi.inst.StockInstPassEvent;
import com.iquantex.phoenix.risk.domain.service.RuleReq;
import com.iquantex.phoenix.risk.domain.service.RuleResp;
import com.iquantex.phoenix.risk.domain.service.RuleService;
import com.iquantex.phoenix.server.aggregate.entity.CommandHandler;
import com.iquantex.phoenix.server.aggregate.entity.EntityAggregateAnnotation;
import com.iquantex.phoenix.server.aggregate.model.ActReturn;
import com.iquantex.phoenix.server.aggregate.model.RetCode;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author baozi
 * @date 2020/2/4 2:51 PM 基金产品聚合根
 */
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
	@CommandHandler(aggregateRootId = "fundCode")
	public ActReturn act(FundAssetsCmd cmd) {
		return ActReturn.builder().retCode(RetCode.SUCCESS).retMessage("产品创建成功")
				.event(FundAssetsEvent.builder().fundCode(cmd.getFundCode()).netAssets(cmd.getNetAssets()).build())
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
	@CommandHandler(aggregateRootId = "fundCode")
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
			return ActReturn.builder().retCode(RetCode.FAIL).retMessage("指令创建失败,因子详情" + result.getRuleResultMessage())
					.event(StockInstFailEvent.builder().fundCode(cmd.getFundCode())
							.stockInstInfo(cmd.getStockInstInfo()).riskResult(result.getRuleResultMessage()).build())
					.build();
		}
		else {
			return ActReturn.builder().retCode(RetCode.SUCCESS)
					.retMessage("指令创建成功,因子详情" + result.getRuleResultMessage())
					.event(StockInstPassEvent.builder().fundCode(cmd.getFundCode())
							.stockInstInfo(cmd.getStockInstInfo()).riskResult(result.getRuleResultMessage()).build())
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
		failInstNumber++;
	}

	/**
	 * 处理成交命令
	 * @param cmd
	 * @return
	 */
	@CommandHandler(aggregateRootId = "fundCode")
	public ActReturn act(StockExecutionCmd cmd) {
		return ActReturn.builder().retCode(RetCode.SUCCESS).retMessage("成交处理成功").event(StockExecutionEvent.builder()
				.fundCode(cmd.getFundCode()).stockExecutionInfo(cmd.getStockExecutionInfo()).build()).build();
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
