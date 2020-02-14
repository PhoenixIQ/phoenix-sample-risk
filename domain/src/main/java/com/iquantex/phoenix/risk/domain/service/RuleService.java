package com.iquantex.phoenix.risk.domain.service;

import com.iquantex.phoenix.risk.domain.entity.Position;
import com.iquantex.phoenix.risk.domain.facade.MarketFacade;
import com.ql.util.express.ExpressRunner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

@Slf4j
public class RuleService {

	/** 规则解析引擎 */
	private ExpressRunner runner = new ExpressRunner(true, false);

	/** 入参上下文 */
	private RuleReq context;

	/** 行情服务 */
	private MarketFacade marketFacade = MarketFacade.marketFacade;

	private static class SingletonHolder {

		private static RuleService instance = new RuleService();

	}

	/**
	 * 获取单例对象
	 * @return
	 */
	public static RuleService getRuleEngine() {
		return SingletonHolder.instance;
	}

	/**
	 * 私有构造方法
	 */
	private RuleService() {
		Rule rule = new Rule();
		try {
			Method[] methods = rule.getClass().getDeclaredMethods();
			for (Method m : methods) {
				// 非public方法跳过
				if (!Modifier.isPublic(m.getModifiers())) {
					continue;
				}
				String name = m.getName();
				Class<?>[] paramCls = m.getParameterTypes();
				runner.addFunctionOfServiceMethod(name, rule, name, paramCls, null);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("引擎初始化失败", e);
		}
		log.info("RuleEngine Initialization Successfull!!!");
	}

	/**
	 * 规则检查
	 * @return
	 */
	public RuleResp check(RuleReq context) {
		this.context = context;
		try {
			int result = (int) runner.execute(context.getExps(), null, null, true, false);
			switch (result) {
			case -1:
				return RuleResp.builder().ruleResultCode(RuleResp.RuleResultCode.NO_CHECK)
						.RuleResultMessage(context.getRuleResult()).build();
			case 0:
				return RuleResp.builder().ruleResultCode(RuleResp.RuleResultCode.FAIL)
						.RuleResultMessage(context.getRuleResult()).build();
			default:
				return RuleResp.builder().ruleResultCode(RuleResp.RuleResultCode.PASS)
						.RuleResultMessage(context.getRuleResult()).build();
			}
		}
		catch (InvocationTargetException e) {
			log.error(e.getMessage(), e.getTargetException());
			return RuleResp.builder().ruleResultCode(RuleResp.RuleResultCode.FAIL).RuleResultMessage("计算异常").build();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return RuleResp.builder().ruleResultCode(RuleResp.RuleResultCode.FAIL).RuleResultMessage("计算异常").build();
		}
	}

	@Data
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

}
