package com.iquantex.phoenix.risk.controller;

import com.iquantex.phoenix.client.PhoenixClient;
import com.iquantex.phoenix.client.RpcResult;
import com.iquantex.phoenix.risk.coreapi.execution.StockExecutionCmd;
import com.iquantex.phoenix.risk.coreapi.execution.StockExecutionInfo;
import com.iquantex.phoenix.risk.coreapi.fund.FundAssetsCmd;
import com.iquantex.phoenix.risk.coreapi.inst.StockInstCmd;
import com.iquantex.phoenix.risk.coreapi.inst.StockInstInfo;
import com.iquantex.phoenix.risk.domain.entity.FundAggregate;
import com.iquantex.phoenix.server.eventsourcing.AggregateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author baozi
 * @date 2020/2/4 6:17 PM
 */
@Slf4j
@RestController
@RequestMapping("/funds")
public class TradeController {

	private static DecimalFormat df = new DecimalFormat("0.000");

	@Autowired
	private PhoenixClient client;

	/**
	 * 模拟产品净资产
	 * @return
	 */
	@GetMapping("")
	public String infos() {
		int pageIndex = 1;
		int pageSize = 1000;
		List<FundAggregate> aggList = new ArrayList<>();
		while (true) {
			List<String> tmpList = AggregateRepository.getInstance().getAggregateIdListByAggregateRootType("Risk",
					pageIndex, pageSize);
			log.info("select all aggregate:{}", tmpList);
			if (tmpList.isEmpty()) {
				break;
			}
			for (String aggId : tmpList) {
				FundAggregate aggregate = (FundAggregate) AggregateRepository.getInstance().load(aggId)
						.getAggregateRoot();
				aggList.add(aggregate);
			}
			pageIndex++;
		}

		StringBuffer sb = new StringBuffer();

		int instAllNumber = 0;
		int passInstNumber = 0;
		int failInstNumber = 0;
		int executionNumber = 0;
		for (FundAggregate aggregate : aggList) {
			instAllNumber += (aggregate.getFailInstNumber() + aggregate.getPassInstNumber());
			passInstNumber += aggregate.getPassInstNumber();
			failInstNumber += aggregate.getFailInstNumber();
			executionNumber += aggregate.getExecutionNumber();
		}

		sb.append("<br>");
		sb.append("<br>");
		sb.append("<br>");
		sb.append("<table border=1 width='1000px'>");
		sb.append("<tr><th>产品编码</th><th>净资产</th><th>证券编码</th><th>持仓数量</th><th>在途数量</th><th>持仓占比</th><th>阈值</th></tr>");
		for (FundAggregate aggregate : aggList) {
			if (aggregate.getPositions().isEmpty()) {
				sb.append("<tr>");
				sb.append("<td>" + aggregate.getFundCode() + "</td>");
				sb.append("<td>" + df.format(aggregate.getNetAssets()) + "</td>");
				sb.append("<td>" + "NONE" + "</td>");
				sb.append("<td>" + 0 + "</td>");
				sb.append("<td>" + 0 + "</td>");
				sb.append("<td>" + 0 + "</td>");
				sb.append("<td>0.3</td>");
				sb.append("</tr>");

			}
			aggregate.getPositions().values().forEach(position -> {
				sb.append("<tr>");
				sb.append("<td>" + aggregate.getFundCode() + "</td>");
				sb.append("<td>" + df.format(aggregate.getNetAssets()) + "</td>");
				sb.append("<td>" + position.getSecuCode() + "</td>");
				sb.append("<td>" + position.getQty() + "</td>");
				sb.append("<td>" + position.getTransitQty() + "</td>");
				double calcRatio = position.calcRatio(aggregate.getNetAssets());
				if (calcRatio > 0.3) {
					sb.append("<td><font color=\"red\">" + df.format(calcRatio) + "</font></td>");
				}
				else {
					sb.append("<td>" + df.format(calcRatio) + "</td>");
				}
				sb.append("<td>0.3</td>");
				sb.append("</tr>");
			});
		}
		sb.append("<tr>");
		sb.append("<td>指令总数:" + instAllNumber + "</td>");
		sb.append("<td>通过总数:" + passInstNumber + "</td>");
		sb.append("<td>告警总数:" + failInstNumber + "</td>");
		sb.append("<td>成交总数:" + executionNumber + "</td>");
		sb.append("<td></td>");
		sb.append("<td></td>");
		sb.append("<td></td>");
		sb.append("</tr>");
		sb.append("</table><br><br><br>");
		return sb.toString();
	}

	/**
	 * 模拟指令
	 * @return
	 */
	@PostMapping("/inst")
	public String inst(@RequestBody StockInstInfo stockInstInfo) {

		StockInstCmd cmd = new StockInstCmd();
		cmd.setFundCode(stockInstInfo.getFundCode());
		cmd.setStockInstInfo(stockInstInfo);

		Future<RpcResult> future = client.send(cmd, "");
		try {
			RpcResult result = future.get(10, TimeUnit.SECONDS);
			return result.getMessage();
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
			return "rpc error: " + e.getMessage();
		}
	}

	/**
	 * 模拟成交
	 * @return
	 */
	@PostMapping("/execution")
	public String execution(@RequestBody StockExecutionInfo stockExecutionInfo) {

		StockExecutionCmd cmd = new StockExecutionCmd();
		cmd.setFundCode(stockExecutionInfo.getFundCode());
		cmd.setStockExecutionInfo(stockExecutionInfo);

		Future<RpcResult> future = client.send(cmd, "");
		try {
			RpcResult result = future.get(10, TimeUnit.SECONDS);
			return result.getMessage();
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
			return "rpc error: " + e.getMessage();
		}
	}

	/**
	 * 模拟产品净资产
	 * @return
	 */
	@PostMapping("/fund_assets")
	public String execution(@RequestBody FundAssetsCmd cmd) {

		Future<RpcResult> future = client.send(cmd, "");
		try {
			RpcResult result = future.get(10, TimeUnit.SECONDS);
			return result.getMessage();
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
			return "rpc error: " + e.getMessage();
		}
	}

}
