# Chat App

Small TCP chat demo with optional end-to-end encryption. A simple Java server accepts multiple TCP clients, negotiates a shared key per connection using X25519 + HKDF, then relays messages encrypted with AES-GCM. A lightweight set of model classes (`RoomLink`, `ServerLink`, `ServerLink_HTTPS`) sketch future multi-room, multi-protocol support.

## How it works
- Transport: plain TCP on `localhost:9999`.
- Handshake: client sends `DHINIT:<base64(X25519 pub)>`; server replies `DHRESP:<base64(pub)>`. Both sides derive a shared secret with X25519, stretch it with HKDF-SHA256 (`info="chat-app"`) to a 256-bit AES key.
- Message format: unencrypted lines stay unchanged; encrypted lines are prefixed with `ENC:` followed by base64(iv || ciphertext || tag) for AES-GCM (12-byte IV, 128-bit tag).
- Broadcast: the server decrypts messages from a client with that client’s session key, then encrypts per-recipient when their session key is available (falls back to plaintext if a recipient has no key or encryption fails).
- Commands: `/nick <newName>` renames the sender; `/quit` disconnects.

## Components (src/)
- `Server.java`: Listens on 9999, accepts sockets, and spins a `ConnectionHandler` per client. Handles the DH handshake, nickname prompts, command parsing, and broadcast fan-out.
- `Client.java`: Connects to the server, performs the handshake, prompts for a nickname, encrypts outbound messages, and decrypts incoming lines.
- `CryptoUtil.java`: Cryptography helpers (X25519 keygen/derivation, HKDF-SHA256, AES-GCM encode/decode, base64 helpers).
- `RoomLink.java`, `ServerLink.java`, `ServerLink_HTTPS.java`: Early model types for addressing rooms/servers; `ServerLink_HTTPS` validates host strings (domain or IP) and exposes a `HTTPS://` string form.

## Prerequisites
- Java 17+ (required for built-in X25519 support).
- `make` (optional; the provided `makefile` wraps compilation and runs).

## Build and run
```sh
# compile to ./bin
make build

# start the server (listens on 9999)
make run_server

# in another shell, start a client
make run_client
```
Running `Client` prompts for a nickname, then accepts chat input. Type `/quit` to disconnect. You can run multiple clients in separate terminals to see broadcast behavior.

## Security notes
- AES-GCM keys are ephemeral per client connection; there is no identity binding or authentication of public keys, so the handshake is vulnerable to MITM in real deployments.
- Messages fall back to plaintext if a session key is missing or encryption fails for a recipient; this is acceptable for a demo but not for production.
- IVs are randomly generated per message; tags are included in the ciphertext blob.

## Roadmap ideas
- Authenticate public keys (pre-shared fingerprints or a trust-on-first-use key store).
- Persist rooms and introduce room-scoped broadcasts using the `RoomLink` model.
- Add TLS on the transport, graceful shutdown hooks, and automated tests.
