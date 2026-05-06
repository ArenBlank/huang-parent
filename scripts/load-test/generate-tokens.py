#!/usr/bin/env python3
"""Generate test tokens in batches to avoid login rate limit (10/min per IP)."""
import requests, json, time, sys, os

COUNT = int(sys.argv[1]) if len(sys.argv) > 1 else 30
BATCH = 10
WAIT = 65  # seconds between batches (rate limit window = 60s)
BASE = "http://localhost:8081/app/auth/login"
OUTPUT = os.path.join(os.path.dirname(__file__), "data", "test-tokens.json")

os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)

usernames = ["root", "root_member", "case_member"]
for i in range(1, COUNT + 1):
    usernames.append(f"lt_u{i:02d}")
usernames = usernames[:COUNT + 3]  # exact count

tokens = []
for i, username in enumerate(usernames):
    if i > 0 and i % BATCH == 0:
        print(f"  ... rate limit pause ({len(tokens)}/{len(usernames)} tokens, wait {WAIT}s)")
        time.sleep(WAIT)

    try:
        resp = requests.post(BASE, json={
            "account": username, "password": "root",
            "loginType": "password", "captchaVerification": "skip"
        }, timeout=10)

        if resp.status_code == 200:
            data = resp.json()
            if data.get("code") == 200 and data.get("data", {}).get("accessToken"):
                tokens.append(data["data"]["accessToken"])
                print(f"  [{len(tokens):2d}/{len(usernames)}] {username} OK")
            else:
                print(f"  [--/{len(usernames)}] {username} FAIL: {data.get('message', 'unknown')}")
        else:
            print(f"  [--/{len(usernames)}] {username} HTTP{resp.status_code}")
    except Exception as e:
        print(f"  [--/{len(usernames)}] {username} ERROR: {e}")

with open(OUTPUT, "w") as f:
    json.dump(tokens, f)
print(f"\nSaved {len(tokens)} tokens to {OUTPUT}")
