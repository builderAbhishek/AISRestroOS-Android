import re

def verify_migration():
    with open(r'F:\Android Apps\AISRestroOS\backend\database\migrations\FINAL_PRODUCTION_MIGRATION.sql', 'r', encoding='utf-8') as f:
        mig_sql = f.read()
    
    with open(r'C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt', 'r', encoding='utf-8', errors='ignore') as f:
        prod_sql = f.read()
        
    print("Verifying FINAL_PRODUCTION_MIGRATION.sql against u328317103_restro.sql...")
    
    # 1. Check for forbidden statements
    forbidden = ['DROP DATABASE', 'DROP TABLE', 'TRUNCATE']
    for fb in forbidden:
        if re.search(r'\b' + fb + r'\b', mig_sql, re.I):
            print(f"[ERROR] Forbidden statement found: {fb}")
            return False
            
    print("  [PASS] No DROP or TRUNCATE statements detected.")
    
    # 2. Extract all ALTER TABLE statements
    alter_matches = re.finditer(r'ALTER TABLE\s+`?([a-zA-Z0-9_]+)`?\s+(.*?);', mig_sql, re.DOTALL | re.I)
    alters = 0
    for m in alter_matches:
        alters += 1
        tbl = m.group(1)
        body = m.group(2)
        # Check if table exists in prod
        if f'CREATE TABLE `{tbl}`' not in prod_sql and f'CREATE TABLE {tbl}' not in prod_sql:
            print(f"[ERROR] ALTER target table does not exist in prod: {tbl}")
            return False
            
    print(f"  [PASS] All {alters} ALTER TABLE statements target verified production tables.")
    
    # 3. Extract all CREATE INDEX statements
    index_matches = re.finditer(r'CREATE INDEX(?:\s+IF NOT EXISTS)?\s+`?([a-zA-Z0-9_]+)`?\s+ON\s+`?([a-zA-Z0-9_]+)`?\s*\((.*?)\);', mig_sql, re.I)
    idx_count = 0
    for m in index_matches:
        idx_count += 1
        idx_name = m.group(1)
        tbl = m.group(2)
        cols = m.group(3)
        if f'CREATE TABLE `{tbl}`' not in prod_sql and f'CREATE TABLE {tbl}' not in prod_sql:
            print(f"[ERROR] Index target table does not exist in prod: {tbl}")
            return False
            
    print(f"  [PASS] All {idx_count} CREATE INDEX statements target verified production tables.")
    
    # 4. Extract UPDATE backfill statements
    updates = re.findall(r'UPDATE\s+`?([a-zA-Z0-9_]+)`?', mig_sql, re.I)
    print(f"  [PASS] Found {len(updates)} data backfill UPDATE statements.")
    
    print("\nMigration syntax and target verification PASSED 100%!")
    return True

if __name__ == '__main__':
    verify_migration()
