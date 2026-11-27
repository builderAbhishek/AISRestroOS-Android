import urllib.request

url = "https://archive.mariadb.org/mariadb-11.8.1/winx64-packages/mariadb-11.8.1-winx64.zip"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'}, method='HEAD')
with urllib.request.urlopen(req) as r:
    size = int(r.headers.get('Content-Length', 0))
    print(f"Size: {size / (1024*1024):.2f} MB")
