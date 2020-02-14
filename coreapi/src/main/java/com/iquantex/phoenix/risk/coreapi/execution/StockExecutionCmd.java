package com.iquantex.phoenix.risk.coreapi.execution;

import lombok.Data;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 3:13 PM
 */
@Data
public class StockExecutionCmd implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 指令信息 */
	private StockExecutionInfo stockExecutionInfo;

}
