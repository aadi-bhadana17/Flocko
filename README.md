
<p align="center">
  <img src="assets/logo.png" alt="Flocko Logo" width="180">
</p>

# 🍽️ Flocko — Smart Food Delivery Platform

## What Is Flocko?

Flocko is a backend-first food delivery platform, built to demonstrate how a real-world
ordering system is designed — not just CRUD endpoints wrapped around a database.

At its core it does what any food delivery app does: browse restaurants, build a cart,
place an order, track it through to delivery. What makes it worth a second look is the
layer on top of that core — a set of interconnected systems (subscriptions, group deals,
dynamic pricing, kitchen load) that all read from and write to the same order pipeline,
forcing real decisions around transaction boundaries, module ownership, and concurrency.

The project is deliberately scoped for a single Spring Boot service (~10,000 users,
~1,000 orders/day) rather than a distributed system — the goal is depth in one service,
not breadth across many.

---

## 🛠️ Tech Stack

**Backend**
- Java 17+ with Spring Boot 3.4.5
- Spring Security with JWT (jjwt)
- PostgreSQL + JPA/Hibernate
- Lombok for boilerplate reduction
- Maven for dependency management
- PayPal Checkout SDK (sandbox, wallet deposit flow)
- Spring Scheduler for mess subscriptions, pre-orders, and group deal lifecycle

**Frontend**
- React + Vite
- React Router DOM

---

## 🏗️ Architecture

Flocko is a **modular monolith**. The codebase is split into domain modules —
`identity`, `catalog`, `ordering`, `deals`, `wallet`, `chat` — each owning its own
entities, repositories, and services. Modules don't reach into each other's persistence
layer directly; cross-module reads and writes go through facade interfaces
(`UserFacade`, `CatalogFacade`, `OrderingFacade`). Shared enums that don't belong to any
single domain live in a `common` shared kernel instead of being awkwardly owned by one
module and imported by others.

This gets most of the benefit people reach for microservices for — clear ownership
boundaries, no tangled cross-module entity graphs — without the operational cost of
distributed transactions, service discovery, or network calls between services that
don't need them at this scale.

**Why this matters for a single-service app:** the facade boundary is what makes it
possible to reason about a transaction. When `SharedCartService` needs to deduct a
user's wallet balance, it doesn't touch the `User` entity directly — it calls
`UserFacade.deductWalletBalance()`, so the wallet mutation stays inside `identity`'s
own transactional and locking logic no matter who calls it.

---

## 👥 Roles

- `CUSTOMER` — Places orders, manages cart, subscribes to mess plans, joins group deals
- `RESTAURANT_OWNER` — Manages restaurant, menu, staff, and deals
- `RESTAURANT_STAFF` — Handles order operations for their assigned restaurant
- `ADMIN` — Platform-level control: user restriction/blocking, restaurant suspension, role change approvals

Role changes aren't self-service — a user requests a role change (e.g. CUSTOMER →
RESTAURANT_OWNER), and an admin approves or rejects it. This models a real onboarding
gate rather than trusting client-supplied roles.

---

## 🔄 Core Order Lifecycle

```
CREATED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
                ↘ CANCELLED (only from CREATED or CONFIRMED)
```

Transitions are validated centrally (`OrderStatus.canTransitionTo`) rather than scattered
across service methods, so an order can't jump from `CREATED` straight to `DELIVERED` or
be cancelled once it's `PREPARING`.

---

## 🔍 Feature Deep-Dive

### 1. 📅 Mess Subscription System
Users subscribe to a restaurant's weekly meal plan (breakfast/lunch/snacks/dinner slots
per day of the week). A scheduler runs at each meal time, finds active subscriptions
matching that day and meal type, and auto-generates an order — no manual ordering needed
for the length of the subscription. Gives the restaurant predictable, recurring demand.

### 2. 🛒 Shared Group Cart
One user hosts a shared cart tied to a restaurant and a join code. Each member gets their
own individual cart, linked to the shared cart, so the system always knows who added what.
Settlement supports two modes: the host pays the full total, or each member contributes
their share from their own wallet before checkout is allowed to proceed.

### 3. 🎯 Group Buying / Bulk Deal Unlock
A restaurant configures a deal — a target participation count, a discount tier list, and
a voting window. Customers "vote in" with a quantity. A scheduler checks deals whose voting
window has closed: if at least 50% of the target participation was reached, the deal moves
into a 30-minute confirmation window; otherwise it expires and everyone already in gets
refunded. If the confirmation window closes with the threshold still met, orders are placed
for every confirmed participant at the price tier they collectively unlocked.

**States:** `CREATED → VOTING → CONFIRMATION_WINDOW → FULFILLED / EXPIRED`

### 4. 💬 Chef Chat for Special Dishes
Orders can be marked "Special" at placement. For those orders only, the customer and
restaurant staff can exchange order-scoped chat messages — useful for dietary tweaks or
prep instructions that don't fit a structured form. The thread closes automatically once
the order is delivered or cancelled, so it can't be used as a general support channel.

### 5. 🔥 Kitchen Load Indicator
The system counts a restaurant's currently active orders (`CONFIRMED` + `PREPARING`) and
derives a LOW/MEDIUM/HIGH load indicator from that count. Staff can also manually override
the indicator. This load value feeds directly into the pricing engine below.

### 6. 📈 Dynamic Pricing Engine
Menu prices aren't static — each item's effective price is computed at cart-add time from
two multipliers: a demand multiplier (based on how much of that item has sold in the last
hour) and a time-of-day multiplier (opening discounts, lunch/dinner peak surcharges,
pre-closing discounts). No restaurant owner has to manually adjust prices for rush hours.

### 7. ⏰ Pre-Order with Cancellation Penalty
Users can schedule an order for a future time instead of immediate delivery. A scheduler
auto-confirms pre-orders once their scheduled time arrives. Cancelling a pre-order refunds
only 75% of the order value — the 25% penalty reflects that ingredients were already
committed for that slot, versus a full refund for cancelling a same-day regular order.

---

## 💳 Payment & Wallet

All in-app spending — orders, mess subscriptions, group deal participation, shared cart
contributions — draws from an internal wallet balance rather than charging a card per
transaction. The wallet itself is funded through the **PayPal Checkout SDK** (currently
sandbox, USD-only):

1. User requests a deposit → Flocko creates a PayPal order and returns the approval URL
2. User approves the payment on PayPal's hosted flow
3. PayPal redirects back → Flocko captures the payment and credits the wallet
4. All downstream spending (orders, deals, subscriptions) is a wallet debit, not a new
   payment each time

This keeps every other feature's payment logic simple — one wallet debit/credit path — while
still handling a real external payment provider correctly at the funding step.

---

## 🎓 What This Project Demonstrates

This isn't meant to look like a feature checklist — it's meant to show the kind of
decisions a backend engineer has to make once a system has more than one moving part:

- **Module boundaries** enforced through facades, not just folder names
- **Transactional correctness** across features that touch the same resources (wallet,
  orders) from different entry points (checkout, deal fulfillment, subscription orders)
- **State machines** for order status and group deal status, validated centrally
- **Scheduler-driven side effects** (subscriptions, pre-order confirmation, deal
  expiry/fulfillment) that have to coexist with user-initiated actions on the same data
- **Auth and authorization** that goes beyond "logged in or not" — ownership checks,
  role-gated admin workflows, and staff-to-restaurant employment checks

---

## 📝 Notes & Learnings

See `notes/failures-log.md` and `notes/daily-log.md` for documented mistakes and lessons
learned throughout the project — including the ENUM-in-database pitfall and the reasoning
behind cascading decisions on entity relationships.