package com.iquantex.phoenix.risk.coreapi.inst;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 3:13 PM
 */
@Getter
@Builder
public class StockInstFailEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockInstInfo stockInstInfo;

	/** 风控检查结果 */
	private String riskResult;

}
