import urllib.request
import zipfile
import os
import sys

dest_dir = os.path.join(os.environ.get('TEMP', 'C:\\Temp'), 'mariadb_11_8')
zip_path = os.path.join(dest_dir, 'mariadb.zip')
os.makedirs(dest_dir, exist_ok=True)

url = "https://archive.mariadb.org/mariadb-11.8.1/winx64-packages/mariadb-11.8.1-winx64.zip"

if not os.path.exists(os.path.join(dest_dir, 'bin', 'mariadbd.exe')):
    if not os.path.exists(zip_path):
        print(f"Downloading MariaDB 11.8 (88 MB) to {zip_path}...")
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as resp, open(zip_path, 'wb') as out:
            chunk_size = 1024 * 1024
            downloaded = 0
            while True:
                chunk = resp.read(chunk_size)
                if not chunk:
                    break
                out.write(chunk)
                downloaded += len(chunk)
                sys.stdout.write(f"\rDownloaded {downloaded / (1024*1024):.1f} MB")
                sys.stdout.flush()
        print("\nDownload complete.")
    
    print("Extracting zip...")
    with zipfile.ZipFile(zip_path, 'r') as zf:
        # Find top level folder inside zip
        names = zf.namelist()
        top_prefix = names[0].split('/')[0] + '/'
        for member in zf.infolist():
            if member.filename.startswith(top_prefix):
                rel_path = member.filename[len(top_prefix):]
                if not rel_path:
                    continue
                target_path = os.path.join(dest_dir, rel_path)
                if member.is_dir():
                    os.makedirs(target_path, exist_ok=True)
                else:
                    os.makedirs(os.path.dirname(target_path), exist_ok=True)
                    with zf.open(member) as source, open(target_path, 'wb') as target:
                        target.write(source.read())
    print("Extraction complete.")
else:
    print("MariaDB 11.8 already present.")

print("MariaDB location:", dest_dir)
