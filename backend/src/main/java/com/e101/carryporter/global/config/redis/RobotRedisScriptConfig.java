package com.e101.carryporter.global.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RobotRedisScriptConfig {

    @Bean("acquireRobotScript")
    public RedisScript<Long> acquireRobotScript() {
        String script = """
                -- [KEYS]
                local queueKey = KEYS[1]      -- robot:available (List)
                
                -- [ARGV]
                local statusKeyPrefix = ARGV[1]  -- robot:status:
                local newStatus = ARGV[2]        -- BUSY
                local updatedAt = ARGV[3]

                -- 1. [FIFO] 대기열의 맨 앞(Left)에서 하나 꺼냄
                local robotId = redis.call('LPOP', queueKey)

                -- 2. 없으면 nil 반환 (Java에서는 null로 받음)
                if not robotId then
                    return nil
                end

                -- 3. [Atomic] 꺼낸 로봇의 상태를 즉시 BUSY로 변경
                -- Hash Key 동적 생성 (prefix + robotId)
                local hashKey = statusKeyPrefix .. robotId
                
                redis.call('HSET', hashKey, 'status', newStatus, 'updatedAt', updatedAt)

                -- 4. 로봇 ID 반환
                return tonumber(robotId)
                """;

        return new DefaultRedisScript<>(script, Long.class);
    }

    @Bean("updateRobotInfoScript")
    public RedisScript<Long> updateRobotInfoScript() {
        String script = """
                -- [KEYS]
                local hashKey = KEYS[1]      -- robot:status:{id}
                local queueKey = KEYS[2]     -- robot:available (List)

                -- [ARGV]
                local robotId = ARGV[1]
                local status = ARGV[2]
                local battery = ARGV[3]      -- null 가능
                local updatedAt = ARGV[4]

                -- 1. 해시(Hash) 정보 업데이트 (Status, UpdatedAt은 필수)
                redis.call('HSET', hashKey, 'status', status, 'updatedAt', updatedAt)

                -- 2. 배터리 부분 업데이트 (Partial Update)
                -- Java에서 null을 보내면 문자열 'null'로 오거나 빈 값일 수 있어서 체크
                if battery ~= nil and battery ~= 'null' and battery ~= '' then
                    redis.call('HSET', hashKey, 'battery', battery)
                end

                -- 3. 대기열(List) 관리 로직
                -- (핵심) 일단 큐에서 무조건 지웁니다 (중복 방지 & 상태 변경 시 제거 목적)
                -- LREM key count value: count가 0이면 일치하는 모든 요소 제거
                redis.call('LREM', queueKey, 0, robotId)

                -- (핵심) 가용 상태(IDLE)가 되면 큐의 맨 뒤(Right)에 줄을 세웁니다.
                if status == 'IDLE' then
                    redis.call('RPUSH', queueKey, robotId)
                end

                -- BUSY, WORKING 등 다른 상태라면?
                -- 위에서 LREM으로 이미 지워졌으므로 아무것도 안 하면 됨 (큐에서 사라짐)

                return 1
                """;

        // 반환 타입 Long (성공 시 1)
        return new DefaultRedisScript<>(script, Long.class);
    }

    @Bean("registerRobotScript")
    public RedisScript<Long> registerRobotScript() {
        String script = """
                -- [KEYS]
                local hashKey = KEYS[1]      -- robot:status:{id}
                local queueKey = KEYS[2]     -- robot:available (List)

                -- [ARGV]
                local robotId = ARGV[1]
                local macAddress = ARGV[2]
                local status = ARGV[3]
                local battery = ARGV[4]
                local updatedAt = ARGV[5]

                -- 1. Redis Hash 생성 (초기 로봇 정보 저장)
                redis.call('HSET', hashKey,
                    'macAddress', macAddress,
                    'status', status,
                    'battery', battery,
                    'updatedAt', updatedAt)

                -- 2. 상태가 IDLE이면 가용 큐에 추가 (중복 방지를 위해 먼저 제거)
                redis.call('LREM', queueKey, 0, robotId)

                if status == 'IDLE' then
                    redis.call('RPUSH', queueKey, robotId)
                end

                return 1
                """;

        return new DefaultRedisScript<>(script, Long.class);
    }
}
