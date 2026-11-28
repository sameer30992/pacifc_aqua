# Client Call Summary — R&B 2.m4a (~56 min)
Source: /Users/sameerroy/Downloads/R&B 2.m4a
Transcripts: docs/transcripts/rb2_key_sections.txt (high-quality key sections), rb2_transcript.json (full pass, noisier)

## Context
Walkthrough of the Pacific Aqua mobile + admin mockups vs current WhatsApp-group + Excel workflow. Client (owner) and Sameer agreed on changes and a phased build.

---

## Mobile app (drivers / helpers) — requested changes

1. **Replace WhatsApp group entry**
   - Helpers currently log diya / mila / payment in WhatsApp; same work must be doable in the app with clearer structure.

2. **Simple New Sale flow (core pain fix)**
   - Select **customer first**
   - Enter **product + qty** (multiple products in one visit)
   - Mark **given / taken** (diya / mila / empty return)
   - Enter **amount collected**
   - Optional remarks / extra detail → save → syncs to backend

3. **Customer dues + empty jars on one screen**
   - After selecting a customer, show:
     - Remaining **money balance / udhari**
     - How many **empty jars** already with that customer
   - Add a **Check Balance / Check Dues** control so the helper sees full pending record before delivering
   - Solves the WhatsApp miss case: e.g. ₹200 pending not posted in group → next boy told “already paid”

4. **Route / regular customer list (no search for daily stops)**
   - If a vehicle/route has ~25 fixed customers, those names should appear **by default** without searching
   - Still allow **search** for ad-hoc / other customers
   - Reduces time vs typing free-text WhatsApp messages

5. **Rolling / shift start (can be phase 2)**
   - Driver starts a rolling (e.g. Rolling 1)
   - First step: enter **stock taken from godown/plant** before sales
   - Multiple drivers = separate routes

6. **Offline support**
   - If internet is down, **save on phone** and sync when back online

7. **Order notifications**
   - When admin creates a new order on backend and assigns a driver, that driver’s mobile gets a **notification** to deliver to that customer

8. **What mobile must cover from current WhatsApp group**
   - Qty taken from plant
   - Previous customer pending balance / empty jars
   - Delivery + collection entry
   - Ability to see previous hisaab even if bill isn’t generated yet

9. **Who uses what**
   - Field boys → **mobile**
   - Owner wants **laptop/admin** for big-screen work (ledger, bills, inventory); may still view some history on mobile

---

## Admin backend (laptop / shop PC) — requested changes

1. **Module layout (Excel coverage, better UX)**
   - Separate areas / tabs (not one messy sheet):
     - Attendance
     - Expenses
     - Inventory / jars / stock
     - Sales ledger
     - Billing
   - Need not be an exact Excel clone; similar coverage with cleaner UX

2. **Local server setup**
   - Run on a shop computer / cheap PC (~₹10–15k)
   - ~500 GB enough for years of text data
   - Mobiles send data to this local “server”
   - Keep machine on; daily **backup** of data

3. **New Order from backend**
   - Place order in admin
   - Choose customer
   - Choose **which driver** receives the mobile notification

4. **Customer-wise monthly bill / lekhajokha**
   - Per customer, for a given month:
     - Jars given
     - Jars taken / empties
     - Money pending
     - Payments received
   - **Bill Generate** from that summary
   - Client to send a clear photo/scan of a **blank bill with seal/stamp**; system overlays name, phone, amounts, date, etc.
   - Download + print (B/W print is fine)

5. **Sales ledger**
   - Month view of sales
   - Customer list with **outstanding amount**
   - Click customer → full detail drill-down

6. **Stock / factory inventory control**
   - Product lifted from company/plant
   - Delivered vs collected payment (driver chittha)
   - Factory remaining stock (e.g. purchased cartons/petis left)

---

## Delivery approach agreed on the call

1. **Phased release** — don’t ship every feature in v1
2. **v1 priority** = biggest time sink: WhatsApp/Excel entry (1–2 hrs/day) + customer dues/empty-jar visibility + basic sales ledger
3. Advanced rolling/route features can come **later**
4. Keep UX simple enough that the product could later be sold to other water businesses
5. First build: make core functions solid; polish / extra features iteratively

---

## Note on transcript quality
Call is Hinglish with screen-share noise; some mid-call segments have ASR repetition. Key-section re-transcription (large-v3-turbo) used for this summary. Full timed transcript: `docs/transcripts/rb2_key_sections.txt`.
