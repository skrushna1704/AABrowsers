package com.kododake.ecommerce.services

import com.kododake.ecommerce.exceptions.AuthenticationException
import com.kododake.ecommerce.exceptions.NotFoundException
import com.kododake.ecommerce.exceptions.ValidationException
import com.kododake.ecommerce.models.Address
import com.kododake.ecommerce.models.Admin
import com.kododake.ecommerce.models.Customer
import com.kododake.ecommerce.models.TransactionType
import com.kododake.ecommerce.models.UserRole
import com.kododake.ecommerce.repository.UserRepository
import com.kododake.ecommerce.utils.RandomGenerator
import com.kododake.ecommerce.utils.Validator
import java.util.concurrent.ConcurrentHashMap

class LoginService(
    private val userRepo: UserRepository = UserRepository.instance
) {
    private var currentUser: com.kododake.ecommerce.models.User? = null
    private val otpStore = ConcurrentHashMap<String, Pair<String, Long>>()

    fun getCurrentUser() = currentUser
    fun getCurrentCustomer(): Customer? = currentUser as? Customer
    fun getCurrentAdmin(): Admin? = currentUser as? Admin
    fun isLoggedIn(): Boolean = currentUser != null

    fun registerCustomer(name: String, email: String, password: String, phone: String): Customer {
        Validator.validateEmail(email)
        Validator.validatePassword(password)
        Validator.validatePhone(phone)
        if (userRepo.existsByEmail(email)) throw ValidationException("Email already registered")

        val customer = Customer(
            id = RandomGenerator.accountId(),
            name = Validator.requireNonBlank(name, "Name"),
            email = email.lowercase(),
            passwordHash = userRepo.hashPassword(password),
            phone = phone
        )
        userRepo.save(customer)
        return customer
    }

    fun login(email: String, password: String): com.kododake.ecommerce.models.User {
        val user = userRepo.findByEmail(email)
            ?: throw AuthenticationException("Invalid email or password")

        if (user is Customer && user.isLocked) {
            throw AuthenticationException("Account locked due to multiple failed attempts. Contact support.")
        }

        if (!userRepo.verifyPassword(password, user.passwordHash)) {
            if (user is Customer) {
                user.failedLoginAttempts++
                if (user.failedLoginAttempts >= 3) user.isLocked = true
            }
            throw AuthenticationException("Invalid email or password")
        }

        if (user is Customer) user.failedLoginAttempts = 0
        currentUser = user
        return user
    }

    fun logout() {
        currentUser = null
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = currentUser ?: throw AuthenticationException("Not logged in")
        if (!userRepo.verifyPassword(oldPassword, user.passwordHash)) {
            throw AuthenticationException("Current password is incorrect")
        }
        user.passwordHash = userRepo.hashPassword(Validator.validatePassword(newPassword))
    }

    fun forgotPassword(email: String): String {
        val user = userRepo.findByEmail(email) ?: throw NotFoundException("Email not found")
        val otp = RandomGenerator.otp()
        otpStore[email.lowercase()] = otp to System.currentTimeMillis()
        return otp
    }

    fun verifyOtp(email: String, otp: String): Boolean {
        val stored = otpStore[email.lowercase()] ?: return false
        val (storedOtp, timestamp) = stored
        val expired = System.currentTimeMillis() - timestamp > 10 * 60 * 1000
        if (expired) {
            otpStore.remove(email.lowercase())
            return false
        }
        return storedOtp == otp
    }

    fun resetPassword(email: String, otp: String, newPassword: String) {
        if (!verifyOtp(email, otp)) throw ValidationException("Invalid or expired OTP")
        val user = userRepo.findByEmail(email) ?: throw NotFoundException("User not found")
        user.passwordHash = userRepo.hashPassword(Validator.validatePassword(newPassword))
        otpStore.remove(email.lowercase())
    }

    fun addAddress(customerId: String, label: String, street: String, city: String, state: String, pincode: String, isDefault: Boolean): Address {
        val customer = userRepo.findCustomerById(customerId) ?: throw NotFoundException("Customer not found")
        if (isDefault) customer.addresses.forEach { it.isDefault = false }
        val address = Address(
            id = "ADDR${System.currentTimeMillis()}",
            label = Validator.requireNonBlank(label, "Label"),
            street = Validator.requireNonBlank(street, "Street"),
            city = Validator.requireNonBlank(city, "City"),
            state = Validator.requireNonBlank(state, "State"),
            pincode = Validator.requireNonBlank(pincode, "Pincode"),
            isDefault = isDefault
        )
        customer.addresses.add(address)
        return address
    }

    fun rechargeWallet(customerId: String, amount: Double) {
        val customer = userRepo.findCustomerById(customerId) ?: throw NotFoundException("Customer not found")
        val validAmount = Validator.validatePositive(amount, "Amount")
        customer.wallet.credit(validAmount, TransactionType.RECHARGE, "Wallet recharge")
    }

    fun redeemLoyaltyPoints(customerId: String): Double {
        val customer = userRepo.findCustomerById(customerId) ?: throw NotFoundException("Customer not found")
        if (customer.loyaltyPoints < 100) throw ValidationException("Need at least 100 points to redeem")
        val redeemable = (customer.loyaltyPoints / 100) * 100
        val cashback = (redeemable / 100) * 100.0
        customer.loyaltyPoints -= redeemable
        customer.wallet.credit(cashback, TransactionType.CASHBACK, "Loyalty points redemption")
        return cashback
    }
}
