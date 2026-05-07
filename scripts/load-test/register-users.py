#!/usr/bin/env python3
"""Register test users for k6 load testing (registration has no rate limit)."""
import requests, sys, json

COUNT = int(sys.argv[1]) if len(sys.argv) > 1 else 50
BASE = "http://localhost/app/auth/register"

created = 0
for i in range(1, COUNT + 1):
    username = f"lt_u{i:02d}"
    phone = f"1990000{i:04d}"
    payload = {
        "username": username,
        "password": "root",
        "confirmPassword": "root",
        "nickname": f"TestUser{i:02d}",
        "phone": phone,
        "captchaVerification": "bypass",
        "gender": 1
    }
    try:
        resp = requests.post(BASE, json=payload,
            headers={"Host": "app.localhost"}, timeout=10)
        if resp.status_code == 200:
            data = resp.json()
            if data.get("code") == 200:
                created += 1
                print(f"  [{created:2d}/{COUNT}] {username} (phone={phone}) OK")
            else:
                print(f"  [--/{COUNT}] {username} FAIL: {data.get('message','?')}")
        else:
            print(f"  [--/{COUNT}] {username} HTTP {resp.status_code}: {resp.text[:100]}")
    except Exception as e:
        print(f"  [--/{COUNT}] {username} ERROR: {e}")

print(f"\nCreated {created}/{COUNT} users")
