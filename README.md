# 🍽️ Flocko — Smart Food Delivery Platform

## Overview
Flocko is a full-stack food delivery platform built with a strong focus on backend mastery.
Designed and implemented a demand-aware food ordering system with subscription automation, threshold-based deal activation, and dynamic pricing engine. 

Built with Spring Boot, PostgreSQL JWT authentication, Razorpay payment integration, and a React frontend.

---

## Tech Stack

**Backend**
- Java 17+ with Spring Boot
- Spring Security with JWT
- PostgreSQL + JPA/Hibernate
- Lombok for boilerplate reduction
- Maven for dependency management
- Razorpay Payment Integration

**Frontend**
- React + Vite
- React Router DOM

---

## Roles
- `USER` — Customer
- `RESTAURANT_OWNER` — Manages restaurant, menu, and orders
- `RESTAURANT_STAFF` — Handles order operations
- `ADMIN` — Platform-level control

---

## Current Progress
✅ **Authentication & Authorization** — JWT-based auth with role-based access control
✅ **User Management** — Signup, login, role change requests with admin approval workflow
✅ **Restaurant Management** — CRUD operations with owner verification and status management
✅ **Category Management** — Menu categories with owner-only access and proper validation
✅ **Addon Management** — AddonCategory + Addons
✅ **Food/Menu Management** — CRUD for food items
✅ **Cart System** — Add, update, remove items
✅ **Order Management** — Order placement with lifecycle tracking

---

## Core Order Lifecycle
```
PLACED → ACCEPTED → PREPARING → READY → DELIVERED
```

---

## 🚀 Unique Features (Flocko's Identity)

### 1. Mess Subscription System
Users subscribe to a restaurant's daily meal plan on a weekly or monthly basis.
The system auto-generates orders on scheduled days without manual ordering each time.
Restaurant gets guaranteed revenue and predictable demand planning.

### 2. Shared Group Cart
One user creates a cart and shares it via a link or in-app invite to friends or colleagues.
Everyone adds their own items and the cart clearly shows who added what.
Supports two split modes — equal split (total ÷ users) or pay-per-your-orders.

### 3. Group Buying / Bulk Deal Unlock
A restaurant sets a deal that activates only when X users order a dish within a time window.
Once the threshold is hit, a 30-minute confirmation window opens for all participants.
After the window closes, the deal locks, everyone is committed, and the discount is applied.

**States:** `VOTING → THRESHOLD_HIT → CONFIRMATION_WINDOW → LOCKED → FULFILLED`

### 4. Chef Chat for Special Dishes
Customers can open a 1-on-1 polling-based chat with restaurant staff for dishes marked as "Special."
Customer can describe customizations or preferences, chef can clarify before preparing.
Chat is order-scoped and automatically closes once the order is delivered.

### 5. Kitchen Load Indicator
System auto-calculates a restaurant's current load based on number of active orders in pipeline.
Drives ETA adjustments and feeds data into the Dynamic Pricing engine.
Restaurant staff can also manually flag kitchen as overwhelmed.

### 6. Dynamic Pricing Engine
Dish prices fluctuate automatically based on kitchen load, time of day, and current demand.
High kitchen load increases prices slightly; low demand near closing time triggers discounts.
Fully automated — no manual intervention needed from restaurant owner.

### 7. Pre-Order with Ingredient Reservation + Cancellation Penalty
Users schedule an order in advance and the system reserves ingredients for that time slot.
Cancellation is free if done more than 2 hours before the scheduled time.
Late cancellations (under 2 hours) result in a 20% penalty applied on the user's next order.

---

## Payment
- Integrated with **Razorpay** for real payment processing
- Webhook handling for payment verification
- Mocked flow available for development/testing

---

## Project Goal
By the end of this project, demonstrate a deep understanding of how a real-world backend is:
- Planned and architected
- Designed with clean, scalable patterns
- Implemented with proper auth, business logic, and database modeling
- Not just CRUD — but a product with real business value

---

## Notes & Learnings
See `notes/failures-log.md` for documented mistakes and lessons learned throughout the project.
