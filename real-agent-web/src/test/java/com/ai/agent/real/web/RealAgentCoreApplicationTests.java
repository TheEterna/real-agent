package com.ai.agent.real.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.*;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

class RealAgentCoreApplicationTests {

	@Test
	void simpleRxJavaPublisherConsumerTest() throws InterruptedException {
		// 创建发布者（发布字符串数据）
		SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

		// 创建并注册订阅者
		Flow.Subscriber<String> subscriber = new Flow.Subscriber<>() {
			private Flow.Subscription subscription;

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				System.out.println("订阅建立，获取控制对象");
				this.subscription = subscription;
				// 向发布者请求1条数据
				subscription.request(1);
			}

			@Override
			public void onNext(String item) {
				System.out.println("接收到数据：" + item);
				// 处理完当前数据后，再请求1条新数据
				subscription.request(1);
			}

			@Override
			public void onError(Throwable throwable) {
				System.err.println("发生错误：" + throwable.getMessage());
				// 取消订阅
				subscription.cancel();
			}

			@Override
			public void onComplete() {
				System.out.println("数据发送完成");
			}
		};

		// 订阅关系建立
		publisher.subscribe(subscriber);

		// 发布数据
		publisher.submit("第一条数据");
		publisher.submit("第二条数据");
		publisher.submit("第三条数据");
		publisher.closeExceptionally(new RuntimeException("测试异常"));

		// 关闭发布者
		publisher.close();

		// 等待异步处理完成
		Thread.sleep(100000);
	}

	@Test
	void flexTest() {
		// 创建一个简单的Flux示例
		Flux<String> flux = Flux.just("Hello", "World", "Reactor").map(String::toUpperCase).filter(s -> s.length() > 3);
		// 订阅并打印结果
		flux.subscribe(data -> System.out.println("接收到数据：" + data),
				error -> System.err.println("发生错误：" + error.getMessage()), () -> System.out.println("数据流处理完成"));

		// 阻塞主线程以确保异步处理完成
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
