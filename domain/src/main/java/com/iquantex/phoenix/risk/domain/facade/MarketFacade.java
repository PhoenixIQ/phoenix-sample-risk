package com.iquantex.phoenix.risk.domain.facade;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author baozi
 * @date 2020/2/5 5:29 PM
 */
@Component
public class MarketFacade {

	public static MarketFacade marketFacade;

	/** 模拟行情 */
	public Map<String /* 股票编码 */, Double/* 最新报价 */> quotes;

	@PostConstruct
	public void init() {
		marketFacade = this;
		quotes = new HashMap<>();

		quotes.put("000001.SZ", 14.56);
		quotes.put("600519.SH", 1067.93);
		quotes.put("000673.SZ", 4.16);
		quotes.put("000423.SZ", 32.36);
		quotes.put("002294.SZ", 18.75);

	}

	/**
	 * 获取行情服务
	 * @param secuCode
	 * @return
	 */
	public double getQuote(String secuCode) {
		return quotes.getOrDefault(secuCode, 0.0);
	}

}