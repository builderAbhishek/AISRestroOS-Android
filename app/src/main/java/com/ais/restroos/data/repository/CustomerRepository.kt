package com.ais.restroos.data.repository

import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.domain.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

object CustomerRepository {

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    fun findCustomerByMobile(mobileNumber: String): Customer? {
        return _customers.value.find { it.mobileNumber == mobileNumber }
    }

    fun saveOrUpdateCustomer(name: String, mobileNumber: String, isRegular: Boolean): Customer {
        val existing = findCustomerByMobile(mobileNumber)
        val customer = existing?.copy(name = name, isRegular = isRegular)
            ?: Customer(
                id = "CUST-${UUID.randomUUID().toString().take(6).uppercase()}",
                name = name,
                mobileNumber = mobileNumber,
                isRegular = isRegular
            )

        _customers.update { list ->
            val index = list.indexOfFirst { it.mobileNumber == mobileNumber }
            if (index >= 0) {
                list.toMutableList().apply { set(index, customer) }
            } else {
                list + customer
            }
        }
        return customer
    }

    suspend fun fetchCustomersFromBackend(): Result<List<Customer>> {
        return try {
            val response = RetrofitClient.apiService.getCustomers()
            if (response.isSuccessful && response.body()?.status == "success") {
                val dtoList = response.body()?.data ?: emptyList()
                val list = dtoList.map { dto ->
                    Customer(
                        id = dto.id ?: "CUST-${UUID.randomUUID().toString().take(6)}",
                        name = dto.name,
                        mobileNumber = dto.mobile_number,
                        isRegular = dto.is_regular
                    )
                }
                _customers.value = list
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch customers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}