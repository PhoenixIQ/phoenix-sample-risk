package com.iquantex.phoenix.risk.domain.service;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * @author baozi
 * @date 2020/2/5 12:00 PM
 */
@Getter
@Builder
@ToString
public class RuleResp {

	private RuleResultCode ruleResultCode;

	private String RuleResultMessage;

	/**
	 * @author baozi
	 * @date 2020/2/5 12:01 PM
	 */
	public enum RuleResultCode {

		/** 不检查 */
		NO_CHECK,
		/** 通过 */
		PASS,
		/** 失败 */
		FAIL;

	}

}
