package com.example.demo.core.hibernate.id;

import java.util.Random;

import lombok.Value;

public class Snowflake {

	private final static long EPOCH = 1556150400000L;
	private final static Random RANDOM = new Random();
	private final int workerId;
	private final int workerIdBits;
	private final int sequenceBits;
	private final long sequenceMask;
	private long sequence = 0L;
	private long lastTimestamp = -1L;

	public Snowflake(int workerId) {
		this(workerId, 8, 10);
	}

	public Snowflake(int workerId, int workerIdBits, int sequenceBits) {
		long maxWorkerId = -1L ^ -1L << workerIdBits;
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(
					String.format("workerId can't be greater than %d or less than 0", maxWorkerId));
		}
		this.workerId = workerId;
		this.workerIdBits = workerIdBits;
		this.sequenceBits = sequenceBits;
		this.sequenceMask = -1L ^ -1L << sequenceBits;
	}

	public synchronized long nextId() {
		long timestamp = System.currentTimeMillis();
		if (timestamp == lastTimestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				timestamp = System.currentTimeMillis();
				while (timestamp <= lastTimestamp) {
					timestamp = System.currentTimeMillis();
				}
			}
		} else if (timestamp > lastTimestamp) {
			sequence = RANDOM.nextInt(2);
		} else {
			long offset = lastTimestamp - timestamp;
			if (offset < 5000) {
				try {
					Thread.sleep(offset + 1);
					timestamp = System.currentTimeMillis();
					sequence = RANDOM.nextInt(2);
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			} else {
				throw new IllegalStateException(
						String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
			}
		}
		lastTimestamp = timestamp;
		return ((timestamp - EPOCH) << (sequenceBits + workerIdBits)) | (workerId << sequenceBits) | sequence;
	}

	public Info parse(long id) {
		return new Info(id, workerIdBits, sequenceBits);
	}

	@Value
	public static class Info {
		private long timestamp;
		private int workerId;
		private long sequence;

		Info(long id, int workerIdBits, int sequenceBits) {
			long duration = id >> (sequenceBits + workerIdBits);
			timestamp = EPOCH + duration;
			workerId = (int) ((id - (duration << (sequenceBits + workerIdBits))) >> (sequenceBits));
			sequence = id - (duration << (sequenceBits + workerIdBits)) - (workerId << sequenceBits);
		}
	}

}
