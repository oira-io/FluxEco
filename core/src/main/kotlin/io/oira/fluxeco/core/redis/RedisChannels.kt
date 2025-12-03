package io.oira.fluxeco.core.redis

enum class RedisChannels(val channelName: String) {
    PLAYER_JOIN("fluxeco:player:join"),
    PLAYER_QUIT("fluxeco:player:quit"),
    PAYMENT_NOTIFICATION("fluxeco:payment:notification"),
    ECONOMY_NOTIFICATION("fluxeco:economy:notification")
}