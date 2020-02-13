package com.iquantex.phoenix.risk.coreapi.fund;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/2/4 4:08 PM
 */
@Getter
@Builder
public class FundAssetsEvent implements Serializable {

	/** 产品编码 */
	private String fundCode;

	/** 净资产 */
	private double netAssets;

}
