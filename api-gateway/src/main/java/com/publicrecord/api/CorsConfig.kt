package com.publicrecord.api

import com.fasterxml.jackson.annotation.JsonProperty

data class CorsConfig(
    @JsonProperty("allowedOrigins")
    var allowedOrigins: String = "http://localhost:3000,http://localhost:5173",
    @JsonProperty("allowedMethods")
    var allowedMethods: String = "OPTIONS,GET,HEAD",
    @JsonProperty("allowedHeaders")
    var allowedHeaders: String = "X-Requested-With,Content-Type,Accept,Origin",
    @JsonProperty("allowCredentials")
    var allowCredentials: Boolean = false
)
