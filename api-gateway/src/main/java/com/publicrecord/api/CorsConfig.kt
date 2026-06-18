package com.publicrecord.api

import com.fasterxml.jackson.annotation.JsonProperty

data class CorsConfig(
    @JsonProperty("allowedOrigins")
    var allowedOrigins: String = "http://localhost:3000,http://localhost:5173,http://localhost:5174,http://localhost:5175,http://localhost:5176,http://localhost:5177,http://localhost:8082",
    @JsonProperty("allowedMethods")
    var allowedMethods: String = "OPTIONS,GET,HEAD",
    @JsonProperty("allowedHeaders")
    var allowedHeaders: String = "X-Requested-With,Content-Type,Accept,Origin,X-Admin-Token",
    @JsonProperty("allowCredentials")
    var allowCredentials: Boolean = false
)
