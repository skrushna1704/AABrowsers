package com.kododake.ecommerce.repository

import com.kododake.ecommerce.models.Admin
import com.kododake.ecommerce.models.Customer
import com.kododake.ecommerce.models.User
import com.kododake.ecommerce.models.UserRole
import com.kododake.ecommerce.utils.Constants
import java.io.File

class UserRepository private constructor() {
    private val users = HashMap<String, User>()
    private val emailIndex = HashMap<String, String>()

    companion object {
        val instance: UserRepository by lazy { UserRepository() }
    }

    init {
        seedDefaultAdmin()
    }

    private fun seedDefaultAdmin() {
        val admin = Admin(
            id = "ADM001",
            name = "System Admin",
            email = "admin@ecommerce.com",
            passwordHash = hashPassword("admin123"),
            department = "Operations"
        )
        save(admin)
    }

    fun save(user: User) {
        users[user.id] = user
        emailIndex[user.email.lowercase()] = user.id
    }

    fun findById(id: String): User? = users[id]

    fun findByEmail(email: String): User? {
        val id = emailIndex[email.lowercase()] ?: return null
        return users[id]
    }

    fun findCustomerById(id: String): Customer? = users[id] as? Customer

    fun getAllCustomers(): List<Customer> =
        users.values.filterIsInstance<Customer>()

    fun getAllAdmins(): List<Admin> =
        users.values.filterIsInstance<Admin>()

    fun delete(id: String): Boolean {
        val user = users.remove(id) ?: return false
        emailIndex.remove(user.email.lowercase())
        return true
    }

    fun existsByEmail(email: String): Boolean =
        email.lowercase() in emailIndex

    fun loadFromFile(dataDir: String) {
        val file = File(dataDir, Constants.USERS_FILE)
        if (!file.exists()) return
        file.readLines().forEach { line ->
            if (line.isBlank() || line.startsWith("#")) return@forEach
            val parts = line.split("|")
            if (parts.size < 5) return@forEach
            when (parts[4]) {
                "CUSTOMER" -> {
                    val customer = Customer(
                        id = parts[0],
                        name = parts[1],
                        email = parts[2],
                        passwordHash = parts[3],
                        phone = parts.getOrElse(5) { "" },
                        loyaltyPoints = parts.getOrElse(6) { "0" }.toIntOrNull() ?: 0,
                        totalSpent = parts.getOrElse(7) { "0" }.toDoubleOrNull() ?: 0.0
                    )
                    customer.wallet.balance = parts.getOrElse(8) { "0" }.toDoubleOrNull() ?: 0.0
                    save(customer)
                }
                "ADMIN" -> save(Admin(
                    id = parts[0], name = parts[1], email = parts[2],
                    passwordHash = parts[3], department = parts.getOrElse(5) { "Operations" }
                ))
            }
        }
    }

    fun saveToFile(dataDir: String) {
        val dir = File(dataDir)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, Constants.USERS_FILE)
        file.writeText(buildString {
            appendLine("# id|name|email|passwordHash|role|extra...")
            users.values.forEach { user ->
                when (user) {
                    is Customer -> appendLine(
                        "${user.id}|${user.name}|${user.email}|${user.passwordHash}|CUSTOMER|${user.phone}|${user.loyaltyPoints}|${user.totalSpent}|${user.wallet.balance}"
                    )
                    is Admin -> appendLine(
                        "${user.id}|${user.name}|${user.email}|${user.passwordHash}|ADMIN|${user.department}"
                    )
                    else -> {}
                }
            }
        })
    }

    fun hashPassword(password: String): String =
        password.hashCode().toString(16)

    fun verifyPassword(password: String, hash: String): Boolean =
        hashPassword(password) == hash
}
