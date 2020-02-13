package com.iquantex.phoenix.risk.coreapi.execution;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 3:13 PM
 */
@Getter
@Builder
public class StockExecutionEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockExecutionInfo stockExecutionInfo;

}
