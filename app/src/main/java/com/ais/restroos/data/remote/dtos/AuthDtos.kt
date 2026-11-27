package com.ais.restroos.data.remote.dtos

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val restaurant_name: String,
    val phone: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ApiResponseWrapper<T>(
    val status: String,
    val message: String,
    val data: T? = null,
    val errors: Map<String, List<String>>? = null
)

data class AuthData(
    val token: String,
    val user: UserDto,
    val restaurant_id: String? = null,
    val restaurant_name: String? = null,
    val branch_id: String? = null,
    val role: String? = null
)

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String? = null
)