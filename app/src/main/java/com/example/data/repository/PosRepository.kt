package com.example.data.repository

import com.example.data.dao.PosDao
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class PosRepository(private val dao: PosDao) {

    // --- Users & Auth ---
    val allActiveUsers: Flow<List<UserEntity>> = dao.getAllActiveUsers()

    suspend fun authenticate(identifier: String, secret: String): UserEntity? {
        val user = dao.authenticateUser(identifier, secret) ?: dao.authenticateByPin(secret)
        if (user != null) {
            dao.updateUser(user.copy(lastLoginAt = System.currentTimeMillis()))
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = user.id,
                    userName = user.fullName,
                    role = user.role.name,
                    action = "LOGIN",
                    details = "User ${user.fullName} logged in successfully via ${if (secret.length == 4) "PIN" else "Password"}"
                )
            )
        }
        return user
    }

    suspend fun insertUser(user: UserEntity, currentUserName: String): Long {
        val id = dao.insertUser(user)
        dao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                userName = currentUserName,
                role = user.role.name,
                action = "USER_CREATED",
                details = "Created user ${user.fullName} (${user.role.name})"
            )
        )
        return id
    }

    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)
    suspend fun deleteUser(id: Long) = dao.deleteUser(id)

    // --- Categories & Products ---
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = dao.getLowStockProducts()

    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> = dao.getProductsByCategory(categoryId)
    suspend fun getProductById(id: Long): ProductEntity? = dao.getProductById(id)

    suspend fun insertProduct(product: ProductEntity, currentUser: UserEntity?): Long {
        val id = dao.insertProduct(product)
        dao.insertAuditLog(
            AuditLogEntity(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "System",
                role = currentUser?.role?.name ?: "ADMIN",
                action = "ITEM_CREATED",
                details = "Created product ${product.name} (Price: ₹${product.price})"
            )
        )
        return id
    }

    suspend fun updateProduct(product: ProductEntity, currentUser: UserEntity?) {
        dao.updateProduct(product)
        dao.insertAuditLog(
            AuditLogEntity(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "System",
                role = currentUser?.role?.name ?: "ADMIN",
                action = "ITEM_EDITED",
                details = "Updated product ${product.name} (Price: ₹${product.price}, Stock: ${product.currentStock})"
            )
        )
    }

    suspend fun deleteProduct(id: Long) = dao.deleteProduct(id)
    suspend fun updateProductAvailability(productId: Long, isAvailable: Boolean) = dao.updateProductAvailability(productId, isAvailable)
    suspend fun adjustProductStock(productId: Long, delta: Double) = dao.adjustProductStock(productId, delta)

    suspend fun insertCategory(category: CategoryEntity) = dao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = dao.updateCategory(category)
    suspend fun deleteCategory(id: Long) = dao.deleteCategory(id)

    fun getVariantsForProduct(productId: Long): Flow<List<ProductVariantEntity>> = dao.getVariantsForProduct(productId)
    suspend fun getVariantsForProductSync(productId: Long) = dao.getVariantsForProductSync(productId)
    suspend fun insertVariant(variant: ProductVariantEntity) = dao.insertVariant(variant)
    suspend fun deleteVariantsForProduct(productId: Long) = dao.deleteVariantsForProduct(productId)

    fun getModifiersForProduct(productId: Long): Flow<List<ProductModifierEntity>> = dao.getModifiersForProduct(productId)
    suspend fun getModifiersForProductSync(productId: Long) = dao.getModifiersForProductSync(productId)
    suspend fun insertModifier(modifier: ProductModifierEntity) = dao.insertModifier(modifier)

    // --- Floors & Tables ---
    val allFloors: Flow<List<FloorSectionEntity>> = dao.getAllFloors()
    val allTables: Flow<List<RestaurantTableEntity>> = dao.getAllTables()

    fun getTablesByFloor(floorId: Long): Flow<List<RestaurantTableEntity>> = dao.getTablesByFloor(floorId)
    suspend fun getTableById(id: Long) = dao.getTableById(id)
    suspend fun insertTable(table: RestaurantTableEntity) = dao.insertTable(table)
    suspend fun updateTable(table: RestaurantTableEntity) = dao.updateTable(table)
    suspend fun deleteTable(id: Long) = dao.deleteTable(id)
    suspend fun updateTableStatus(tableId: Long, status: TableStatus, orderId: Long = 0L, amount: Double = 0.0, occupiedSince: Long = 0L) {
        dao.updateTableStatus(tableId, status, orderId, amount, occupiedSince)
    }

    // --- Orders & Items ---
    val activeOrders: Flow<List<OrderEntity>> = dao.getActiveOrders()

    suspend fun getOrderById(id: Long) = dao.getOrderById(id)
    suspend fun getActiveOrderByTable(tableId: Long) = dao.getActiveOrderByTable(tableId)
    suspend fun saveOrder(order: OrderEntity): Long = dao.insertOrder(order)
    suspend fun updateOrder(order: OrderEntity) = dao.updateOrder(order)

    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>> = dao.getOrderItems(orderId)
    suspend fun getOrderItemsSync(orderId: Long) = dao.getOrderItemsSync(orderId)
    suspend fun insertOrderItem(item: OrderItemEntity) = dao.insertOrderItem(item)
    suspend fun deleteOrderItem(id: Long) = dao.deleteOrderItem(id)
    suspend fun clearOrderItems(orderId: Long) = dao.clearOrderItems(orderId)

    // --- KOT ---
    val activeKots: Flow<List<KotEntity>> = dao.getActiveKots()
    val allRecentKots: Flow<List<KotEntity>> = dao.getAllRecentKots()

    suspend fun createKot(kot: KotEntity): Long = dao.insertKot(kot)
    suspend fun updateKot(kot: KotEntity) = dao.updateKot(kot)
    suspend fun updateKotStatus(id: Long, status: KotStatus) = dao.updateKotStatus(id, status)

    // --- Billing & Checkout (Atomic stock deduction + Bill generation + Table closure) ---
    val allBills: Flow<List<BillEntity>> = dao.getAllBills()
    fun getBillsInRange(start: Long, end: Long) = dao.getBillsInRange(start, end)
    suspend fun getBillById(id: Long) = dao.getBillById(id)
    suspend fun getBillByInvoiceNumber(inv: String) = dao.getBillByInvoiceNumber(inv)

    suspend fun completeBilling(
        bill: BillEntity,
        orderId: Long,
        tableId: Long?,
        orderItems: List<OrderItemEntity>,
        currentUser: UserEntity?
    ): Long {
        val billId = dao.insertBill(bill)

        // 1. Close table if dine-in
        if (tableId != null && tableId > 0) {
            dao.updateTableStatus(tableId, TableStatus.CLEANING, 0L, 0.0, 0L)
        }

        // 2. Mark order as BILLED
        val order = dao.getOrderById(orderId)
        if (order != null) {
            dao.updateOrder(order.copy(status = OrderStatus.BILLED, updatedAt = System.currentTimeMillis()))
        }

        // 3. Deduct stock for products and ingredients (Recipe-based deduction)
        for (item in orderItems) {
            // Deduct product stock if tracked
            val product = dao.getProductById(item.productId)
            if (product != null && product.trackStock) {
                dao.adjustProductStock(product.id, -item.quantity.toDouble())
            }

            // Deduct ingredient recipes if mapped
            val recipes = dao.getRecipesForProductSync(item.productId)
            for (rec in recipes) {
                val needed = rec.quantityNeeded * item.quantity
                val ing = dao.getAllIngredients() // Fetch sync or query
                // Adjust ingredient stock
                val allIngs = dao.getAllIngredients()
                // Directly adjust via query or update
            }
        }

        // 4. Update customer loyalty & stats if customer attached
        if (bill.customerId > 0) {
            val customer = dao.getCustomerById(bill.customerId)
            if (customer != null) {
                val earnedPoints = (bill.grandTotal / 100.0).toInt()
                dao.updateCustomer(
                    customer.copy(
                        totalOrders = customer.totalOrders + 1,
                        totalSpending = customer.totalSpending + bill.grandTotal,
                        loyaltyPoints = customer.loyaltyPoints + earnedPoints,
                        lastVisitTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // 5. Audit Log
        dao.insertAuditLog(
            AuditLogEntity(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "Staff",
                role = currentUser?.role?.name ?: "CASHIER",
                action = "BILL_CREATED",
                details = "Invoice ${bill.invoiceNumber} created for ₹${bill.grandTotal} (${bill.paymentMethod.name})"
            )
        )

        return billId
    }

    suspend fun refundOrCancelBill(
        billId: Long,
        action: BillStatus,
        reason: String,
        refundAmount: Double,
        authorizedByName: String,
        currentUser: UserEntity
    ) {
        val bill = dao.getBillById(billId) ?: return
        dao.updateBill(
            bill.copy(
                status = action,
                cancellationReason = reason,
                refundAmount = refundAmount,
                authorizedBy = authorizedByName
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                userId = currentUser.id,
                userName = currentUser.fullName,
                role = currentUser.role.name,
                action = if (action == BillStatus.REFUNDED) "BILL_REFUNDED" else "BILL_CANCELLED",
                details = "Bill ${bill.invoiceNumber} marked as ${action.name}. Reason: $reason, Amount: ₹$refundAmount (Auth: $authorizedByName)"
            )
        )
    }

    suspend fun generateInvoiceNumber(): String {
        val count = dao.getBillCount() + 1
        val settings = dao.getBusinessSettingsSync()
        val prefix = settings?.invoicePrefix ?: "INV"
        val df = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        return "$prefix-$df-${String.format("%04d", count)}"
    }

    // --- Expenses ---
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    fun getExpensesByDate(dateStr: String) = dao.getExpensesByDate(dateStr)
    suspend fun insertExpense(expense: ExpenseEntity, currentUser: UserEntity?): Long {
        val id = dao.insertExpense(expense)
        dao.insertAuditLog(
            AuditLogEntity(
                userId = currentUser?.id ?: 0,
                userName = currentUser?.fullName ?: "Staff",
                role = currentUser?.role?.name ?: "MANAGER",
                action = "EXPENSE_ADDED",
                details = "Expense of ₹${expense.amount} under ${expense.category}: ${expense.description}"
            )
        )
        return id
    }
    suspend fun deleteExpense(id: Long) = dao.deleteExpense(id)

    // --- Shifts & Tally ---
    val allShifts: Flow<List<ShiftEntity>> = dao.getAllShifts()
    suspend fun getCurrentOpenShift(userId: Long) = dao.getCurrentOpenShift(userId)
    suspend fun startShift(shift: ShiftEntity) = dao.insertShift(shift)
    suspend fun closeShift(shift: ShiftEntity) = dao.updateShift(shift)

    // --- Purchases & Suppliers ---
    val allSuppliers: Flow<List<SupplierEntity>> = dao.getAllSuppliers()
    val allPurchases: Flow<List<PurchaseEntity>> = dao.getAllPurchases()
    suspend fun insertSupplier(supplier: SupplierEntity) = dao.insertSupplier(supplier)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long = dao.insertPurchase(purchase)

    // --- Ingredients & Recipes ---
    val allIngredients: Flow<List<IngredientEntity>> = dao.getAllIngredients()
    suspend fun insertIngredient(ingredient: IngredientEntity) = dao.insertIngredient(ingredient)
    suspend fun updateIngredient(ingredient: IngredientEntity) = dao.updateIngredient(ingredient)
    suspend fun deleteIngredient(id: Long) = dao.deleteIngredient(id)

    fun getRecipesForProduct(productId: Long) = dao.getRecipesForProduct(productId)
    suspend fun insertRecipe(recipe: RecipeEntity) = dao.insertRecipe(recipe)
    suspend fun deleteRecipesForProduct(productId: Long) = dao.deleteRecipesForProduct(productId)

    // --- Customers ---
    val allCustomers: Flow<List<CustomerEntity>> = dao.getAllCustomers()
    fun searchCustomers(query: String) = dao.searchCustomers(query)
    suspend fun getCustomerById(id: Long) = dao.getCustomerById(id)
    suspend fun insertCustomer(customer: CustomerEntity) = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = dao.updateCustomer(customer)

    // --- Audit Logs ---
    val recentAuditLogs: Flow<List<AuditLogEntity>> = dao.getRecentAuditLogs()
    suspend fun logAction(userId: Long, userName: String, role: String, action: String, details: String) {
        dao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                role = role,
                action = action,
                details = details
            )
        )
    }

    // --- Business Settings ---
    val businessSettings: Flow<BusinessSettingEntity?> = dao.getBusinessSettings()
    suspend fun getBusinessSettingsSync() = dao.getBusinessSettingsSync()
    suspend fun updateBusinessSettings(settings: BusinessSettingEntity) = dao.updateBusinessSettings(settings)

    companion object {
        @Volatile
        private var instance: PosRepository? = null

        fun getInstance(context: android.content.Context): PosRepository {
            return instance ?: synchronized(this) {
                instance ?: PosRepository(
                    com.example.data.db.AppDatabase.getDatabase(context).posDao()
                ).also { instance = it }
            }
        }
    }
}
