package com.example.demo.core.coordination.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.core.Application;
import com.example.demo.core.coordination.Membership;

public class StandaloneMembership implements Membership {

	private Map<String, List<String>> groups = new HashMap<>();

	private final String self;

	public StandaloneMembership(Application application) {
		this.self = application.getInstanceId(false);
	}

	@Override
	public void join(String group) {
		List<String> members = groups.get(group);
		if (members == null) {
			members = new ArrayList<>();
			groups.put(group, members);
		}
		if (!members.contains(self))
			members.add(self);
	}

	@Override
	public void leave(String group) {
		List<String> members = groups.get(group);
		if (members != null) {
			members.remove(self);
		}
	}

	@Override
	public boolean isLeader(String group) {
		return self.equals(getLeader(group));
	}

	@Override
	public String getLeader(String group) {
		List<String> members = getMembers(group);
		if (members == null || members.isEmpty())
			return null;
		else
			return members.get(0);
	}

	@Override
	public List<String> getMembers(String group) {
		return groups.get(group);
	}

}
