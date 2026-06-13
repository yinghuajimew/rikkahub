package me.rerere.ai.provider

import android.content.Context
import me.rerere.ai.provider.providers.ClaudeProvider
import me.rerere.ai.provider.providers.GoogleProvider
import me.rerere.ai.provider.providers.OpenAIProvider
import okhttp3.OkHttpClient

/**
 * Provider管理器，负责注册和获取Provider实例
 */
class ProviderManager(client: OkHttpClient, context: Context) {
    // 存储已注册的Provider实例
    private val providers = mutableMapOf<String, Provider<*>>()

    init {
    // 在原始 client 基础上套一层 User-Agent 拦截器
    val patchedClient = client.newBuilder()
        .addInterceptor { chain ->
            val original = chain.request()
            val req = if (original.header("User-Agent") != null) {
                // 用户已在自定义 Header 里设置了 UA，直接保留
                original
            } else {
                // 否则注入默认 UA，避免暴露底层客户端
                original.newBuilder()
                    .header("User-Agent", "RikkaHub/1.0 (Android)")
                    .build()
            }
            chain.proceed(req)
        }
        .build()

    // 注册默认Provider（全部换成 patchedClient）
    registerProvider("openai", OpenAIProvider(patchedClient, context))
    registerProvider("google", GoogleProvider(patchedClient, context))
    registerProvider("claude", ClaudeProvider(patchedClient, context))
}

    /**
     * 注册Provider实例
     *
     * @param name Provider名称
     * @param provider Provider实例
     */
    fun registerProvider(name: String, provider: Provider<*>) {
        providers[name] = provider
    }

    /**
     * 获取Provider实例
     *
     * @param name Provider名称
     * @return Provider实例，如果不存在则返回null
     */
    fun getProvider(name: String): Provider<*> {
        return providers[name] ?: throw IllegalArgumentException("Provider not found: $name")
    }

    /**
     * 根据ProviderSetting获取对应的Provider实例
     *
     * @param setting Provider设置
     * @return Provider实例，如果不存在则返回null
     */
    fun <T : ProviderSetting> getProviderByType(setting: T): Provider<T> {
        @Suppress("UNCHECKED_CAST")
        return when (setting) {
            is ProviderSetting.OpenAI -> getProvider("openai")
            is ProviderSetting.Google -> getProvider("google")
            is ProviderSetting.Claude -> getProvider("claude")
        } as Provider<T>
    }
}
