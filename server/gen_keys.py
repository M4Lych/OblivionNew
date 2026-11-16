#!/usr/bin/env python3
"""Creates signing_key.pem (ECDSA P-256) and prints the public key to paste into the app
(res/values/oblivion_config.xml -> oblivion_server_pubkey)."""
import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

key = ec.generate_private_key(ec.SECP256R1())
open("signing_key.pem", "wb").write(key.private_bytes(
    serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
pub = key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
print("oblivion_server_pubkey =", base64.b64encode(pub).decode())
