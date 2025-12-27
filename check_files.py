import os

root = r"C:\Users\mnxzm\OneDrive\Desktop\DEV\Niusic\app\src\main\kotlin\com\github\niusic"
for dirpath, dirnames, filenames in os.walk(root):
    for f in filenames:
        fp = os.path.join(dirpath, f)
        size = os.path.getsize(fp)
        if size < 150:
            with open(fp, 'r') as file:
                content = file.read()
            print(f"{fp} ({size} bytes):")
            print(content)
            print("-" * 20)
