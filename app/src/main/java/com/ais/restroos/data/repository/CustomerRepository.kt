package com.ais.restroos.data.repository

import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.CustomerSyncDto
import com.ais.restroos.data.remote.dtos.KhataRecordRequest
import com.ais.restroos.domain.model.Customer
import com.ais.restroos.domain.model.KhataEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

object CustomerRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        repoScope.launch {
            try {
                RetrofitClient.apiService.saveCustomer(
                    CustomerSyncDto(
                        id = customer.id,
                        name = customer.name,
                        mobile_number = customer.mobileNumber,
                        is_regular = customer.isRegular
                    )
                )
            } catch (_: Exception) {}
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
                        isRegular = dto.is_regular,
                        udhaarBalance = dto.udhaar_balance
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

    suspend fun fetchKhataLedger(mobile: String? = null): Result<List<KhataEntry>> {
        return try {
            val resp = RetrofitClient.apiService.getKhataLedger(mobile)
            if (resp.isSuccessful && resp.body()?.status == "success") {
                val list = resp.body()?.data.orEmpty().map { dto ->
                    KhataEntry(
                        id = dto.id,
                        customerName = dto.customer_name,
                        mobileNumber = dto.mobile_number,
                        entryType = dto.entry_type,
                        amount = dto.amount,
                        note = dto.note,
                        createdAt = dto.created_at
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch khata"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordKhata(
        name: String,
        mobile: String,
        entryType: String,
        amount: Double,
        note: String = ""
    ): Result<Unit> {
        return try {
            val resp = RetrofitClient.apiService.recordKhataEntry(
                KhataRecordRequest(
                    customer_name = name,
                    mobile_number = mobile,
                    entry_type = entryType,
                    amount = amount,
                    note = note
                )
            )
            if (resp.isSuccessful && resp.body()?.status == "success") {
                fetchCustomersFromBackend()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save khata entry"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}