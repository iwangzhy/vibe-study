package com.vibe.common.core.utils;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 */
@Component
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳 (2024-01-01 00:00:00)
     */
    private final long twepoch = 1704067200000L;

    /**
     * 机器ID所占位数
     */
    private final long workerIdBits = 5L;

    /**
     * 数据中心ID所占位数
     */
    private final long datacenterIdBits = 5L;

    /**
     * 序列号所占位数
     */
    private final long sequenceBits = 12L;

    /**
     * 机器ID最大值
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 数据中心ID最大值
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * 机器ID左移位数
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据中心ID左移位数
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间戳左移位数
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 序列号掩码
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 机器ID
     */
    private long workerId;

    /**
     * 数据中心ID
     */
    private long datacenterId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     */
    public SnowflakeIdGenerator() {
        this(0L, 0L);
    }

    /**
     * 构造函数
     *
     * @param workerId     机器ID
     * @param datacenterId 数据中心ID
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 序列号溢出
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 生成ID
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    /**
     * 等待下一毫秒
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
