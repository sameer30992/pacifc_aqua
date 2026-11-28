#!/usr/bin/env python3
"""Import Pacific Aqua Excel masters + BILL ledger into MySQL."""

from __future__ import annotations

import hashlib
import re
import sys
from datetime import datetime
from pathlib import Path

import bcrypt
import openpyxl
import pymysql

ROOT = Path(__file__).resolve().parents[1]
XLSX = ROOT / "docs" / "39. Pacific Aqua.xlsx"

DB = dict(
    host="127.0.0.1",
    port=3306,
    user="pacific",
    password="pacific",
    database="pacific_aqua",
    charset="utf8mb4",
    autocommit=False,
)

MONTH_MAP = {
    "JAN": 1,
    "FEB": 2,
    "MAR": 3,
    "APR": 4,
    "MAY": 5,
    "JUNE": 6,
    "JUN": 6,
    "JULY": 7,
    "JUL": 7,
    "AUG": 8,
    "SEPT": 9,
    "SEP": 9,
    "OCT": 10,
    "NOV": 11,
    "DEC": 12,
}

PRODUCT_COLS = [
    # (excel index, product code)
    (3, "jar"),
    (4, "ch"),
    (5, "pouch"),
    (6, "l1"),
    (7, "ml500"),
    (8, "ml250"),
]


def norm_name(name: str) -> str:
    s = str(name or "").strip().lower()
    s = re.sub(r"[,\-·]+", " ", s)
    s = re.sub(r"\s+", " ", s)
    return s


def parse_month_token(token) -> tuple[int, int] | None:
    if token is None:
        return None
    t = str(token).strip().upper().replace(" ", "")
    m = re.match(r"^([A-Z]+)\.?(\d{2}|\d{4})$", t)
    if not m:
        return None
    mon = MONTH_MAP.get(m.group(1))
    if not mon:
        return None
    y = int(m.group(2))
    if y < 100:
        y += 2000
    return y, mon


def connect():
    return pymysql.connect(**DB)


def fetch_product_map(cur) -> dict[str, tuple[int, float]]:
    cur.execute("SELECT id, code, default_rate FROM products")
    out = {}
    for pid, code, rate in cur.fetchall():
        out[code] = (pid, float(rate or 0))
    return out


def ensure_employees(cur):
    # Keep owner; upsert helpers from ATT.
    names = ["Prakash", "Deepak", "Himanshu", "Tikesh", "Pankaj", "Khillu"]
    pwd = bcrypt.hashpw(b"helper123", bcrypt.gensalt(rounds=10)).decode()
    for i, name in enumerate(names, start=1):
        phone = f"90000000{i:02d}"
        cur.execute("SELECT id FROM users WHERE phone=%s", (phone,))
        if cur.fetchone():
            cur.execute(
                "UPDATE users SET name=%s, role='HELPER', status='ACTIVE', password_hash=%s WHERE phone=%s",
                (name, pwd, phone),
            )
        else:
            cur.execute(
                """
                INSERT INTO users (name, phone, password_hash, role, status)
                VALUES (%s, %s, %s, 'HELPER', 'ACTIVE')
                """,
                (name, phone, pwd),
            )
    print(f"employees upserted: {len(names)}")


def import_customers(cur, wb) -> dict[str, int]:
    ws = wb["Customers"]
    # wipe transactional + customers (keep products/users)
    cur.execute("SET FOREIGN_KEY_CHECKS=0")
    for table in [
        "payments",
        "visit_lines",
        "visits",
        "order_lines",
        "orders",
        "plant_trip_lines",
        "plant_trips",
        "customers",
    ]:
        cur.execute(f"TRUNCATE TABLE {table}")
    cur.execute("UPDATE visit_sequences SET last_seq=0 WHERE id=1")
    cur.execute("SET FOREIGN_KEY_CHECKS=1")

    by_norm: dict[str, int] = {}
    inserted = 0
    for r in ws.iter_rows(min_row=4, values_only=True):
        if not r or not r[1]:
            continue
        name = str(r[1]).strip()
        if not name or name.lower() in {"none", "nan"}:
            continue
        addr = str(r[2]).strip() if r[2] else None
        if addr and addr.lower() in {"none", "nan", "#ref!", "#value!"}:
            addr = None
        phone = None
        if len(r) > 11 and r[11]:
            digits = re.sub(r"\D", "", str(r[11]))
            if len(digits) >= 10:
                phone = digits[-10:]
        card = None
        # use row number as soft card
        nos = r[0]
        if isinstance(nos, (int, float)):
            card = f"PA-{int(nos):04d}"

        # infer type lightly
        ctype = "RETAIL"
        low = name.lower()
        if any(x in low for x in ["pvt", "ltd", "agency", "traders", "marketing", "flanges"]):
            ctype = "DEALER"
        if any(x in low for x in ["hotel", "school", "hospital", "college", "club", "auditorium"]):
            ctype = "INSTITUTION"

        key = norm_name(name)
        if key in by_norm:
            continue
        cur.execute(
            """
            INSERT INTO customers (name, phone, city_area, type, card_no, status)
            VALUES (%s, %s, %s, %s, %s, 'ACTIVE')
            """,
            (name, phone, addr, ctype, card),
        )
        by_norm[key] = cur.lastrowid
        inserted += 1
    print(f"customers imported: {inserted}")
    return by_norm


def find_customer_id(by_norm: dict[str, int], name: str) -> int | None:
    key = norm_name(name)
    if key in by_norm:
        return by_norm[key]
    # prefix / contains soft match
    for k, cid in by_norm.items():
        if key.startswith(k) or k.startswith(key) or key in k or k in key:
            return cid
    return None


def import_bills(cur, wb, by_norm: dict[str, int], products: dict[str, tuple[int, float]]):
    ws = wb["BILL"]
    cur.execute("SELECT id FROM users WHERE role='OWNER' LIMIT 1")
    owner = cur.fetchone()
    owner_id = owner[0] if owner else None
    cur.execute("SELECT id FROM users WHERE role='HELPER' AND status='ACTIVE' ORDER BY id LIMIT 1")
    helper = cur.fetchone()
    helper_id = helper[0] if helper else None

    seq = 0
    inserted = 0
    skipped = 0
    unmatched = 0

    for r in ws.iter_rows(min_row=3, values_only=True):
        if not r or not r[2]:
            continue
        ym = parse_month_token(r[0])
        if not ym:
            skipped += 1
            continue
        year, month = ym
        cust_name = str(r[2]).strip()
        cid = find_customer_id(by_norm, cust_name)
        if not cid:
            # create on the fly from bill name
            cur.execute(
                """
                INSERT INTO customers (name, type, status)
                VALUES (%s, 'RETAIL', 'ACTIVE')
                """,
                (cust_name,),
            )
            cid = cur.lastrowid
            by_norm[norm_name(cust_name)] = cid
            unmatched += 1

        amount = float(r[9] or 0)
        paid = float(r[10] or 0)
        if paid > amount:
            paid = amount
        balance = max(amount - paid, 0.0)
        if amount <= 0 and all(not (r[i] or 0) for i, _ in PRODUCT_COLS):
            skipped += 1
            continue

        if paid <= 0:
            status = "PENDING"
            mode = "CREDIT"
        elif balance <= 0:
            status = "PAID"
            mode = "CASH"
        else:
            status = "PARTIAL"
            mode = "CASH"

        seq += 1
        visit_no = f"PA-{seq:06d}"
        visit_at = datetime(year, month, 15, 10, 0, 0)
        # stable idempotency from excel month+sr+name
        sr = r[1] or seq
        req = hashlib.sha1(f"{r[0]}|{sr}|{cust_name}".encode()).hexdigest()[:32]

        cur.execute(
            """
            INSERT INTO visits (
              visit_no, customer_id, helper_id, visit_at, payment_mode, payment_status,
              amount_total, amount_paid, balance, remarks, status, client_request_id,
              created_by, updated_by
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,'ACTIVE',%s,%s,%s)
            """,
            (
                visit_no,
                cid,
                helper_id,
                visit_at,
                mode,
                status,
                amount,
                paid,
                balance,
                f"Excel BILL {r[0]} #{sr}",
                req,
                owner_id,
                owner_id,
            ),
        )
        visit_id = cur.lastrowid

        # lines from product qty columns; amount allocated by rate*qty when possible
        line_rows = []
        computed = 0.0
        for idx, code in PRODUCT_COLS:
            qty = int(r[idx] or 0)
            if qty <= 0:
                continue
            pid, rate = products.get(code, (None, 0))
            if not pid:
                continue
            line_amt = round(rate * qty, 2)
            computed += line_amt
            line_rows.append((visit_id, pid, code, rate, qty, line_amt))

        if not line_rows and amount > 0:
            # fallback single jar line
            pid, rate = products.get("jar", (None, 40.0))
            if pid:
                qty = int(round(amount / rate)) if rate else 0
                if qty <= 0:
                    qty = 1
                line_amt = amount
                line_rows.append((visit_id, pid, "jar", rate, qty, line_amt))

        # if excel amount differs from rate*qty, scale last line or keep excel totals on visit
        name_by_code = {
            "jar": "20 Ltr Jar",
            "ch": "Chilled Jar",
            "pouch": "Pouch",
            "l1": "1 Ltr",
            "ml500": "500 ml",
            "ml250": "250 ml",
        }
        for visit_id, pid, code, rate, qty, line_amt in line_rows:
            cur.execute(
                """
                INSERT INTO visit_lines (
                  visit_id, product_id, product_name_snap, rate_snap, qty_diya, qty_mila, line_amount
                ) VALUES (%s,%s,%s,%s,%s,0,%s)
                """,
                (visit_id, pid, name_by_code.get(code, code), rate, qty, line_amt),
            )

        if paid > 0:
            cur.execute(
                """
                INSERT INTO payments (visit_id, amount, mode, paid_at, recorded_by)
                VALUES (%s,%s,%s,%s,%s)
                """,
                (visit_id, paid, mode, visit_at, owner_id),
            )
        inserted += 1

    cur.execute("UPDATE visit_sequences SET last_seq=%s WHERE id=1", (seq,))
    print(f"bill visits imported: {inserted} (created unmatched customers: {unmatched}, skipped: {skipped})")


def main():
    if not XLSX.exists():
        print("Excel not found:", XLSX, file=sys.stderr)
        sys.exit(1)
    print("Loading", XLSX.name)
    wb = openpyxl.load_workbook(XLSX, data_only=True, read_only=True)
    conn = connect()
    try:
        with conn.cursor() as cur:
            ensure_employees(cur)
            products = fetch_product_map(cur)
            if not products:
                raise RuntimeError("No products in DB — start server once to seed catalog")
            by_norm = import_customers(cur, wb)
            import_bills(cur, wb, by_norm, products)
        conn.commit()
        print("DONE")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
