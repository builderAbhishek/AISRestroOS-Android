with open(r'C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

import re
m = re.search(r'CREATE TABLE `order_items`\s*\((.*?)\)\s*ENGINE', text, re.DOTALL)
if m:
    for line in m.group(1).split('\n'):
        print(line.strip())
else:
    print("Not found")
