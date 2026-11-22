# Pacific Aqua Sales System — Product Requirements Document

| | |
| --- | --- |
| **Version** | 0.2 |
| **Date** | 22 Nov 2026 (Asia/Calcutta) |
| **Owner** | Sameer Roy / Pacific Aqua |
| **Status** | Draft for review |
| **Apps** | (1) Mobile sales web app (2) Backend admin web app |

> **Source note:** Kickoff referenced `R&B.m4a` (Hindi meeting) and `Pacific Aqua.xlsx` (sales sheet). Those files were not available in this environment. Personas, fields, and flows below are inferred from the stated goal (replace Excel sales tracking). Re-attach the recording and sheet to lock exact columns, Hindi terms, and credit rules.

---

## 1. Problem statement

Pacific Aqua records each sale in a shared Excel workbook. That works for one person at a desk, but fails when:

- Field staff need to enter sales from a phone
- More than one person edits the same sheet (conflicts, lost rows)
- Payment status, outstanding balances, and daily collections are hard to see
- Reports by city, salesperson, product, or payment mode need manual pivots

**Goal:** Make a shared digital sales ledger the system of record, with a phone-first app for the field and a desktop backend for the office.

---

## 2. Product overview

| App | Audience | Device | Purpose |
| --- | --- | --- | --- |
| **Mobile sales app** | Sales executives | Phone browser (web) | Capture sales, look up rates/customers, see today’s numbers |
| **Backend admin** | Owner / office admin | Laptop / desktop browser | Full ledger, dues, masters, reports, users |

Both apps read/write the **same sales database**. Excel becomes an **export**, not the live register.

---

## 3. Personas

### P1 — Rahul · Field Sales Executive

| | |
| --- | --- |
| **Role** | Sales Executive |
| **Device** | Android phone, often on 4G, sometimes weak signal |
| **Language** | Hindi primary; can read simple English |
| **Day** | Visits dealers/retailers; books orders; collects cash/UPI; needs speed |
| **Pain today** | Calls office to note sale, or waits until evening to update Excel; forgets rates; loses track of credit |
| **Goals** | Enter a sale in under a minute; see today’s collection; share bill summary on WhatsApp |
| **Primary app** | Mobile |

### P2 — Sameer · Owner / Admin

| | |
| --- | --- |
| **Role** | Admin / Owner |
| **Device** | Laptop in office |
| **Language** | English + Hindi |
| **Day** | Reviews all sales, follows dues, manages products/prices, month-end export |
| **Pain today** | Excel is messy; cannot see who sold what live; dues chase is manual |
| **Goals** | One clean ledger; dues list; export like today’s sheet; control who can edit |
| **Primary app** | Backend |

### P3 — Priya · Office Accountant *(phase 2 optional)*

| | |
| --- | --- |
| **Role** | Accountant |
| **Device** | Desktop |
| **Day** | Marks collections, reconciles pending, prepares monthly sheet |
| **Goals** | Update payments without changing product masters; export for accounts |
| **Primary app** | Backend (limited) |

### P4 — Dealer / Customer *(indirect — not a login user in MVP)*

| | |
| --- | --- |
| **Role** | Buyer (Dealer / Retail / Institution) |
| **Touchpoint** | Receives WhatsApp sale summary / verbal invoice from executive |
| **Need** | Correct qty, rate, balance due |

---

## 4. Users, roles & permissions

| Role | Login app | Create sale | Edit any sale | Manage products | Manage users | Export | Record dues |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Sales Executive | Mobile | Own only | Own, same day* | Read | No | Own sales (optional) | Own sales |
| Admin / Owner | Backend (+ mobile optional) | Yes | Yes | Yes | Yes | All | Yes |
| Accountant | Backend | No | Payments only | Read | No | All | Yes |

\*Same-day edit policy is a default assumption — confirm after reviewing the meeting recording.

---

## 5. Use cases

### UC-01 — Login

**Actor:** Any user  
**Precondition:** Active account  
**Flow:** Open app → enter phone/email + password (or OTP later) → land on Home/Dashboard  
**Success:** Session started; role-correct home screen  
**Errors:** Invalid credentials; inactive user

### UC-02 — Create field sale (happy path)

**Actor:** Sales Executive (Rahul)  
**Precondition:** Logged into mobile  
**Flow:**

1. Tap **New Sale**
2. Select or quick-add customer
3. Add one or more products + qty (rate prefilled, editable)
4. Choose payment mode; enter amount paid
5. Save → system assigns invoice `PA-####`
6. See success screen; optionally share WhatsApp text

**Success:** Sale appears in My Sales and in Backend ledger  
**Errors:** No customer; qty ≤ 0; no lines; amount paid > total

### UC-03 — Quick-add customer while selling

**Actor:** Sales Executive  
**Flow:** On New Sale → Add customer → name, phone, city, type → save → selected on sale  
**Success:** Customer available system-wide

### UC-04 — View today’s performance

**Actor:** Sales Executive  
**Flow:** Open Home → see sales count, collected, pending for today  
**Success:** Numbers match own sales for today

### UC-05 — Collect balance on a credit sale

**Actor:** Sales Executive or Admin  
**Flow:** Open sale (Partial/Pending) → Record payment → enter amount → status recalculates (Partial/Paid)  
**Success:** Outstanding reduced; dues list updates

### UC-06 — Chase dues from office

**Actor:** Admin  
**Flow:** Backend → Dues → filter by city/salesperson → open sale → record payment or call customer  
**Success:** Pending balance list shrinks; audit of payment kept

### UC-07 — Maintain product rates

**Actor:** Admin  
**Flow:** Products → add/edit name, category, unit, default rate, stock → save  
**Success:** Mobile shows updated rates on next New Sale

### UC-08 — Replace Excel month close

**Actor:** Admin / Accountant  
**Flow:** Sales ledger → filter date range → Export CSV/XLSX  
**Success:** File usable like current `Pacific Aqua.xlsx` layout (column parity TBD)

### UC-09 — Onboard a salesperson

**Actor:** Admin  
**Flow:** Users → invite → name, phone, role=Executive → active  
**Success:** Executive can log into mobile

### UC-10 — Void / correct a wrong sale

**Actor:** Admin (Executive same-day only if allowed)  
**Flow:** Open sale → Edit lines/payment **or** Void with reason  
**Success:** Ledger reflects correction; void retained in audit (not hard-deleted)

### UC-11 — Switch language (HI / EN)

**Actor:** Sales Executive  
**Flow:** Toggle हिंदी / EN on mobile  
**Success:** Primary labels switch; numbers/currency stay `en-IN`

---

## 6. Functionalities (by area)

### 6.1 Authentication & access

- Login with phone or email + password (MVP)
- Role-based routing (mobile vs backend)
- Logout; session expiry
- Active/inactive user flag
- *(Later)* OTP login, password reset

### 6.2 Sales

- Create multi-line sale
- Auto invoice number (`PA-####`)
- Default date = today; editable by Admin
- Snapshot product name/rate on line (history safe if rate changes later)
- Payment modes: Cash, UPI, Bank, Credit
- Payment status auto: Paid / Partial / Pending from amount paid vs total
- Remarks / notes
- List, filter, search sales
- Sale detail view
- Edit / void (permission-based)
- WhatsApp-friendly share text after save

### 6.3 Customers

- Create, search, edit customer
- Fields: name, phone, city, type (Dealer / Retail / Institution)
- Outstanding balance (computed from unpaid sales)
- Customer history (list of sales) — Backend MVP; Mobile nice-to-have

### 6.4 Products

- CRUD catalog (Admin)
- Fields: name, category, unit, default rate, stock
- Mobile: read-only browse/search for rate reference
- Soft stock warning if qty > stock (optional MVP)

### 6.5 Payments & dues

- Record payment against a sale
- Dues queue: all Partial + Pending
- Filter dues by salesperson, city, date
- Running outstanding per customer

### 6.6 Dashboards & reports

- Mobile Home: today KPIs (own sales)
- Backend Dashboard: month revenue, collected, pending, invoice count, top city/product
- Export filtered ledger to CSV/XLSX
- *(Later)* GST summary, salesperson leaderboard

### 6.7 Users & admin

- Invite/deactivate users
- Assign role
- Audit: createdBy / updatedBy / timestamps on sales

### 6.8 Localization

- HI / EN string packs on mobile primary screens
- Backend English-first (Hindi optional later)

---

## 7. Pages & screen specifications

### 7.1 Mobile sales app

#### M0 — Login

| Item | Detail |
| --- | --- |
| **Purpose** | Authenticate executive |
| **Elements** | Logo, phone/email, password, Login CTA, language hint |
| **Actions** | Submit → Home |
| **Empty/error** | Inline invalid login message |

#### M1 — Home

| Item | Detail |
| --- | --- |
| **Purpose** | Today snapshot + primary CTA |
| **Elements** | Greeting, HI/EN toggle, KPI cards (Sales, Collected, Pending, Date), **New Sale** button, recent 3 sales |
| **Actions** | New Sale; tap sale → Detail; bottom nav |
| **States** | Loading skeleton; empty “No sales today” |

#### M2 — New Sale

| Item | Detail |
| --- | --- |
| **Purpose** | Capture a full invoice |
| **Elements** | Customer select/search + Add; line list (product, qty, rate, amount); Add product; Total; Payment mode chips; Amount paid; Remarks; Save |
| **Actions** | Save → Success; Back |
| **Validation** | Customer required; ≥1 line; qty > 0; amount paid ≥ 0 and ≤ total |
| **States** | Saving spinner; validation errors under fields |

#### M3 — Sale success

| Item | Detail |
| --- | --- |
| **Purpose** | Confirm save + share |
| **Elements** | Invoice no, customer, total, status; Share WhatsApp; Back to Home |
| **Actions** | Share copies/opens text summary |

#### M4 — My Sales

| Item | Detail |
| --- | --- |
| **Purpose** | Browse own sales |
| **Elements** | Filters: Today / Week / All; status chips; list rows (customer, invoice, amount, status) |
| **Actions** | Open detail |
| **States** | Empty list message |

#### M5 — Sale detail

| Item | Detail |
| --- | --- |
| **Purpose** | View lines + collect balance |
| **Elements** | Header fields; line table; paid/balance; Record payment (if not Paid) |
| **Actions** | Record payment; *(if allowed)* Edit |

#### M6 — Customers

| Item | Detail |
| --- | --- |
| **Purpose** | Find / add customers |
| **Elements** | Search; list (name, city, phone, outstanding); Add customer |
| **Actions** | Open/edit; Add |

#### M7 — Add / Edit customer

| Item | Detail |
| --- | --- |
| **Elements** | Name, phone, city, type |
| **Validation** | Name + phone required |

#### M8 — Products (read-only)

| Item | Detail |
| --- | --- |
| **Purpose** | Rate reference in field |
| **Elements** | Search; list name, category, rate, stock |

#### M9 — Bottom navigation

Home · My Sales · Customers · Products

---

### 7.2 Backend admin

#### B0 — Login

Same as mobile; routes Admin/Accountant to Dashboard.

#### B1 — Dashboard

| Item | Detail |
| --- | --- |
| **Purpose** | Month health at a glance |
| **Elements** | KPIs: Revenue, Collected, Pending dues, Invoice count; Top city; Top product; Recent sales table |
| **Actions** | Click recent sale → editor; nav to ledger/dues |

#### B2 — Sales ledger *(Excel replacement)*

| Item | Detail |
| --- | --- |
| **Purpose** | Full searchable register |
| **Columns (proposed)** | Date, Invoice, Customer, Phone, City, Salesperson, Products summary / lines, Total, Paid, Balance, Mode, Status, Remarks |
| **Filters** | Date range, status, city, salesperson, search text |
| **Actions** | New sale; open row; Export |
| **States** | Empty; loading; no filter matches |

#### B3 — Sale create / edit

| Item | Detail |
| --- | --- |
| **Purpose** | Desktop invoice form |
| **Elements** | Same fields as mobile + salesperson selector + void control |
| **Actions** | Save; Void with reason; Cancel |

#### B4 — Dues / collections

| Item | Detail |
| --- | --- |
| **Purpose** | Work queue for unpaid balances |
| **Elements** | List of Partial/Pending; balance; customer phone; Record payment CTA |
| **Filters** | City, salesperson, ageing (optional) |

#### B5 — Customers

| Item | Detail |
| --- | --- |
| **Elements** | Table: name, phone, city, type, outstanding; search; Add/Edit |
| **Actions** | Open customer statement (sales list) |

#### B6 — Products

| Item | Detail |
| --- | --- |
| **Elements** | Table/cards: name, category, unit, rate, stock; Add/Edit/Deactivate |

#### B7 — Reports / export

| Item | Detail |
| --- | --- |
| **Purpose** | Month close & sharing |
| **Elements** | Date range; columns preset “Excel parity”; Download CSV/XLSX |
| **Success** | File opens in Excel with expected headers |

#### B8 — Users & roles

| Item | Detail |
| --- | --- |
| **Elements** | User list; invite form (name, phone, role); active toggle |
| **Actions** | Create; deactivate |

#### B9 — Sidebar navigation

Dashboard · Sales ledger · Dues · Customers · Products · Reports · Users

---

## 8. Key end-to-end workflows

### Flow A — Field sale → office visibility

1. Rahul logs into mobile  
2. Creates sale for dealer (multi-line, UPI partial)  
3. Saves → `PA-1143`  
4. Sameer opens Backend ledger → sees sale immediately  
5. Dues shows remaining balance until paid  

### Flow B — Credit collection

1. Sameer opens Dues → Green Valley Farm balance  
2. Records bank payment  
3. Status → Paid; customer outstanding updates  
4. Rahul sees updated status on My Sales  

### Flow C — Month close

1. Sameer filters November sales  
2. Exports Excel  
3. Reviews Pending list before filing  

---

## 9. Data model (Excel replacement)

### Sale

| Field | Type | Notes |
| --- | --- | --- |
| date | date | Default today |
| invoiceNo | string | Auto `PA-####` |
| customerId | ref | + name/phone snapshot |
| city | string | From customer or override |
| salespersonId | ref | Default = logged-in user |
| paymentMode | enum | Cash / UPI / Bank / Credit |
| paymentStatus | enum | Paid / Partial / Pending |
| amountPaid | number | |
| remarks | text | Optional |
| status | enum | Active / Void |
| createdAt / updatedAt / createdBy | audit | |

### Sale line

| Field | Type |
| --- | --- |
| productId | ref |
| productName / category | snapshot |
| qty | number |
| rate | number |
| amount | qty × rate |

### Customer

name, phone, city, type (Dealer / Retail / Institution), notes

### Product

name, category, unit, defaultRate, stock, active

### User

name, phone, email, role, active

### Payment entry *(recommended)*

saleId, amount, mode, paidAt, recordedBy — keeps collection history

---

## 10. Business rules

1. Invoice numbers are unique and monotonic.  
2. `paymentStatus` is derived:  
   - paid = 0 → Pending  
   - 0 < paid < total → Partial  
   - paid = total → Paid  
3. Amount paid cannot exceed total.  
4. Voided sales excluded from revenue KPIs; retained for audit.  
5. Executives see only their sales on mobile (Admin sees all).  
6. Product rate changes do not alter past sale lines.  
7. Currency display: INR (`en-IN`).  

---

## 11. Scope

### In scope (MVP)

- Mobile + Backend web apps  
- Sales, customers, products, dues, dashboard, export  
- Roles: Executive + Admin  
- HI/EN on mobile  
- Password login  

### Out of scope (MVP)

- Native iOS/Android wrappers  
- Full GST filing / e-invoice  
- Multi-warehouse inventory  
- Delivery tracking  
- Customer self-service portal  
- Offline-first sync  
- Accountant role (design now, build next)

---

## 12. Non-functional requirements

| Area | Target |
| --- | --- |
| Performance | Mobile save < 2s on 4G |
| Security | HTTPS, role checks on every API |
| Availability | Daily backup; single region OK for MVP |
| Localization | HI/EN mobile strings |
| Audit | Who created/edited each sale & payment |
| Browsers | Chrome/Safari mobile; Chrome/Edge desktop |
| Accessibility | Large tap targets on mobile (≥44px) |

---

## 13. Milestones

| Milestone | Deliverable |
| --- | --- |
| **M0** | Discovery lock — interpret `Pacific Aqua.xlsx` + `R&B.m4a` |
| **M1** | PRD + static HTML mockups |
| **M2** | Vertical slice — auth, create sale, ledger |
| **M3** | MVP — dues, export, bilingual, roles |
| **M4** | Harden — Excel import history, backups, accountant role |

---

## 14. Open questions

1. Exact columns/names from `Pacific Aqua.xlsx`  
2. Decisions from Hindi `R&B.m4a` (credit policy, invoice format, who may edit)  
3. Dealer vs retail pricing?  
4. GST / HSN on MVP invoices?  
5. Stock decrement on sale in MVP?  
6. Login: password vs phone OTP?  
7. Can executives edit yesterday’s sales?  

---

## 15. Acceptance criteria (MVP)

- [ ] Executive creates a multi-line sale on phone in under 60 seconds  
- [ ] Admin sees that sale on backend ledger without using Excel  
- [ ] Dues total equals sum of balances on Partial/Pending sales  
- [ ] Export downloads a usable spreadsheet of the filtered ledger  
- [ ] HI/EN toggle works on mobile Home and New Sale  
- [ ] Executive cannot access Users or other executives’ sales  
- [ ] Admin can invite/deactivate an executive  
- [ ] Voided sale no longer counts in revenue KPI  

---

## 16. Document history

| Ver | Date (IST) | Notes |
| --- | --- | --- |
| 0.1 | 22 Nov 2026 | Initial draft from kickoff intent |
| 0.2 | 22 Nov 2026 | Added personas, use cases, page specs, functionality map, permissions, business rules |

---

*Mockups: static HTML pages in this repo (`index.html`, `mobile.html`, `backend.html`, `prd.html`).*
