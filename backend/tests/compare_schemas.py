import re
import json

def parse_sql(filepath):
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        text = f.read()
    
    tables = {}
    idx = 0
    while True:
        pos = text.lower().find('create table', idx)
        if pos == -1:
            break
        # find table name
        start_paren = text.find('(', pos)
        header = text[pos:start_paren]
        parts = header.strip().split()
        tname = parts[-1].strip('`" \r\n\t')
        
        # find matching closing paren
        depth = 0
        end_paren = -1
        for i in range(start_paren, len(text)):
            c = text[i]
            if c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    end_paren = i
                    break
        if end_paren == -1:
            break
            
        body = text[start_paren+1:end_paren]
        cols = {}
        indexes = []
        for line in body.split('\n'):
            line = line.strip().rstrip(',')
            if not line:
                continue
            upper = line.upper()
            if any(upper.startswith(k) for k in ['PRIMARY KEY', 'KEY', 'UNIQUE KEY', 'CONSTRAINT', 'FULLTEXT']):
                indexes.append(line)
            else:
                col_parts = line.split(None, 1)
                cname = col_parts[0].strip('`" \r\n\t')
                cdef = col_parts[1] if len(col_parts) > 1 else ''
                cols[cname] = cdef
        tables[tname] = {'cols': cols, 'indexes': indexes}
        idx = end_paren + 1
    return tables

prod_path = r'C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt'
base_path = r'C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437929084_fcbeb1e8.txt'

prod = parse_sql(prod_path)
base = parse_sql(base_path)

print(f"Production tables ({len(prod)}): {sorted(list(prod.keys()))}")
print(f"Baseline tables ({len(base)}): {sorted(list(base.keys()))}")

all_tables = sorted(set(list(prod.keys()) + list(base.keys())))
print("\n================== TABLE-BY-TABLE COMPARISON ==================")
for t in all_tables:
    p = prod.get(t)
    b = base.get(t)
    if not p:
        print(f"\n[TABLE ONLY IN BASELINE] {t}")
        continue
    if not b:
        print(f"\n[TABLE ONLY IN PROD] {t} ({len(p['cols'])} columns)")
        continue
    p_cols = set(p['cols'].keys())
    b_cols = set(b['cols'].keys())
    diff_p = p_cols - b_cols
    diff_b = b_cols - p_cols
    same = p_cols & b_cols
    
    print(f"\n=== TABLE: {t} ===")
    if diff_p:
        print(f"  [Columns in Prod only]: {diff_p}")
        for c in diff_p:
            print(f"     prod {c}: {p['cols'][c]}")
    if diff_b:
        print(f"  [Columns in Baseline only]: {diff_b}")
        for c in diff_b:
            print(f"     base {c}: {b['cols'][c]}")
    
    type_diffs = []
    for c in same:
        p_type = p['cols'][c].lower()
        b_type = b['cols'][c].lower()
        if p_type.split()[0] != b_type.split()[0]:
            type_diffs.append((c, p['cols'][c], b['cols'][c]))
    if type_diffs:
        print(f"  [Type differences in common columns]:")
        for c, pt, bt in type_diffs:
            print(f"     {c} -> Prod: {pt} | Base: {bt}")
