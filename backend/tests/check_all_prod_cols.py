with open(r'C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt', 'r', encoding='utf-8', errors='ignore') as f:
    dump_text = f.read()

import re

def get_table_cols(tbl):
    m = re.search(r'CREATE TABLE `' + tbl + r'`\s*\((.*?)\)\s*ENGINE', dump_text, re.DOTALL)
    if not m:
        return []
    cols = []
    for line in m.group(1).split('\n'):
        line = line.strip().rstrip(',')
        if line and not re.match(r'^(PRIMARY KEY|KEY|UNIQUE KEY|CONSTRAINT)', line, re.I):
            cols.append(line.split()[0].strip('`'))
    return cols

tables = ['users', 'restaurants', 'branches', 'restaurant_memberships', 'categories', 'customers', 'menu_items', 'tables', 'orders', 'order_items', 'kot_tickets', 'kot_items', 'payments', 'inventory_items', 'stock_movements', 'devices', 'subscription_plans', 'subscriptions', 'sync_tombstones', 'idempotency_keys', 'restaurant_settings', 'audit_logs']

for t in tables:
    print(f"{t}: {get_table_cols(t)}")
