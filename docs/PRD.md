# Pacific Aqua Sales System — Product Requirements Document

| | |
| --- | --- |
| **Version** | 0.3 |
| **Date** | 27 Nov 2026 (Asia/Calcutta) |
| **Owner** | Sameer Roy / Pacific Aqua |
| **Status** | Draft — locked from client call `R&B 2.m4a` |
| **Apps** | (1) Mobile field app (2) Backend admin (laptop / shop PC) |

> **Sources:** Client call `R&B 2.m4a` (~56 min) → `docs/transcripts/rb2_call_summary.md`; Excel `39. Pacific Aqua.xlsx`; WhatsApp group delivery logs (diya / mila / liye).

---

## 1. Problem statement

Pacific Aqua runs field ops on a **WhatsApp group** and office books on **Excel**. That fails when:

- Helpers type free-text deliveries (`diya` / `mila` / payment) and pending dues get missed
- Next day’s helper gets told “already paid” because ₹ pending was never logged
- Owner spends **1–2 hours/day** re-entering WhatsApp into Excel
- Empty jars with customers and money udhari are not visible in one place
- Multi-helper routes mix customer lists; search/typing slows every stop
- Factory stock, attendance, expenses, and bills live in separate messy sheets

**Goal:** Replace WhatsApp+Excel with a **phone app for helpers** and a **laptop admin** on one shared ledger. Excel becomes export-only.

**Phased build (agreed on call):** Ship v1 around the biggest time sink first; add rolling/route advanced features later. Keep UX simple enough to resell to other water businesses later.

---

## 2. Product overview

| App | Audience | Device | Purpose |
| --- | --- | --- | --- |
| **Mobile field app** | Helpers | Android phone (web) | Replace WhatsApp entries: visit → diya/mila → collection; see dues & empty jars |
| **Backend admin** | Owner / office | Laptop / shop PC browser | Ledger, bills, inventory, attendance, expenses, orders → helper notify |

**Deployment (agreed):** Local shop PC (~₹10–15k) acts as always-on server; mobiles sync to it; ~500 GB enough for years of text data; **daily backup**.

Both apps read/write the **same database**. Owner prefers **big-screen admin** for ledger/bills/inventory; helpers use **mobile** only.

---

## 3. Personas

### P1 — Field helper (e.g. Tikesh, Himanshu, Deepak)

| | |
| --- | --- |
| **Role** | Helper |
| **Device** | Android phone, 4G, sometimes offline |
| **Language** | Hindi / Hinglish primary |
| **Day** | Takes stock from plant → follows route customers → diya/mila jars → collects cash → posts in WhatsApp today |
| **Pain today** | Free-text WhatsApp; no view of prior pending; next boy gets lied to; slow typing |
| **Goals** | Select customer → see dues/empties → log visit in &lt;1 min; work offline |
| **Primary app** | Mobile |

### P2 — Sameer · Owner / Admin

| | |
| --- | --- |
| **Role** | Admin / Owner |
| **Device** | Laptop in office / shop |
| **Language** | English + Hindi |
| **Day** | Reviews sales, dues, stock, bills; assigns ad-hoc orders to helpers |
| **Pain today** | Re-typing WhatsApp into Excel; no live outstanding; bill prep manual |
| **Goals** | One clean ledger; customer-wise monthly lekhajokha + bill; factory inventory; order → helper notify |
| **Primary app** | Backend (mobile optional for history) |

### P3 — Office accountant *(phase 2 optional)*

| | |
| --- | --- |
| **Role** | Accountant |
| **Device** | Desktop |
| **Goals** | Payments, month export without changing masters |
| **Primary app** | Backend (limited) |

### P4 — Customer / dealer *(no login in v1)*

Receives delivery + verbal/printed bill; needs correct qty, empties, and balance.

---

## 4. Users, roles & permissions

| Role | App | Create visit/sale | Edit | Manage masters | Place order → notify | Bills / export | Inventory |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Helper | Mobile | Own visits | Own, same day* | Read customers/products | Receive only | Own history | Log lift from plant (v1 lite / P2 rolling) |
| Admin / Owner | Backend (+ mobile optional) | Yes | Any | Yes | Yes | Yes | Yes |
| Accountant | Backend | No | Payments only | Read | No | Yes | Read |

\*Same-day edit default — confirm if owner wants stricter lock.

---

## 5. Use cases

### UC-01 — Login
Any user → phone/email + password → role-correct home.

### UC-02 — Field visit / New Sale (replaces WhatsApp) — **v1 core**
**Actor:** Helper  
**Flow:**

1. Open **New Sale / Visit**
2. Pick customer from **route list** (default) or **search** ad-hoc
3. Tap **Check Balance / Check Dues** → see money udhari + empty jars with customer
4. Add one or more products: qty **diya** (given) and/or **mila** (empty/taken)
5. Enter **amount collected**; optional remarks
6. Save → syncs to backend (or queues if offline)

**Success:** Visit appears in helper history + admin ledger; customer outstanding & empty-jar balance update.  
**Errors:** No customer; no lines; qty ≤ 0; amount paid &gt; billable total (if priced).

### UC-03 — Check Balance / Check Dues — **v1 core**
**Actor:** Helper  
**Flow:** After selecting customer → Check Balance → see remaining money + empty jars + recent hisaab (even if no bill generated).  
**Success:** Helper knows what to collect before delivering.  
**Pain solved:** Missed WhatsApp ₹200 pending → next boy told “already paid”.

### UC-04 — Route customers without search — **v1**
**Actor:** Helper  
**Flow:** On New Sale, default list shows ~fixed route customers (~25) for that helper/vehicle; search still available for others.  
**Success:** Daily stops appear without typing.

### UC-05 — Offline save + sync — **v1**
**Actor:** Helper  
**Flow:** Save visit with no/poor network → stored on phone → auto-sync when online.  
**Success:** No data loss; admin sees visits after sync.

### UC-06 — Admin New Order → helper notification — **v1**
**Actor:** Admin  
**Flow:** Backend → New Order → select customer → select helper → save.  
**Success:** That helper’s mobile gets a **notification** to deliver to that customer.

### UC-07 — Customer-wise monthly bill / lekhajokha — **v1**
**Actor:** Admin  
**Flow:** Pick customer + month → see jars given, jars taken/empties, money pending, payments → **Generate Bill** (template overlay) → download / print (B/W OK).  
**Success:** Printable bill matches monthly summary.

### UC-08 — Sales ledger + outstanding drill-down — **v1**
**Actor:** Admin  
**Flow:** Month ledger → customer list with outstanding → click customer → full detail.  
**Success:** Replaces Excel P&S browsing.

### UC-09 — Factory / plant inventory — **v1**
**Actor:** Admin (+ helper lift log)  
**Flow:** Track product lifted from plant, delivered, payment collected (helper chittha); remaining factory stock (e.g. cartons/petis).  
**Success:** Owner knows stock left without Excel.

### UC-10 — Attendance, expenses, purchases — **v1 modules**
**Actor:** Admin  
**Flow:** Separate tabs (not one sheet): Attendance/Salary, Daily Expenses, Outside Purchases — Excel coverage, cleaner UX.

### UC-11 — Day trip (plant → route → plant) — **v1 core**
**Actor:** Helper  
**Seamless flow (one trip object for the day):**

1. **Load at plant** — enter qty of each product taken onto the van → Start trip (van stock = loaded)
2. **Deliver on route** — visits: Given / Empty back / money; each Given reduces van full stock; Empty back increases van empties; cash tallies
3. **Return to plant** — app pre-fills expected **unsold full** (= loaded − given) and **empties collected**; helper confirms or adjusts with reason → Submit → trip closed; plant inventory updated

**Rules that make it seamless:**
- New Visit blocked until trip is loaded
- Home always shows trip step (Load → Deliver → Return) + live van stock + cash in hand
- No separate “remember what to return” — return screen is calculated from the same trip
- Variance (breakage / recount) requires a note; admin sees variance on backend Inventory

**Multi-helper:** each helper has their own open trip; routes stay separate.

### UC-12 — Language toggle (HI / EN)
Mobile primary labels switch; INR stays `en-IN`.

### UC-13 — Void / correct visit
Admin (helper same-day if allowed) → edit or void with reason; audit retained.

---

## 6. Functionalities (by area)

### 6.1 Authentication & access
- Login phone/email + password (v1)
- Role-based routing (mobile vs backend)
- Logout; session expiry; active/inactive users
- *(Later)* OTP login

### 6.2 Field visits / sales *(WhatsApp replacement)* — **v1**
- Customer-first New Sale / Visit
- Multi-product lines per visit
- Qty per product with bilingual labels: **Given** (दिया) = full jars left at customer; **Empty back** (मिला) = empty jars collected. Always show English + Hindi on the control so non-English helpers understand.
- Amount collected + payment mode (Cash / UPI / Bank / Credit)
- Payment status: Paid / Partial / Pending
- Remarks / extra detail
- Auto visit/invoice id (`PA-####`)
- Snapshot product name/rate on line
- List / filter own visits; detail + record further payment
- Share text optional (WhatsApp share still useful for customer SMS)

### 6.3 Check Balance / Dues on mobile — **v1**
- After customer select: outstanding **money** + **empty jars** (and other empties if tracked)
- Explicit **Check Balance / Check Dues** control
- Prior hisaab visible even when bill not yet generated

### 6.4 Route customer list — **v1**
- Assign customers to helper/route/vehicle
- Default New Sale list = route customers (no search required)
- Search / quick-add for ad-hoc customers

### 6.5 Offline sync — **v1**
- Queue creates/edits on device when offline
- Sync to local shop server when online
- Clear sync status on mobile (pending / synced / failed)

### 6.6 Orders & notifications — **v1**
- Admin: New Order (customer + assigned helper + optional products/notes)
- Push / in-app notification to assigned helper’s mobile
- Helper opens notification → customer preselected for visit

### 6.7 Customers
- CRUD; fields: name, phone, city/area, type, **route assignment**, notes
- Computed: money outstanding, empty-jar balance
- Customer statement / history (backend v1; mobile history nice-to-have)

### 6.8 Products
- Admin CRUD: name, category, unit, default rate, stock, active
- Products from Excel reality: Jar, Chilled, Pouch, bottles, Dispenser, etc.
- Mobile: select from catalog on visit

### 6.9 Payments & dues (admin)
- Dues queue: Partial + Pending
- Record payment against visit/sale
- Filter by helper, route, city, date
- Running outstanding per customer

### 6.10 Sales ledger & dashboard — **v1**
- Backend: month ledger; KPIs; charts
- Customer list with outstanding → drill-down
- Mobile Home: today’s own visits, collected, pending

### 6.11 Billing / lekhajokha — **v1**
- Customer + month summary: jars given, jars taken, money pending, payments
- **Bill Generate** from blank bill+seal/stamp template (client photo/scan)
- Overlay: name, phone, amounts, date, line details
- Download + print (B/W fine); optional download without print styling

### 6.12 Inventory / plant stock — **v1**
- Stock lifted from plant/godown
- Delivered vs collected (helper chittha rollup)
- Factory remaining stock
- Soft warnings optional

### 6.13 Attendance & salary *(Excel `ATT.`)* — **v1**
- Daily Present / Absent / Leave + wage/advance
- Month: days present, total paid, remaining payable
- Seed employees from sheet (Prakash, Deepak, Himanshu, Tikesh, Pankaj, Khillu, …)

### 6.14 Expenses & purchases — **v1**
- Daily expenses cash book (EMI, diesel, electric, office, …)
- Outside purchases (OTH PURCH + P&S purchase side)

### 6.15 Day trip / van stock — **v1**
- Trip lifecycle: `none` → `loaded` → `closed` (optionally reopen next morning)
- Plant load entry (per product qty + optional issuer note)
- Live **van stock**: full left, empties in van, cash/UPI in hand
- Visits require an open loaded trip; Given cannot exceed van full left (warn or hard block)
- Plant return: expected unsold + empties prefilled; adjust + variance reason; close trip
- Admin Inventory: lift / given / return / variance per trip
- *(Later polish)* multiple load top-ups mid-day; barcode/scan of jar crates

### 6.16 Local server & backup — **v1 infra**
- Shop PC as local server; mobiles point to it (LAN / VPN / tunnel TBD)
- Daily automated backup
- Text-heavy data; ~500 GB disk guidance from call

### 6.17 Users & audit
- Invite/deactivate; assign role + route
- Audit: createdBy / updatedBy / timestamps on visits & payments

### 6.18 Localization
- HI / EN on mobile primary screens
- Backend English-first (Hindi optional later)

---

## 7. Pages & screen specifications

### 7.1 Mobile field app

#### M0 — Login
Logo, phone/email, password, Login; language hint.

#### M1 — Home
Greeting; HI/EN; **Today’s trip** card (Load → Deliver → Return) with live van stock; today KPIs; **New Visit** (blocked if not loaded); recent visits; **Orders** badge.

#### M1b — Load from plant
Product steppers for qty loaded; optional plant witness; **Start trip · van loaded**.

#### M1c — Return to plant
Trip summary (loaded / given / empties / cash); return unsold (expected prefilled); submit empties; variance note; **Submit to plant · close trip**.

#### M2 — New Sale / Visit — **redesigned for call**
| Item | Detail |
| --- | --- |
| **Purpose** | Replace WhatsApp diya/mila entry |
| **Elements** | Route customer list (default) + search; selected customer summary; **Check Balance / Check Dues** button/panel; product lines with **Given (दिया)** + **Empty back (मिला)** steppers; amount collected; payment mode; remarks; Save |
| **Actions** | Save (online or offline queue); Back |
| **Validation** | Customer required; ≥1 line; qtys ≥ 0 with at least one &gt; 0; amount ≥ 0 |

#### M3 — Check Balance / Dues panel
Money outstanding; empty jars outstanding; last few visits; “Record collection only” shortcut optional.

#### M4 — Sale success
Visit id, customer, totals, sync status; optional share; Back to Home.

#### M5 — My Visits / Sales
Today / Week / All; status chips; open detail → record payment / view dues.

#### M6 — Orders inbox
List of admin-assigned orders; tap → open New Sale with customer prefilled.

#### M7 — Customers
Search + route filter; outstanding + empties; add/edit (if allowed).

#### M8 — Products (read-only browse)
Rate/stock reference.

#### M9 — Bottom nav (v1)
Home · New Sale · My Visits · Orders · More (Customers / Products)

#### M10 — Sync status (More)
Pending queue count; last sync; retry failed.

---

### 7.2 Backend admin

#### B0 — Login → Dashboard

#### B1 — Dashboard
Month revenue, collected, pending, visit count; top product/route; charts; wages/expenses snapshots.

#### B2 — Sales ledger
Dense table: date, visit/invoice, customer, helper, products (diya/mila summary), total, paid, balance, mode, status, remarks.  
Filters: date, status, helper, route, search. Export CSV/XLSX.

#### B3 — Customer outstanding
List customers with outstanding amount + empty jars; click → statement (month lines + payments).

#### B4 — New Order
Customer, helper assignee, notes/products, Save → notify helper.

#### B5 — Billing
Customer + month picker → lekhajokha summary → Generate Bill → preview / download / print.  
Template management: upload blank bill+seal image.

#### B6 — Inventory / plant stock
Lift entries, deliveries vs collections, remaining stock by product.

#### B7 — Dues / collections
Partial/Pending work queue; record payment.

#### B8 — Customers / Products masters
CRUD; route assignment on customer; rates/stock on product.

#### B9 — Attendance
Daily P/A + pay (Excel ATT.).

#### B10 — Salary
Month wage cards from attendance pays.

#### B11 — Expenses
Cash issue/receive book.

#### B12 — Purchases
Outside purchase register.

#### B13 — Reports / export
Date range; Excel-parity presets; download.

#### B14 — Users & roles
Invite; role; route/helper mapping; active toggle.

#### B15 — Sidebar (v1 modules)
Dashboard · Sales ledger · Customers / Outstanding · Orders · Billing · Inventory · Dues · Products · Attendance · Salary · Expenses · Purchases · Reports · Users

---

## 8. Key end-to-end workflows

### Flow A — Field visit replaces WhatsApp
1. Helper opens New Sale → picks route customer  
2. Check Balance → sees ₹200 + 2 empty jars pending  
3. Logs 5 jar diya / 5 mila + collects ₹800  
4. Saves (offline OK) → syncs to shop server  
5. Owner sees visit on ledger; customer outstanding updates  

### Flow B — Admin order → helper notify
1. Owner creates New Order for “G R Electrical”, assigns Tikesh  
2. Tikesh gets mobile notification  
3. Opens → customer prefilled → completes visit  

### Flow C — Month bill
1. Owner opens Billing → customer + November  
2. Reviews jars given/taken, payments, pending  
3. Generate Bill from seal template → print B/W  

### Flow D — Day trip (seamless)
1. Tikesh at plant → Load 40 jar / 25 chilled → Start trip  
2. On route visits: Given reduces van; Empty back + cash accumulate on Home  
3. Back at plant → Return screen shows Expected unsold 18 jar + 22 empties  
4. Confirms submit → trip closed; plant stock + empties updated; admin sees chittha  

### Flow E — Blocked visit without load
1. Helper taps New Visit with no open trip  
2. App prompts Load from plant first  
3. After load, visits unlock  



---

## 9. Data model (high level)

### Visit / Sale
date, visitNo (`PA-####`), customerId (+ snapshot), helperId, routeId, paymentMode, paymentStatus, amountPaid, remarks, syncStatus, status (Active/Void), audit fields

### Visit line
productId, productName snapshot, qtyDiya, qtyMila, rate, amount

### Customer
name, phone, city/area, type, routeId, notes → computed moneyDue, emptyJarsDue

### Product
name, category, unit, defaultRate, stock, active

### Order (admin → helper)
customerId, helperId, notes, status (Open/Done/Cancelled), createdAt

### Payment entry
visitId, amount, mode, paidAt, recordedBy

### Stock lift / inventory movement
date, productId, qty, from (Plant/Godown), helperId/rollingId, notes

### Attendance / Expense / Purchase
As in Excel ATT., Daily Expenses, OTH PURCH

### User
name, phone, email, role, routeIds, active

---

## 10. Business rules

1. Visit/invoice numbers unique and monotonic.  
2. `paymentStatus` from amount paid vs total (Pending / Partial / Paid).  
3. Amount paid cannot exceed total.  
4. Empty-jar balance: prior empties + diya − mila (confirm exact jar cycle with owner).  
5. Money outstanding: prior due + billable − collected.  
6. Voided visits excluded from revenue KPIs; kept for audit.  
7. Helpers see own visits (+ assigned orders); Admin sees all.  
8. Rate changes do not alter past lines.  
9. Offline saves must sync exactly once (idempotent visit ids).  
10. Currency: INR (`en-IN`).  
11. Bill template overlays must not invent seal/stamp — use client-provided blank bill image.

---

## 11. Scope

### In scope — **v1 (call priority)**
- Mobile + Backend web apps on **local shop server**
- **Day trip:** load at plant → visits → return unsold + empties to plant
- WhatsApp-replacement visit entry (Given / Empty back + collection)
- Check Balance / empty jars + money dues
- Route customer default list + search
- Offline queue + sync
- Admin New Order → helper notification
- Sales ledger + customer outstanding drill-down
- Customer-wise monthly bill generate (template) + print/download
- Plant/factory inventory tied to trips
- Attendance, salary, expenses, purchases modules
- Roles: Helper + Admin
- HI/EN on mobile
- Daily backup

### Phase 2
- Mid-day plant top-up loads; multi-vehicle advanced isolation UX
- Native iOS/Android wrappers
- Full GST / e-invoice
- Customer self-service portal
- Accountant role build-out
- Personal loan / EMI “Challenge” tracker (owner finance)

### Out of scope
- Exact Excel clone UI (coverage yes, UX cleaner)
- AI parsing of WhatsApp chat as primary input (optional later; call preferred structured app)

---

## 12. Non-functional requirements

| Area | Target |
| --- | --- |
| Performance | Mobile save &lt; 2s on 4G when online; offline save instant |
| Sync | Reliable queue; conflict policy = last-write with audit (confirm) |
| Security | HTTPS if exposed; role checks on every API; LAN lock-down for shop PC |
| Availability | Shop PC always-on; daily backup |
| Localization | HI/EN mobile |
| Audit | Who created/edited each visit & payment |
| Browsers | Chrome/Safari mobile; Chrome/Edge desktop |
| Accessibility | Large tap targets (≥44px) |
| Disk | ~500 GB guidance sufficient for years of text data |

---

## 13. Milestones

| Milestone | Deliverable |
| --- | --- |
| **M0** | Discovery lock — Excel + `R&B 2.m4a` summary ✅ |
| **M1** | PRD v0.3 + static HTML mockups (update for Check Balance, Orders, Billing) |
| **M2** | Vertical slice — auth, visit create (diya/mila), ledger on local server |
| **M3** | v1 — dues/empties, route list, offline sync, order notify, bill generate, inventory lite |
| **M4** | Harden — backups, Excel import history, rolling (P2), accountant role |

---

## 14. Open questions

1. Exact empty-jar formula (diya/mila/chilled/pouch variants)?  
2. Bill template: one blank image for all, or per product type?  
3. Local server access from field: same Wi‑Fi only, or public tunnel/VPN?  
4. Push notifications: FCM/web push vs polling while shop PC is LAN-only?  
5. Can helpers edit yesterday’s visits?  
6. GST / HSN on v1 bills?  
7. Dealer vs retail pricing lists?  
8. Stock decrement on each diya in v1 — hard block or warning only?  

---

## 15. Acceptance criteria (v1)

- [ ] Helper must Load at plant before New Visit unlocks  
- [ ] Visits update live van stock (full left / empties / cash)  
- [ ] Return screen prefills unsold + empties from the same trip; close updates plant inventory  
- [ ] Helper creates a multi-product visit (Given / Empty back + collection) in under 60 seconds  
- [ ] Check Balance shows correct money + empty jars before save  
- [ ] Route customers appear without search; search finds non-route customers  
- [ ] Offline save syncs later and appears once on admin ledger  
- [ ] Admin New Order notifies assigned helper; visit can complete from that order  
- [ ] Admin sees visit on ledger without Excel/WhatsApp  
- [ ] Customer monthly lekhajokha → Generate Bill → download/print  
- [ ] Inventory shows plant lift vs remaining stock  
- [ ] Attendance / expenses / purchases usable as separate modules  
- [ ] Helper cannot access Users or other helpers’ private data  
- [ ] HI/EN toggle works on mobile Home and New Sale  
- [ ] Daily backup job documented and runnable on shop PC  

---

## 16. Document history

| Ver | Date (IST) | Notes |
| --- | --- | --- |
| 0.1 | 22 Nov 2026 | Initial draft from kickoff intent |
| 0.2 | 22 Nov 2026 | Personas, use cases, page specs, Excel ATT/expenses/purchases |
| 0.3 | 27 Nov 2026 | Locked from `R&B 2.m4a` / `rb2_call_summary.md`: WhatsApp replace, Check Balance, route list, offline, order notify, local server, billing template, inventory, phased v1 vs rolling P2 |

---

*Mockups: `index.html`, `mobile.html`, `backend.html`, `prd.html`. Call summary: `docs/transcripts/rb2_call_summary.md`.*
