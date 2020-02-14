# 事中风控(4): 领域对象定义
## 前言
本篇是使用phoenix开发高性能事中风控服务系列第五篇，该系列一共分五篇文章介绍。前面几篇已经完成了事中风控的服务端开发，本篇将增加客户端调用和前端页面使完整程度更高。为了方便，将客户端也集成入该服务当中。

- 第一篇：背景和业务介绍
- 第二篇：phoenix工程搭建
- 第三篇：领域设计与消息定义
- 第四篇：领域对象定义
- 第五篇：客户端代码编写


## 增加路由
Phoenix是消息驱动的微服务框架，为了达到服务间通信，需要依赖一份路由表，现在还不支持注册中心，需要手动在配置文件中配置路由表信息。

```yml
quantex:
  phoenix:
    routers:
      - message: com.iquantex.phoenix.risk.api.execution.StockExecutionCmd
        dst: phoenix-risk/EA/Risk
      - message: com.iquantex.phoenix.risk.api.inst.StockInstCmd
        dst: phoenix-risk/EA/Risk
      - message: com.iquantex.phoenix.risk.api.fund.FundAssetsCmd
        dst: phoenix-risk/EA/Risk
```


## 客户端编写

Phoenix是消息驱动框架，一切都是消息通信。为了与前端交互方便，可以再`application`模块中增加发送消息的`Controller`。`Controller`可以接受页面请求，转换为`命令`发送给phoenix服务端。

示例代码
```java
@Slf4j
@RestController
@RequestMapping("/funds")
public class TradeController {

	@Autowired
	private PhoenixClient client;

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
}

```


## 运行启动
> 运行启动前，还需要增加一些简单的`html`方便查看效果，请看源代码中`resources/static`。

1. 执行脚本: `sh tools/build-restart`
2. 打开浏览器: `http://127.0.0.1:8080`
3. 查看效果，左边可以下指令、成交，右边表格可以查看实时状态和风控计算结果
![](./doc/image/01.png)




## 结尾
本文以事中风控的客户端为例，展示了Phoenix的客户端编写方式。到此位置，整个使用pheonix开发事中风控微服务全部结束。可以看出来Phoenix作为微服务框架加上DDD的设计思想可以很好的拆分业务，再通过面向内存的编程模型很容易落地实现。
