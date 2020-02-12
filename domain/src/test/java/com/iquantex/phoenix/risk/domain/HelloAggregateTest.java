package com.iquantex.phoenix.risk.domain;

import com.iquantex.phoenix.risk.coreapi.Hello;
import com.iquantex.phoenix.risk.domain.entity.HelloAggregate;
import com.iquantex.phoenix.server.test.util.EntityAggregateFixture;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author baozi
 * @date 2020/02/10 3:44 下午
 * @Description 账户聚合根单元测试
 */
public class HelloAggregateTest {

	@Test
	public void testHello() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();
		Hello.HelloCmd helloCmd = Hello.HelloCmd.newBuilder().setHelloId("H001").build();

		// 断言
		fixture.when(helloCmd).expectRetSuccessCode().expectMessage(Hello.HelloEvent.class);
		HelloAggregate helloAggregate = fixture.getAggregateRoot(HelloAggregate.class, "H001");
		Assert.assertTrue(helloAggregate.getNum() == 1);

	}

}
