package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY id ASC")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE (username = :identifier OR mobile = :identifier) AND (pin = :secret OR passwordHash = :secret) AND isActive = 1 LIMIT 1")
    suspend fun authenticateUser(identifier: String, secret: String): UserEntity?

    @Query("SELECT * FROM users WHERE pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun authenticateByPin(pin: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)

    // --- Categories ---
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY isBestseller DESC, name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)

    @Query("UPDATE products SET isAvailable = :isAvailable WHERE id = :productId")
    suspend fun updateProductAvailability(productId: Long, isAvailable: Boolean)

    @Query("UPDATE products SET currentStock = currentStock + :adjustment WHERE id = :productId")
    suspend fun adjustProductStock(productId: Long, adjustment: Double)

    @Query("SELECT * FROM products WHERE trackStock = 1 AND currentStock <= minStockAlert")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    // --- Variants & Modifiers ---
    @Query("SELECT * FROM product_variants WHERE productId = :productId")
    fun getVariantsForProduct(productId: Long): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants WHERE productId = :productId")
    suspend fun getVariantsForProductSync(productId: Long): List<ProductVariantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity): Long

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteVariantsForProduct(productId: Long)

    @Query("SELECT * FROM product_modifiers WHERE productId = :productId OR productId = 0")
    fun getModifiersForProduct(productId: Long): Flow<List<ProductModifierEntity>>

    @Query("SELECT * FROM product_modifiers WHERE productId = :productId OR productId = 0")
    suspend fun getModifiersForProductSync(productId: Long): List<ProductModifierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifier(modifier: ProductModifierEntity): Long

    // --- Floor Sections & Tables ---
    @Query("SELECT * FROM floor_sections ORDER BY sortOrder ASC")
    fun getAllFloors(): Flow<List<FloorSectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloor(floor: FloorSectionEntity): Long

    @Query("SELECT * FROM restaurant_tables ORDER BY tableNumber ASC")
    fun getAllTables(): Flow<List<RestaurantTableEntity>>

    @Query("SELECT * FROM restaurant_tables WHERE floorId = :floorId ORDER BY tableNumber ASC")
    fun getTablesByFloor(floorId: Long): Flow<List<RestaurantTableEntity>>

    @Query("SELECT * FROM restaurant_tables WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: Long): RestaurantTableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RestaurantTableEntity): Long

    @Update
    suspend fun updateTable(table: RestaurantTableEntity)

    @Query("DELETE FROM restaurant_tables WHERE id = :id")
    suspend fun deleteTable(id: Long)

    @Query("UPDATE restaurant_tables SET status = :status, currentOrderId = :orderId, currentOrderAmount = :amount, occupiedSinceTimestamp = :occupiedSince WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: Long, status: TableStatus, orderId: Long, amount: Double, occupiedSince: Long)

    // --- Orders & Items ---
    @Query("SELECT * FROM orders WHERE status != 'BILLED' AND status != 'CANCELLED' ORDER BY updatedAt DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE tableId = :tableId AND (status = 'ACTIVE' OR status = 'SENT_TO_KITCHEN' OR status = 'READY') LIMIT 1")
    suspend fun getActiveOrderByTable(tableId: Long): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsSync(orderId: Long): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity): Long

    @Update
    suspend fun updateOrderItem(item: OrderItemEntity)

    @Query("DELETE FROM order_items WHERE id = :id")
    suspend fun deleteOrderItem(id: Long)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun clearOrderItems(orderId: Long)

    // --- KOTs ---
    @Query("SELECT * FROM kots WHERE status != 'SERVED' ORDER BY priority DESC, createdAt ASC")
    fun getActiveKots(): Flow<List<KotEntity>>

    @Query("SELECT * FROM kots ORDER BY createdAt DESC LIMIT 50")
    fun getAllRecentKots(): Flow<List<KotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKot(kot: KotEntity): Long

    @Update
    suspend fun updateKot(kot: KotEntity)

    @Query("UPDATE kots SET status = :status WHERE id = :id")
    suspend fun updateKotStatus(id: Long, status: KotStatus)

    // --- Bills ---
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE createdAt >= :startTimestamp AND createdAt <= :endTimestamp ORDER BY createdAt DESC")
    fun getBillsInRange(startTimestamp: Long, endTimestamp: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getBillByInvoiceNumber(invoiceNumber: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getBillCount(): Int

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE expenseDate = :dateStr ORDER BY createdAt DESC")
    fun getExpensesByDate(dateStr: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE phone LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    // --- Ingredients & Recipes ---
    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity): Long

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Query("DELETE FROM ingredients WHERE id = :id")
    suspend fun deleteIngredient(id: Long)

    @Query("SELECT * FROM recipes WHERE productId = :productId")
    fun getRecipesForProduct(productId: Long): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE productId = :productId")
    suspend fun getRecipesForProductSync(productId: Long): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Query("DELETE FROM recipes WHERE productId = :productId")
    suspend fun deleteRecipesForProduct(productId: Long)

    // --- Suppliers & Purchases ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Query("SELECT * FROM purchases ORDER BY createdAt DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    // --- Shifts ---
    @Query("SELECT * FROM shifts WHERE userId = :userId AND isClosed = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentOpenShift(userId: Long): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    // --- Business Settings ---
    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    fun getBusinessSettings(): Flow<BusinessSettingEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1 LIMIT 1")
    suspend fun getBusinessSettingsSync(): BusinessSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBusinessSettings(settings: BusinessSettingEntity)
}
