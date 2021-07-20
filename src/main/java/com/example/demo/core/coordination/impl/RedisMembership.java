package com.example.demo.core.coordination.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;

import com.example.demo.core.Application;
import com.example.demo.core.coordination.Membership;

public class RedisMembership implements Membership {

	private static final String NAMESPACE = "membership:";

	private Set<String> groups = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Value("${redis.membership.heartbeat:60000}")
	private int heartbeat = 60000;

	private final String self;

	private final StringRedisTemplate stringRedisTemplate;

	private final TaskScheduler taskScheduler;

	public RedisMembership(Application application, StringRedisTemplate stringRedisTemplate,
			TaskScheduler taskScheduler) {
		this.self = application.getInstanceId(false);
		this.stringRedisTemplate = stringRedisTemplate;
		this.taskScheduler = taskScheduler;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		taskScheduler.scheduleAtFixedRate(() -> {
			for (String group : groups) {
				List<String> members = getMembers(group);

				if (!members.contains(self))
					stringRedisTemplate.opsForList().rightPush(NAMESPACE + group, self);
				for (String member : members) {
					if (member.equals(self))
						continue;
					boolean alive = false;
					String url = "http://" + member + "/actuator/health";
					try {
						HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
						conn.setConnectTimeout(3000);
						conn.setReadTimeout(2000);
						conn.setInstanceFollowRedirects(false);
						conn.setDoOutput(false);
						conn.setUseCaches(false);
						conn.connect();
						if (conn.getResponseCode() == 200) {
							try (BufferedReader br = new BufferedReader(
									new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
								String value = br.lines().collect(Collectors.joining("\n"));
								if (value.contains("UP")) {
									alive = true;
								}
							}
						}
						conn.disconnect();
					} catch (IOException e) {
					}
					if (!alive)
						stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0, member);
				}
			}
		}, heartbeat);
	}

	@Override
	public void join(String group) {
		stringRedisTemplate.opsForList().leftPush(NAMESPACE + group, self);
		groups.add(group);
	}

	@Override
	public void leave(String group) {
		stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0, self);
		groups.remove(group);
	}

	@Override
	public boolean isLeader(String group) {
		return self.equals(getLeader(group));
	}

	@Override
	public String getLeader(String group) {
		List<String> list = getMembers(group);
		if (!list.isEmpty())
			return list.get(0);
		return null;
	}

	@Override
	public List<String> getMembers(String group) {
		return stringRedisTemplate.opsForList().range(NAMESPACE + group, 0, -1);
	}

	@PreDestroy
	public void destroy() {
		for (String group : groups)
			stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0, self);
	}

}
