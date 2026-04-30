package com.donabere.amm.network

import com.donabere.amm.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()
        
        // Si no hay token o la petición es de login, pasa la petición sin cambios
        if (token.isNullOrEmpty() || originalRequest.url().encodedPath().contains("/auth/login")) {
            return chain.proceed(originalRequest)
        }

        // Si hay token, lo agrega al header
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
            
        return chain.proceed(newRequest)
    }
}
