# E-Commerce Management System

Advanced Kotlin console application demonstrating OOP, design patterns, collections, algorithms, and e-commerce business logic. Lives in the **AABrowser** repo as a standalone JVM module.

## Quick Start

From the AABrowser root:

```bash
./gradlew :ecommerce-system:run
```

Run from repo root so file persistence resolves to `ecommerce-system/data/`.

## Default Credentials

| Role     | Email                  | Password  |
|----------|------------------------|-----------|
| Admin    | admin@ecommerce.com    | admin123  |

Register a customer account from the main menu.

## Features

### User Module
- Register / Login / Logout
- Forgot password with OTP simulation
- Change password
- Wallet (recharge, pay, refund, history)
- Loyalty points (earn & redeem)
- Address book

### Product Module
- CRUD (admin)
- Search (binary search + contains)
- Sort (merge sort by price)
- Filter (category, brand, price, rating, availability)
- Category tree (DFS)

### Cart Module
- Add / remove / update quantity
- Save for later / wishlist
- Undo stack (last 10 states)
- Stock validation
- Shipping by weight

### Coupon Engine
- WELCOME100, SAVE10, FLAT500, B2G1, FESTIVAL25
- Min purchase, max discount, expiry, one-time use
- Dynamic discount (high cart / loyal customer / coupon / default)

### Payment (Strategy Pattern)
- UPI, Credit Card, Wallet, Net Banking, Cash on Delivery

### Order Module
- Place order with GST, shipping, invoice
- Cancel / return (90% refund)
- Track shipment
- Order history

### Admin
- Dashboard, inventory, reports
- Sales, revenue, GST, profit estimate
- Top/worst products, customer ranking
- Low stock alerts, monthly sales

## Architecture

```
ecommerce-system/
├── Main.kt
├── models/          # 15+ data/domain classes
├── services/        # 9 service classes
├── repository/      # Singleton repositories
├── patterns/        # Strategy, Factory, Observer, Builder
├── utils/           # Validators, algorithms, invoice generator
└── data/            # File persistence
```

## Kotlin Concepts Demonstrated

- OOP (inheritance, polymorphism, encapsulation, abstraction)
- Data classes, sealed classes, enums
- Singleton (lazy repositories)
- Factory & Strategy & Observer patterns
- Builder pattern (InvoiceBuilder)
- Extension functions, scope functions
- Higher-order functions (map, filter, groupBy, fold)
- Generics, null safety
- Exception handling (custom sealed hierarchy)
- File I/O persistence
- Collections: HashMap, HashSet, PriorityQueue, Stack, Queue

## Algorithms

| Algorithm      | Usage                    |
|----------------|--------------------------|
| Binary Search  | Product search by name   |
| Merge Sort     | Sort products by price   |
| Quick Sort     | Revenue report           |
| Priority Queue | Top selling products     |
| DFS            | Category tree traversal  |

## Business Rules

- GST: Electronics 18%, Fashion 12%, Food 5%, Books 0%, Medicine 5%
- Shipping: ₹50–₹500 by weight
- Loyalty: 1 point per ₹100; 100 points = ₹100 cashback
- Account lock after 3 failed login attempts
- COD limit: ₹50,000
- Inventory: reserve → pay → deduct → low stock alert

## Build

```bash
./gradlew :ecommerce-system:build
```
