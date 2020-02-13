package com.iquantex.phoenix.risk.domain;

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
import com.iquantex.phoenix.risk.domain.entity.FundAggregate;
import com.iquantex.phoenix.risk.domain.entity.Position;
import com.iquantex.phoenix.risk.domain.facade.MarketFacade;
import com.iquantex.phoenix.server.test.util.EntityAggregateFixture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author baozi
 * @date 2020/2/4 7:35 PM
 */
@Slf4j
public class FundAggregateTest {

	@Before
	public void before() {
		MarketFacade.marketFacade = new MarketFacade();
		MarketFacade.marketFacade.init();
	}

	/**
	 * 测试产品变更
	 */
	@Test
	public void testFundChange() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();
		FundAssetsCmd fundAssetsCmd = new FundAssetsCmd();
		fundAssetsCmd.setFundCode("F001");
		fundAssetsCmd.setNetAssets(10000000);

		// 断言处理流程
		fixture.when(fundAssetsCmd).expectRetSuccessCode().expectMessage(FundAssetsEvent.class);

		// 断言聚合根内容
		FundAggregate fundAggregate = fixture.getAggregateRoot(FundAggregate.class, "F001");
		Assert.assertTrue(fundAggregate.getNetAssets() == 10000000);

	}

	/**
	 * 测试买指令:没有净资产返回风控不通过
	 */
	@Test
	public void testInstCmdBuyFail() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();

		StockInstInfo stockInstInfo = new StockInstInfo();
		stockInstInfo.setFundCode("F001");
		stockInstInfo.setInstCode("INST001");
		stockInstInfo.setQty(10);
		stockInstInfo.setAmt(1064000.0);
		stockInstInfo.setSecuCode("600519.SH");
		stockInstInfo.setTradeTypeCode(TradeType.BUY);

		StockInstCmd cmd = new StockInstCmd();
		cmd.setFundCode(stockInstInfo.getFundCode());
		cmd.setStockInstInfo(stockInstInfo);

		fixture.when(cmd).expectRetFailCode().expectMessage(StockInstFailEvent.class);

	}

	/**
	 * 测试买指令: 风控检查通过
	 */
	@Test
	public void testInstCmdBuyPass() {

		// 1. 初始化净资产
		EntityAggregateFixture fixture = new EntityAggregateFixture();
		FundAssetsCmd fundAssetsCmd = new FundAssetsCmd();
		fundAssetsCmd.setFundCode("F001");
		fundAssetsCmd.setNetAssets(10000000);
		fixture.when(fundAssetsCmd).expectRetSuccessCode().expectMessage(FundAssetsEvent.class);

		// 2. 过风控
		StockInstInfo stockInstInfo = new StockInstInfo();
		stockInstInfo.setFundCode("F001");
		stockInstInfo.setInstCode("INST001");
		stockInstInfo.setQty(10);
		stockInstInfo.setAmt(1064000.0);
		stockInstInfo.setSecuCode("600519.SH");
		stockInstInfo.setTradeTypeCode(TradeType.BUY);
		StockInstCmd cmd = new StockInstCmd();
		cmd.setFundCode(stockInstInfo.getFundCode());
		cmd.setStockInstInfo(stockInstInfo);

		fixture.when(cmd).expectRetSuccessCode().expectMessage(StockInstPassEvent.class);

	}

	/**
	 * 测试指令: 风控检查不通过
	 */
	@Test
	public void testAll() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();

		// 1. 初始化净资产
		FundAssetsCmd fundAssetsCmd = new FundAssetsCmd();
		fundAssetsCmd.setFundCode("F001");
		fundAssetsCmd.setNetAssets(10000000);
		fixture.when(fundAssetsCmd).expectRetSuccessCode().expectMessage(FundAssetsEvent.class);

		// 2. 第一笔指令过风控通过,增加在途
		StockInstInfo stockInstInfo = new StockInstInfo();
		stockInstInfo.setFundCode("F001");
		stockInstInfo.setInstCode("INST001");
		stockInstInfo.setQty(30);
		stockInstInfo.setAmt(3192000.0);
		stockInstInfo.setSecuCode("600519.SH");
		stockInstInfo.setTradeTypeCode(TradeType.BUY);
		StockInstCmd stockInstCmd = new StockInstCmd();
		stockInstCmd.setFundCode(stockInstInfo.getFundCode());
		stockInstCmd.setStockInstInfo(stockInstInfo);
		fixture.when(stockInstCmd).expectRetFailCode().expectMessage(StockInstFailEvent.class);
		fixture.printIdentify();
	}

	/**
	 * 测试卖指令:不检查风控返回正常
	 */
	@Test
	public void testInstCmdSellSuccess() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();

		StockInstInfo stockInstInfo = new StockInstInfo();
		stockInstInfo.setFundCode("F001");
		stockInstInfo.setInstCode("INST001");
		stockInstInfo.setQty(10);
		stockInstInfo.setAmt(1064000.0);
		stockInstInfo.setSecuCode("600519.SH");
		stockInstInfo.setTradeTypeCode(TradeType.SELL);

		StockInstCmd cmd = new StockInstCmd();
		cmd.setFundCode(stockInstInfo.getFundCode());
		cmd.setStockInstInfo(stockInstInfo);

		fixture.when(cmd).expectRetSuccessCode().expectMessage(StockInstPassEvent.class);
	}

	/**
	 * 测试买成交
	 */
	@Test
	public void testExecutionCmd() {

		EntityAggregateFixture fixture = new EntityAggregateFixture();

		StockExecutionInfo stockExecutionInfo = new StockExecutionInfo();
		stockExecutionInfo.setFundCode("F001");
		stockExecutionInfo.setExecutionCode("EX001");
		stockExecutionInfo.setInstCode("INST001");
		stockExecutionInfo.setQty(10);
		stockExecutionInfo.setPrice(1064);
		stockExecutionInfo.setSecuCode("600519.SH");
		stockExecutionInfo.setTradeTypeCode(TradeType.BUY);

		StockExecutionCmd cmd = new StockExecutionCmd();
		cmd.setFundCode(stockExecutionInfo.getFundCode());
		cmd.setStockExecutionInfo(stockExecutionInfo);

		fixture.when(cmd).expectRetSuccessCode().expectMessage(StockExecutionEvent.class);

		// 断言聚合根内容
		FundAggregate fundAggregate = fixture.getAggregateRoot(FundAggregate.class, "F001");
		Assert.assertTrue(fundAggregate.getPositions().size() == 1);
		Position position = fundAggregate.getPositions().get("600519.SH");
		Assert.assertTrue(position.getQty() == 10);
		Assert.assertTrue(position.getTransitQty() == 0);
	}

}
