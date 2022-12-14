# car-go-backend
Project for BYT

`docker run -p 8081:8081 s22630/car-go-backend:latest` run the app

Generate new key pair
```bash
openssl genpkey -algorithm RSA -out rsa_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in rsa_private.pem -pubout -out rsa_public.pem
```