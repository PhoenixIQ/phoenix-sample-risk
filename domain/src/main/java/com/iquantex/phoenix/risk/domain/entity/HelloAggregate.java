package com.iquantex.phoenix.risk.domain.entity;

import com.iquantex.phoenix.risk.coreapi.Hello;
import com.iquantex.phoenix.server.aggregate.entity.CommandHandler;
import com.iquantex.phoenix.server.aggregate.entity.EntityAggregateAnnotation;
import com.iquantex.phoenix.server.aggregate.model.ActReturn;
import com.iquantex.phoenix.server.aggregate.model.RetCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author baozi
 * @date 2020/02/10 3:44 下午
 * @Description 账户聚合根
 */
@Slf4j
@Data
@EntityAggregateAnnotation(aggregateRootType = "Hello")
public class HelloAggregate implements Serializable {

	/** 状态: 计数器 */
	private long num;

	/**
	 * hello cmd
	 * @param cmd
	 * @return
	 */
	@CommandHandler(aggregateRootId = "helloId")
	public ActReturn act(Hello.HelloCmd cmd) {
		log.info("Hello World Phoenix...");
		return ActReturn.builder().retCode(RetCode.SUCCESS).retMessage("success")
				.event(Hello.HelloEvent.newBuilder().setHelloId(cmd.getHelloId()).build()).build();
	}

	/**
	 * hello event
	 * @param event
	 */
	public void on(Hello.HelloEvent event) {
		num++;
		log.info("Phoenix State: {}", num);
	}

}
