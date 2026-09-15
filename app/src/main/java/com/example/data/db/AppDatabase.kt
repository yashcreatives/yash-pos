package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.PosDao
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        ProductModifierEntity::class,
        IngredientEntity::class,
        RecipeEntity::class,
        FloorSectionEntity::class,
        RestaurantTableEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        KotEntity::class,
        BillEntity::class,
        ExpenseEntity::class,
        ShiftEntity::class,
        PurchaseEntity::class,
        SupplierEntity::class,
        AuditLogEntity::class,
        BusinessSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yash_pos_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateSeedData(database.posDao())
                    }
                }
            }
        }

        suspend fun populateSeedData(dao: PosDao) {
            // 1. Users
            dao.insertUser(
                UserEntity(
                    id = 1,
                    username = "owner",
                    pin = "1111",
                    passwordHash = "owner123",
                    fullName = "Yash Demo",
                    role = UserRole.OWNER,
                    mobile = "9876543210",
                    email = "owner@yashpos.com",
                    maxDiscountPercent = 100.0
                )
            )
            dao.insertUser(
                UserEntity(
                    id = 2,
                    username = "manager",
                    pin = "2222",
                    passwordHash = "manager123",
                    fullName = "Rahul Sharma",
                    role = UserRole.MANAGER,
                    mobile = "9876543211",
                    email = "manager@yashpos.com",
                    maxDiscountPercent = 30.0
                )
            )
            dao.insertUser(
                UserEntity(
                    id = 3,
                    username = "cashier",
                    pin = "3333",
                    passwordHash = "cashier123",
                    fullName = "Priya Patel",
                    role = UserRole.CASHIER,
                    mobile = "9876543212",
                    email = "cashier@yashpos.com",
                    maxDiscountPercent = 10.0
                )
            )
            dao.insertUser(
                UserEntity(
                    id = 4,
                    username = "admin",
                    pin = "4444",
                    passwordHash = "admin123",
                    fullName = "System Admin",
                    role = UserRole.ADMIN,
                    mobile = "9876543213",
                    email = "admin@yashpos.com",
                    maxDiscountPercent = 100.0
                )
            )

            // 2. Business Settings
            dao.updateBusinessSettings(
                BusinessSettingEntity(
                    id = 1,
                    restaurantName = "Yash Food Palace & Bar",
                    tagline = "Premium Multi-Cuisine Dine-in & POS",
                    address = "Plot 42, Metro Commercial Hub, Sector 18",
                    phone = "+91 98765 43210",
                    email = "info@yashfoodpalace.com",
                    gstNumber = "27ABCDE1234F1Z5",
                    fssaiNumber = "11522001000987",
                    invoicePrefix = "YASH",
                    defaultTaxPercent = 5.0,
                    enableServiceCharge = false,
                    serviceChargePercent = 5.0,
                    currencySymbol = "₹",
                    branchName = "Main Branch (Flagship)"
                )
            )

            // 3. Floors
            val groundFloorId = dao.insertFloor(FloorSectionEntity(id = 1, name = "Ground Floor", sortOrder = 1))
            val acHallId = dao.insertFloor(FloorSectionEntity(id = 2, name = "AC Hall", sortOrder = 2))
            val outdoorId = dao.insertFloor(FloorSectionEntity(id = 3, name = "Outdoor Garden", sortOrder = 3))
            val familyId = dao.insertFloor(FloorSectionEntity(id = 4, name = "Family Lounge", sortOrder = 4))

            // 4. Tables
            dao.insertTable(RestaurantTableEntity(id = 1, tableNumber = "T-01", capacity = 4, floorId = groundFloorId, status = TableStatus.AVAILABLE))
            dao.insertTable(RestaurantTableEntity(id = 2, tableNumber = "T-02", capacity = 2, floorId = groundFloorId, status = TableStatus.AVAILABLE))
            dao.insertTable(RestaurantTableEntity(id = 3, tableNumber = "T-03", capacity = 4, floorId = groundFloorId, status = TableStatus.OCCUPIED, currentOrderAmount = 680.0, occupiedSinceTimestamp = System.currentTimeMillis() - 25 * 60 * 1000, guestCount = 3))
            dao.insertTable(RestaurantTableEntity(id = 4, tableNumber = "T-04", capacity = 6, floorId = groundFloorId, status = TableStatus.WAITING_FOR_BILL, currentOrderAmount = 1420.0, occupiedSinceTimestamp = System.currentTimeMillis() - 45 * 60 * 1000, guestCount = 5))
            dao.insertTable(RestaurantTableEntity(id = 5, tableNumber = "T-05", capacity = 8, floorId = groundFloorId, status = TableStatus.AVAILABLE))

            dao.insertTable(RestaurantTableEntity(id = 6, tableNumber = "AC-01", capacity = 4, floorId = acHallId, status = TableStatus.AVAILABLE))
            dao.insertTable(RestaurantTableEntity(id = 7, tableNumber = "AC-02", capacity = 4, floorId = acHallId, status = TableStatus.RESERVED, reservedCustomerName = "Mr. Verma", reservedCustomerPhone = "9822012345", reservedTime = "08:00 PM"))
            dao.insertTable(RestaurantTableEntity(id = 8, tableNumber = "AC-03", capacity = 6, floorId = acHallId, status = TableStatus.OCCUPIED, currentOrderAmount = 950.0, occupiedSinceTimestamp = System.currentTimeMillis() - 15 * 60 * 1000, guestCount = 4))
            dao.insertTable(RestaurantTableEntity(id = 9, tableNumber = "AC-04", capacity = 2, floorId = acHallId, status = TableStatus.CLEANING))

            dao.insertTable(RestaurantTableEntity(id = 10, tableNumber = "OUT-01", capacity = 4, floorId = outdoorId, status = TableStatus.AVAILABLE))
            dao.insertTable(RestaurantTableEntity(id = 11, tableNumber = "OUT-02", capacity = 6, floorId = outdoorId, status = TableStatus.AVAILABLE))
            dao.insertTable(RestaurantTableEntity(id = 12, tableNumber = "OUT-03", capacity = 4, floorId = outdoorId, status = TableStatus.AVAILABLE))

            dao.insertTable(RestaurantTableEntity(id = 13, tableNumber = "FAM-01", capacity = 10, floorId = familyId, status = TableStatus.AVAILABLE))
            dao.insertTable(RestaurantTableEntity(id = 14, tableNumber = "FAM-02", capacity = 8, floorId = familyId, status = TableStatus.AVAILABLE))

            // 5. Categories
            val catStarters = dao.insertCategory(CategoryEntity(id = 1, name = "Starters", iconName = "tapas", colorHex = "#EA580C", sortOrder = 1))
            val catMainCourse = dao.insertCategory(CategoryEntity(id = 2, name = "Main Course", iconName = "dinner_dining", colorHex = "#D97706", sortOrder = 2))
            val catBiryani = dao.insertCategory(CategoryEntity(id = 3, name = "Biryani & Rice", iconName = "rice_bowl", colorHex = "#CA8A04", sortOrder = 3))
            val catPizzaBurger = dao.insertCategory(CategoryEntity(id = 4, name = "Pizza & Burgers", iconName = "local_pizza", colorHex = "#E11D48", sortOrder = 4))
            val catChinese = dao.insertCategory(CategoryEntity(id = 5, name = "Chinese & Fast Food", iconName = "ramen_dining", colorHex = "#9333EA", sortOrder = 5))
            val catBeverages = dao.insertCategory(CategoryEntity(id = 6, name = "Beverages & Shakes", iconName = "local_cafe", colorHex = "#0284C7", sortOrder = 6))
            val catDesserts = dao.insertCategory(CategoryEntity(id = 7, name = "Desserts & Ice Cream", iconName = "icecream", colorHex = "#059669", sortOrder = 7))

            // 6. Products
            // Starters
            val p1 = dao.insertProduct(ProductEntity(id = 1, name = "Paneer Tikka", shortName = "Pan Tikka", code = "ST01", categoryId = catStarters, price = 240.0, isVeg = true, isBestseller = true, prepTimeMinutes = 15, trackStock = true, currentStock = 35.0, minStockAlert = 5.0))
            val p2 = dao.insertProduct(ProductEntity(id = 2, name = "Chicken 65", shortName = "Chkn 65", code = "ST02", categoryId = catStarters, price = 280.0, isVeg = false, isBestseller = true, prepTimeMinutes = 12, trackStock = true, currentStock = 28.0, minStockAlert = 5.0))
            val p3 = dao.insertProduct(ProductEntity(id = 3, name = "Crispy Corn", shortName = "Crisp Corn", code = "ST03", categoryId = catStarters, price = 180.0, isVeg = true, prepTimeMinutes = 10, trackStock = true, currentStock = 40.0, minStockAlert = 5.0))
            val p4 = dao.insertProduct(ProductEntity(id = 4, name = "Mutton Seekh Kebab", shortName = "Mut Seekh", code = "ST04", categoryId = catStarters, price = 360.0, isVeg = false, prepTimeMinutes = 18, trackStock = true, currentStock = 15.0, minStockAlert = 4.0))

            // Main Course
            val p5 = dao.insertProduct(ProductEntity(id = 5, name = "Butter Chicken", shortName = "Butr Chkn", code = "MC01", categoryId = catMainCourse, price = 340.0, isVeg = false, isBestseller = true, prepTimeMinutes = 20, trackStock = true, currentStock = 25.0, minStockAlert = 5.0))
            val p6 = dao.insertProduct(ProductEntity(id = 6, name = "Paneer Butter Masala", shortName = "PBM", code = "MC02", categoryId = catMainCourse, price = 260.0, isVeg = true, isBestseller = true, prepTimeMinutes = 18, trackStock = true, currentStock = 30.0, minStockAlert = 5.0))
            val p7 = dao.insertProduct(ProductEntity(id = 7, name = "Dal Makhani", shortName = "Dal Mak", code = "MC03", categoryId = catMainCourse, price = 210.0, isVeg = true, prepTimeMinutes = 15, trackStock = true, currentStock = 45.0, minStockAlert = 8.0))
            val p8 = dao.insertProduct(ProductEntity(id = 8, name = "Butter Naan", shortName = "Naan", code = "MC04", categoryId = catMainCourse, price = 45.0, isVeg = true, prepTimeMinutes = 5, trackStock = false))

            // Biryani & Rice
            val p9 = dao.insertProduct(ProductEntity(id = 9, name = "Hyderabadi Chicken Biryani", shortName = "Hyd Chk Bir", code = "BR01", categoryId = catBiryani, price = 320.0, isVeg = false, isBestseller = true, prepTimeMinutes = 15, trackStock = true, currentStock = 50.0, minStockAlert = 10.0))
            val p10 = dao.insertProduct(ProductEntity(id = 10, name = "Veg Dum Biryani", shortName = "Veg Bir", code = "BR02", categoryId = catBiryani, price = 240.0, isVeg = true, prepTimeMinutes = 15, trackStock = true, currentStock = 30.0, minStockAlert = 5.0))
            val p11 = dao.insertProduct(ProductEntity(id = 11, name = "Jeera Rice", shortName = "Jeera Rice", code = "BR03", categoryId = catBiryani, price = 140.0, isVeg = true, prepTimeMinutes = 8, trackStock = true, currentStock = 60.0, minStockAlert = 10.0))

            // Pizza & Burgers
            val p12 = dao.insertProduct(ProductEntity(id = 12, name = "Farmhouse Veg Pizza", shortName = "Farm Pizza", code = "PZ01", categoryId = catPizzaBurger, price = 299.0, isVeg = true, prepTimeMinutes = 18, trackStock = true, currentStock = 20.0, minStockAlert = 5.0))
            val p13 = dao.insertProduct(ProductEntity(id = 13, name = "Chicken Supreme Pizza", shortName = "Chkn Pizza", code = "PZ02", categoryId = catPizzaBurger, price = 389.0, isVeg = false, isBestseller = true, prepTimeMinutes = 18, trackStock = true, currentStock = 18.0, minStockAlert = 4.0))
            val p14 = dao.insertProduct(ProductEntity(id = 14, name = "Classic Cheese Burger", shortName = "Chs Burger", code = "BG01", categoryId = catPizzaBurger, price = 159.0, isVeg = true, prepTimeMinutes = 10, trackStock = true, currentStock = 22.0, minStockAlert = 5.0))

            // Chinese
            val p15 = dao.insertProduct(ProductEntity(id = 15, name = "Veg Hakka Noodles", shortName = "Veg Nood", code = "CN01", categoryId = catChinese, price = 190.0, isVeg = true, prepTimeMinutes = 12, trackStock = true, currentStock = 35.0, minStockAlert = 5.0))
            val p16 = dao.insertProduct(ProductEntity(id = 16, name = "Chicken Fried Rice", shortName = "Chkn Rice", code = "CN02", categoryId = catChinese, price = 230.0, isVeg = false, isBestseller = true, prepTimeMinutes = 12, trackStock = true, currentStock = 40.0, minStockAlert = 8.0))
            val p17 = dao.insertProduct(ProductEntity(id = 17, name = "Chilli Paneer Dry", shortName = "Chilli Pan", code = "CN03", categoryId = catChinese, price = 220.0, isVeg = true, prepTimeMinutes = 12, trackStock = true, currentStock = 25.0, minStockAlert = 4.0))

            // Beverages
            val p18 = dao.insertProduct(ProductEntity(id = 18, name = "Cold Coffee with Ice Cream", shortName = "Cold Coffee", code = "BV01", categoryId = catBeverages, price = 130.0, isVeg = true, prepTimeMinutes = 5, trackStock = true, currentStock = 50.0, minStockAlert = 10.0))
            val p19 = dao.insertProduct(ProductEntity(id = 19, name = "Fresh Lime Soda", shortName = "Lime Soda", code = "BV02", categoryId = catBeverages, price = 70.0, isVeg = true, prepTimeMinutes = 4, trackStock = true, currentStock = 100.0, minStockAlert = 15.0))
            val p20 = dao.insertProduct(ProductEntity(id = 20, name = "Virgin Mojito", shortName = "Mojito", code = "BV03", categoryId = catBeverages, price = 140.0, isVeg = true, prepTimeMinutes = 6, trackStock = true, currentStock = 40.0, minStockAlert = 8.0))
            val p21 = dao.insertProduct(ProductEntity(id = 21, name = "Masala Chai", shortName = "Chai", code = "BV04", categoryId = catBeverages, price = 40.0, isVeg = true, prepTimeMinutes = 5, trackStock = false))

            // Desserts
            val p22 = dao.insertProduct(ProductEntity(id = 22, name = "Gulab Jamun with Ice Cream", shortName = "Jamun", code = "DS01", categoryId = catDesserts, price = 120.0, isVeg = true, prepTimeMinutes = 3, trackStock = true, currentStock = 45.0, minStockAlert = 8.0))
            val p23 = dao.insertProduct(ProductEntity(id = 23, name = "Sizzling Brownie", shortName = "Brownie", code = "DS02", categoryId = catDesserts, price = 180.0, isVeg = true, isBestseller = true, prepTimeMinutes = 8, trackStock = true, currentStock = 20.0, minStockAlert = 4.0))

            // 7. Product Variants
            dao.insertVariant(ProductVariantEntity(productId = p9, name = "Regular / Half", price = 220.0))
            dao.insertVariant(ProductVariantEntity(productId = p9, name = "Full", price = 320.0, isDefault = true))
            dao.insertVariant(ProductVariantEntity(productId = p9, name = "Family Handi (3-4 Pers)", price = 750.0))

            dao.insertVariant(ProductVariantEntity(productId = p12, name = "Regular 7\"", price = 199.0))
            dao.insertVariant(ProductVariantEntity(productId = p12, name = "Medium 10\"", price = 299.0, isDefault = true))
            dao.insertVariant(ProductVariantEntity(productId = p12, name = "Large 12\"", price = 449.0))

            dao.insertVariant(ProductVariantEntity(productId = p13, name = "Regular 7\"", price = 269.0))
            dao.insertVariant(ProductVariantEntity(productId = p13, name = "Medium 10\"", price = 389.0, isDefault = true))
            dao.insertVariant(ProductVariantEntity(productId = p13, name = "Large 12\"", price = 549.0))

            // 8. Modifiers
            dao.insertModifier(ProductModifierEntity(productId = 0, name = "Extra Cheese", price = 40.0))
            dao.insertModifier(ProductModifierEntity(productId = 0, name = "Extra Spicy 🌶️", price = 0.0))
            dao.insertModifier(ProductModifierEntity(productId = 0, name = "Less Spicy / Mild", price = 0.0))
            dao.insertModifier(ProductModifierEntity(productId = 0, name = "Extra Gravy / Sauce", price = 30.0))
            dao.insertModifier(ProductModifierEntity(productId = 0, name = "Extra Egg", price = 20.0))
            dao.insertModifier(ProductModifierEntity(productId = 0, name = "No Onion / Garlic (Jain)", price = 0.0))

            // 9. Ingredients & Recipes
            val ingRice = dao.insertIngredient(IngredientEntity(id = 1, name = "Basmati Rice", currentStock = 45.0, minStockAlert = 10.0, unit = "Kg", costPerUnit = 90.0, supplierName = "Shree Grain Mills"))
            val ingChicken = dao.insertIngredient(IngredientEntity(id = 2, name = "Fresh Chicken", currentStock = 28.0, minStockAlert = 8.0, unit = "Kg", costPerUnit = 220.0, supplierName = "Prime Poultry"))
            val ingPaneer = dao.insertIngredient(IngredientEntity(id = 3, name = "Fresh Malai Paneer", currentStock = 12.0, minStockAlert = 4.0, unit = "Kg", costPerUnit = 320.0, supplierName = "Dairy Fresh Ltd"))
            val ingCheese = dao.insertIngredient(IngredientEntity(id = 4, name = "Mozzarella Cheese", currentStock = 8.5, minStockAlert = 3.0, unit = "Kg", costPerUnit = 450.0, supplierName = "Dairy Fresh Ltd"))
            val ingOil = dao.insertIngredient(IngredientEntity(id = 5, name = "Refined Sunflower Oil", currentStock = 35.0, minStockAlert = 10.0, unit = "Ltr", costPerUnit = 135.0, supplierName = "Shree Grain Mills"))

            // Chicken Biryani Recipe
            dao.insertRecipe(RecipeEntity(productId = p9, ingredientId = ingRice, ingredientName = "Basmati Rice", quantityNeeded = 0.25, unit = "Kg"))
            dao.insertRecipe(RecipeEntity(productId = p9, ingredientId = ingChicken, ingredientName = "Fresh Chicken", quantityNeeded = 0.20, unit = "Kg"))
            dao.insertRecipe(RecipeEntity(productId = p9, ingredientId = ingOil, ingredientName = "Refined Sunflower Oil", quantityNeeded = 0.03, unit = "Ltr"))

            // Chicken Fried Rice Recipe
            dao.insertRecipe(RecipeEntity(productId = p16, ingredientId = ingRice, ingredientName = "Basmati Rice", quantityNeeded = 0.20, unit = "Kg"))
            dao.insertRecipe(RecipeEntity(productId = p16, ingredientId = ingChicken, ingredientName = "Fresh Chicken", quantityNeeded = 0.10, unit = "Kg"))
            dao.insertRecipe(RecipeEntity(productId = p16, ingredientId = ingOil, ingredientName = "Refined Sunflower Oil", quantityNeeded = 0.02, unit = "Ltr"))

            // 10. Suppliers & Purchases
            dao.insertSupplier(SupplierEntity(id = 1, name = "Prime Poultry Suppliers", phone = "9811223344", email = "orders@primepoultry.com", address = "Shop 12, Meat Market", totalPurchases = 45000.0, outstandingDue = 5200.0))
            dao.insertSupplier(SupplierEntity(id = 2, name = "Dairy Fresh Agro Ltd", phone = "9822334455", email = "sales@dairyfresh.com", address = "Industrial Estate, Unit 4", totalPurchases = 32000.0, outstandingDue = 0.0))
            dao.insertSupplier(SupplierEntity(id = 3, name = "Shree Grain Mills & Spices", phone = "9833445566", email = "shreegrain@gmail.com", address = "Wholesale Mandi Yard", totalPurchases = 58000.0, outstandingDue = 8500.0))

            dao.insertPurchase(
                PurchaseEntity(
                    id = 1,
                    invoiceNumber = "PUR-2026-001",
                    supplierName = "Prime Poultry Suppliers",
                    supplierPhone = "9811223344",
                    purchaseDate = "2026-09-14",
                    totalAmount = 8800.0,
                    paidAmount = 5000.0,
                    dueAmount = 3800.0,
                    itemsSummary = "Fresh Chicken 40kg @ ₹220",
                    notes = "Delivered morning batch"
                )
            )

            // 11. Customers
            dao.insertCustomer(
                CustomerEntity(
                    id = 1,
                    name = "Vikram Aditya",
                    phone = "9876501234",
                    address = "Flat 402, Sunshine Heights",
                    birthday = "1992-10-15",
                    loyaltyPoints = 350,
                    totalOrders = 14,
                    totalSpending = 6850.0,
                    lastVisitTimestamp = System.currentTimeMillis() - 24 * 3600 * 1000
                )
            )
            dao.insertCustomer(
                CustomerEntity(
                    id = 2,
                    name = "Ananya Sen",
                    phone = "9876505678",
                    address = "Villa 18, Palm Meadows",
                    birthday = "1995-04-20",
                    loyaltyPoints = 520,
                    totalOrders = 22,
                    totalSpending = 11200.0,
                    lastVisitTimestamp = System.currentTimeMillis() - 48 * 3600 * 1000
                )
            )
            dao.insertCustomer(
                CustomerEntity(
                    id = 3,
                    name = "Rohan Malhotra",
                    phone = "9876509988",
                    address = "A-201, Green Valley",
                    birthday = "1988-12-05",
                    creditBalance = 450.0,
                    loyaltyPoints = 120,
                    totalOrders = 6,
                    totalSpending = 3400.0,
                    lastVisitTimestamp = System.currentTimeMillis() - 72 * 3600 * 1000
                )
            )

            // 12. Sample Expenses
            dao.insertExpense(
                ExpenseEntity(
                    id = 1,
                    expenseDate = "2026-09-15",
                    category = "Vegetables",
                    amount = 1450.0,
                    paymentMethod = PaymentMethod.CASH,
                    description = "Fresh vegetables from morning mandi market",
                    employeeName = "Rahul Sharma",
                    referenceNumber = "EXP-0915-1"
                )
            )
            dao.insertExpense(
                ExpenseEntity(
                    id = 2,
                    expenseDate = "2026-09-15",
                    category = "Gas",
                    amount = 2100.0,
                    paymentMethod = PaymentMethod.UPI,
                    description = "Commercial LPG Cylinder Refill (19kg)",
                    employeeName = "Yash Demo",
                    referenceNumber = "EXP-0915-2"
                )
            )

            // 13. Sample Past Bills for instant reporting & charts
            val now = System.currentTimeMillis()
            dao.insertBill(
                BillEntity(
                    id = 1,
                    invoiceNumber = "YASH-1001",
                    orderId = 101,
                    orderType = OrderType.DINE_IN,
                    tableName = "T-01",
                    customerName = "Vikram Aditya",
                    customerPhone = "9876501234",
                    cashierName = "Priya Patel",
                    itemsSummary = "Hyderabadi Chicken Biryani x 2, Virgin Mojito x 2",
                    subtotal = 920.0,
                    discountAmount = 46.0,
                    taxAmount = 43.7,
                    grandTotal = 917.7,
                    paidAmount = 917.7,
                    paymentMethod = PaymentMethod.UPI,
                    createdAt = now - 4 * 3600 * 1000
                )
            )
            dao.insertBill(
                BillEntity(
                    id = 2,
                    invoiceNumber = "YASH-1002",
                    orderId = 102,
                    orderType = OrderType.TAKEAWAY,
                    tableName = "",
                    customerName = "Ananya Sen",
                    customerPhone = "9876505678",
                    cashierName = "Priya Patel",
                    itemsSummary = "Butter Chicken x 1, Butter Naan x 4, Jeera Rice x 1",
                    subtotal = 660.0,
                    discountAmount = 0.0,
                    taxAmount = 33.0,
                    grandTotal = 693.0,
                    paidAmount = 693.0,
                    paymentMethod = PaymentMethod.CASH,
                    createdAt = now - 3 * 3600 * 1000
                )
            )
            dao.insertBill(
                BillEntity(
                    id = 3,
                    invoiceNumber = "YASH-1003",
                    orderId = 103,
                    orderType = OrderType.DINE_IN,
                    tableName = "AC-02",
                    customerName = "Walk-in Guest",
                    cashierName = "Rahul Sharma",
                    itemsSummary = "Chicken Supreme Pizza x 1, Cold Coffee x 2, Sizzling Brownie x 1",
                    subtotal = 829.0,
                    discountAmount = 50.0,
                    taxAmount = 38.95,
                    grandTotal = 817.95,
                    paidAmount = 817.95,
                    paymentMethod = PaymentMethod.CARD,
                    createdAt = now - 1 * 3600 * 1000
                )
            )

            // 14. Sample KOTs for KDS
            dao.insertKot(
                KotEntity(
                    id = 1,
                    kotNumber = "KOT-081",
                    orderId = 201,
                    tableName = "T-03",
                    orderType = OrderType.DINE_IN,
                    itemsSummary = "Paneer Butter Masala x 1 (Less Spicy), Butter Naan x 4, Masala Chai x 2",
                    notes = "Serve naan hot immediately",
                    priority = KotPriority.NORMAL,
                    status = KotStatus.PREPARING,
                    createdAt = now - 18 * 60 * 1000
                )
            )
            dao.insertKot(
                KotEntity(
                    id = 2,
                    kotNumber = "KOT-082",
                    orderId = 202,
                    tableName = "AC-03",
                    orderType = OrderType.DINE_IN,
                    itemsSummary = "Hyderabadi Chicken Biryani x 2, Chicken 65 x 1 (Extra Spicy 🌶️)",
                    notes = "Extra raita on side",
                    priority = KotPriority.HIGH,
                    status = KotStatus.NEW,
                    createdAt = now - 6 * 60 * 1000
                )
            )

            // 15. Audit Log
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = 1,
                    userName = "Yash Demo",
                    role = "OWNER",
                    action = "SYSTEM_INITIALIZED",
                    details = "POS System initial startup and database seed completed."
                )
            )
        }
    }
}
