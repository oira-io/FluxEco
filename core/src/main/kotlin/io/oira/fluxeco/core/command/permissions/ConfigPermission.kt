package io.oira.fluxeco.core.command.permissions

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ConfigPermission(
    val key: String,
    val file: String = "commands.yml"
)
