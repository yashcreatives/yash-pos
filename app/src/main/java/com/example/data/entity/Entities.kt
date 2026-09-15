package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    OWNER,
    MANAGER,
    CASHIER,
    ADMIN
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val pin: String, // 4-digit PIN
    val passwordHash: String,
    val fullName: String,
    val role: UserRole,
    val mobile: String = "",
    val email: String = "",
    val isActive: Boolean = true,
    val maxDiscountPercent: Double = if (role == UserRole.OWNER || role == UserRole.ADMIN) 100.0 else if (role == UserRole.MANAGER) 30.0 else 10.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "restaurant",
    val colorHex: String = "#EA580C",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shortName: String = "",
    val code: String = "",
    val barcode: String = "",
    val categoryId: Long,
    val price: Double,
    val purchasePrice: Double = 0.0,
    val discountPrice: Double = 0.0,
    val taxPercent: Double = 5.0,
    val isTaxInclusive: Boolean = true,
    val isVeg: Boolean = true,
    val isBestseller: Boolean = false,
    val isFeatured: Boolean = false,
    val isTodaySpecial: Boolean = false,
    val isDineInAvailable: Boolean = true,
    val isTakeawayAvailable: Boolean = true,
    val isDeliveryAvailable: Boolean = true,
    val isAvailable: Boolean = true, // Menu availability toggle
    val trackStock: Boolean = false,
    val currentStock: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val unit: String = "Pcs", // Pcs, Kg, Gms, Portions, Cups
    val supplierId: Long = 0L,
    val prepTimeMinutes: Int = 15,
    val kitchenStation: String = "Main Kitchen",
    val description: String = "",
    val imageResName: String = ""
)

@Entity(tableName = "product_variants")
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val name: String, // Regular, Small, Medium, Large, Half, Full, Family
    val price: Double,
    val isDefault: Boolean = false
)

@Entity(tableName = "product_modifiers")
data class ProductModifierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long = 0, // 0 for global modifiers
    val name: String, // Extra Cheese, Extra Sauce, Spicy, Less Spicy, Extra Chicken, Extra Egg
    val price: Double = 0.0,
    val isAvailable: Boolean = true
)

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentStock: Double,
    val minStockAlert: Double = 2.0,
    val unit: String = "Kg", // Kg, Gms, Ltr, Ml, Pcs
    val costPerUnit: Double = 0.0,
    val supplierName: String = ""
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val ingredientId: Long,
    val ingredientName: String,
    val quantityNeeded: Double,
    val unit: String
)

@Entity(tableName = "floor_sections")
data class FloorSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // Ground Floor, First Floor, AC Hall, Outdoor, Family Section
    val sortOrder: Int = 0
)

enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    WAITING_FOR_BILL,
    PAYMENT_PENDING,
    COMPLETED,
    CLEANING
}

@Entity(tableName = "restaurant_tables")
data class RestaurantTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableNumber: String,
    val capacity: Int = 4,
    val floorId: Long = 1L,
    val status: TableStatus = TableStatus.AVAILABLE,
    val currentOrderId: Long = 0L,
    val currentOrderAmount: Double = 0.0,
    val occupiedSinceTimestamp: Long = 0L,
    val guestCount: Int = 0,
    val reservedCustomerName: String = "",
    val reservedCustomerPhone: String = "",
    val reservedTime: String = ""
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val landmark: String = "",
    val birthday: String = "",
    val notes: String = "",
    val loyaltyPoints: Int = 0,
    val creditBalance: Double = 0.0, // Outstanding amount owed
    val totalOrders: Int = 0,
    val totalSpending: Double = 0.0,
    val lastVisitTimestamp: Long = 0L
)

enum class OrderType {
    DINE_IN,
    TAKEAWAY,
    DELIVERY,
    PARCEL,
    PRE_ORDER
}

enum class OrderStatus {
    ACTIVE,
    SENT_TO_KITCHEN,
    READY,
    BILLED,
    CANCELLED
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val orderType: OrderType = OrderType.DINE_IN,
    val tableId: Long = 0L,
    val tableName: String = "",
    val customerId: Long = 0L,
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val deliveryCharge: Double = 0.0,
    val status: OrderStatus = OrderStatus.ACTIVE,
    val subtotal: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val grandTotal: Double = 0.0,
    val notes: String = "",
    val cashierId: Long = 0L,
    val cashierName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val variantName: String = "",
    val modifiersJson: String = "", // comma-separated modifier names
    val quantity: Int = 1,
    val unitPrice: Double,
    val rate: Double,
    val taxPercent: Double = 5.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalPrice: Double,
    val isComplimentary: Boolean = false,
    val notes: String = "",
    val isKotSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class KotStatus {
    NEW,
    ACCEPTED,
    PREPARING,
    READY,
    SERVED
}

enum class KotPriority {
    NORMAL,
    HIGH,
    URGENT
}

@Entity(tableName = "kots")
data class KotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kotNumber: String,
    val orderId: Long,
    val tableName: String,
    val orderType: OrderType,
    val itemsSummary: String, // Stringified list of items
    val notes: String = "",
    val priority: KotPriority = KotPriority.NORMAL,
    val status: KotStatus = KotStatus.NEW,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long = 0L,
    val readyAt: Long = 0L
)

enum class PaymentMethod {
    CASH,
    UPI,
    CARD,
    CREDIT,
    SPLIT
}

enum class BillStatus {
    COMPLETED,
    REFUNDED,
    CANCELLED,
    VOIDED
}

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val orderId: Long,
    val orderType: OrderType,
    val tableName: String = "",
    val customerId: Long = 0L,
    val customerName: String = "",
    val customerPhone: String = "",
    val cashierId: Long = 0L,
    val cashierName: String = "",
    val itemsSummary: String = "",
    val subtotal: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val discountReason: String = "",
    val taxAmount: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val grandTotal: Double,
    val paidAmount: Double,
    val dueAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val splitDetails: String = "", // e.g., "Cash: 500, UPI: 300"
    val status: BillStatus = BillStatus.COMPLETED,
    val cancellationReason: String = "",
    val refundAmount: Double = 0.0,
    val authorizedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseDate: String, // YYYY-MM-DD
    val category: String, // Vegetables, Meat, Milk, Gas, Electricity, Rent, Salaries, Transportation, Packaging, Cleaning, Maintenance, Miscellaneous
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val description: String = "",
    val employeeName: String = "",
    val referenceNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val role: UserRole,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val openingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val upiSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalRefunds: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val isClosed: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val supplierName: String,
    val supplierPhone: String = "",
    val purchaseDate: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double = 0.0,
    val itemsSummary: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val outstandingDue: Double = 0.0
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val role: String,
    val action: String, // LOGIN, LOGOUT, BILL_CREATED, BILL_CANCELLED, BILL_REFUNDED, DISCOUNT_APPLIED, ITEM_CREATED, ITEM_EDITED, PRICE_CHANGED, EXPENSE_ADDED, STOCK_ADJUSTED, USER_CREATED, PERMISSION_CHANGED, BACKUP_CREATED, BACKUP_RESTORED
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "business_settings")
data class BusinessSettingEntity(
    @PrimaryKey val id: Long = 1L,
    val restaurantName: String = "Yash Kitchen & Café",
    val tagline: String = "Fresh Food & Great Taste",
    val address: String = "102 Gourmet Boulevard, Food City",
    val phone: String = "+91 98765 43210",
    val email: String = "contact@yashpos.com",
    val gstNumber: String = "27AABCT1234F1Z5",
    val fssaiNumber: String = "11521000000123",
    val invoicePrefix: String = "INV",
    val defaultTaxPercent: Double = 5.0,
    val enableServiceCharge: Boolean = false,
    val serviceChargePercent: Double = 5.0,
    val currencySymbol: String = "₹",
    val autoLockMinutes: Int = 15,
    val thermalPaperWidthMm: Int = 80,
    val printKitchenKot: Boolean = true,
    val soundAlerts: Boolean = true,
    val branchName: String = "Main Branch"
)
