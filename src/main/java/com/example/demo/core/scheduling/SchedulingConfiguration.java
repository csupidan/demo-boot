package com.example.demo.core.scheduling;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.concurrent.ListenableFuture;

import com.example.demo.core.util.CallableWithRequestId;
import com.example.demo.core.util.RunnableWithRequestId;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(0)
@EnableScheduling
@EnableAsync(order = -999, proxyTargetClass = true)
@Configuration
public class SchedulingConfiguration implements SchedulingConfigurer, AsyncConfigurer {

	@Value("${taskScheduler.poolSize:5}")
	private int taskSchedulerPoolSize = 5;

	@Value("${taskExecutor.corePoolSize:50}")
	private int taskExecutorCorePoolSize = 50;

	@Value("${taskExecutor.maxPoolSize:100}")
	private int taskExecutorMaxPoolSize = 100;

	@Value("${taskExecutor.queueCapacity:10000}")
	private int taskExecutorQueueCapacity = 10000;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());
	}

	@Override
	public Executor getAsyncExecutor() {
		return taskExecutor();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new WrappedThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(taskSchedulerPoolSize);
		threadPoolTaskScheduler.setThreadNamePrefix("taskScheduler-");
		threadPoolTaskScheduler.setRemoveOnCancelPolicy(true);
		threadPoolTaskScheduler.setErrorHandler(ex -> {
			String className = ex.getClass().getName();
			if (className.equals("io.github.resilience4j.bulkhead.BulkheadFullException")
					|| className.equals("io.github.resilience4j.ratelimiter.RequestNotPermitted"))
				log.warn("Error occurred in scheduled task: {}", ex.getLocalizedMessage());
			else
				log.error("Unexpected error occurred in scheduled task", ex);
		});
		return threadPoolTaskScheduler;
	}

	@Bean
	public AsyncTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new WrappedThreadPoolTaskExecutor();
		executor.setMaxPoolSize(taskExecutorMaxPoolSize);
		executor.setCorePoolSize(taskExecutorCorePoolSize);
		executor.setQueueCapacity(taskExecutorQueueCapacity);
		executor.setThreadNamePrefix("taskExecutor-");
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, args) -> {
			log.error("Unexpected error occurred when call method ( " + method.toString() + " ) asynchronously", ex);
		};
	}

	static class WrappedThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
		private static final long serialVersionUID = 1L;

		@Override
		public void execute(Runnable task) {
			super.execute(new RunnableWithRequestId(task));
		}
	}

	static class WrappedThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {
		private static final long serialVersionUID = 1L;

		@Override
		public void execute(Runnable task) {
			super.execute(new RunnableWithRequestId(task));
		}

		@Override
		public Future<?> submit(Runnable task) {
			return super.submit(new RunnableWithRequestId(task));
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return super.submit(new CallableWithRequestId<>(task));
		}

		@Override
		public ListenableFuture<?> submitListenable(Runnable task) {
			return super.submitListenable(new RunnableWithRequestId(task));
		}

		@Override
		public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
			return super.submitListenable(new CallableWithRequestId<>(task));
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
			return super.schedule(new RunnableWithRequestId(task), trigger);
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
			return super.schedule(new RunnableWithRequestId(task), startTime);
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
			return super.scheduleAtFixedRate(new RunnableWithRequestId(task), startTime, period);
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
			return super.scheduleAtFixedRate(new RunnableWithRequestId(task), period);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
			return super.scheduleWithFixedDelay(new RunnableWithRequestId(task), startTime, delay);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
			return super.scheduleWithFixedDelay(new RunnableWithRequestId(task), delay);
		}

	}
}
