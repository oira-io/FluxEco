package io.oira.fluxeco.core.redis

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.manager.ConfigManager
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.time.Duration

object RedisManager {
    private var jedisPool: JedisPool? = null
    private var redisCache: RedisCache? = null
    private var redisPublisher: RedisPublisher? = null
    private var redisListener: RedisListener? = null
    private val plugin: FluxEco = FluxEco.instance
    private val cfg = ConfigManager(plugin, "database.yml").getConfig()

    var isEnabled: Boolean = false
        private set

    var serverId: String = "server1"
        private set

    fun init() {
        isEnabled = cfg.getBoolean("redis.enabled", false)

        if (!isEnabled) {
            plugin.logger.info("Redis is disabled in configuration")
            return
        }

        try {
            serverId = cfg.getString("redis.server-id", "server1") ?: "server1"
            val host = cfg.getString("redis.host", "localhost") ?: "localhost"
            val port = cfg.getInt("redis.port", 6379)
            val username = cfg.getString("redis.username")?.takeIf { it.isNotBlank() }
            val password = cfg.getString("redis.password")?.takeIf { it.isNotBlank() }
            val database = cfg.getInt("redis.database", 0)
            val timeout = cfg.getInt("redis.timeout", 2000)
            val poolSize = cfg.getInt("redis.poolSize", 10)

            val poolConfig = JedisPoolConfig().apply {
                maxTotal = poolSize
                maxIdle = poolSize
                minIdle = 1
                testOnBorrow = true
                testOnReturn = true
                testWhileIdle = true
                minEvictableIdleTime = Duration.ofSeconds(60)
                timeBetweenEvictionRuns = Duration.ofSeconds(30)
                numTestsPerEvictionRun = 3
                blockWhenExhausted = true
            }

            jedisPool = if (username != null && password != null) {
                JedisPool(poolConfig, host, port, timeout, username, password, database)
            } else if (password != null) {
                JedisPool(poolConfig, host, port, timeout, password, database)
            } else {
                JedisPool(poolConfig, host, port, timeout)
            }

            jedisPool?.resource?.use { jedis ->
                jedis.ping()
                if (database != 0) {
                    jedis.select(database)
                }
            }

            redisCache = RedisCache(jedisPool!!, serverId)
            redisPublisher = RedisPublisher(jedisPool!!)
            redisListener = RedisListener(jedisPool!!, redisCache!!)

            redisListener?.start()

            plugin.logger.info("Redis initialized successfully! Server ID: $serverId")
            plugin.logger.info("Connected to Redis at $host:$port (database: $database)")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize Redis: ${e.message}")
            e.printStackTrace()
            isEnabled = false
            shutdown()
        }
    }

    fun getCache(): RedisCache? = redisCache

    fun getPublisher(): RedisPublisher? = redisPublisher

    fun shutdown() {
        try {
            redisListener?.stop()
            redisCache?.cleanup()
            jedisPool?.close()
            plugin.logger.info("Redis connection closed")
        } catch (e: Exception) {
            plugin.logger.warning("Error during Redis shutdown: ${e.message}")
            e.printStackTrace()
        }
    }
}