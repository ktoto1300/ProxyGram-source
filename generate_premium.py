import hashlib

def generate_token(user_id):
    secret = "mod_secret"
    data = f"{user_id}{secret}"
    signature = hashlib.sha256(data.encode()).hexdigest()
    return f"{user_id}.{signature}"

if __name__ == "__main__":
    uid = input("Введите Telegram User ID: ")
    token = generate_token(uid)
    print(f"\nВаш Premium Token для {uid}:")
    print(f"\033[92m{token}\033[0m")
    print("\nСкопируйте этот токен и введите его в 'Mod Settings' -> 'Premium Mod'")
