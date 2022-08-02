# 🪨🔙🔚 bulk-backend 

Dette prosjektet representerer backenden for bulk-uttrekk av kontaktinformasjon fra KRR.

## 🔬 Utvikling

### ♻️ Miljøvariabler

Dette prosjektet krever Java 17 eller høyere installert og bruker ellers Kotlin og Gradle.

For å kjøre prosjektet må man lage en `.env` fil så samme form som .env.example i roten av prosjektet.
Her må man legge inn AZURE_APP_JWK
De andre variablene: ENVIRONMENT, AZURE_OPENID_CONFIG_TOKEN_ENDPOINT, AZURE_APP_WELL_KNOWN_URL og FRONTEND_CLIENT_ID kan
kopieres direkte fra .env.example.
Disse er nødvendige for å koble opp mot [digdir-krr](https://github.com/navikt/digdir-krr), også i dev miljø og testing.

Verdien til miljøvariablene AZURE_APP_CLIENT_SECRET finnes i appinformasjonen fra K8s clusteret.
De kan man finne med følgende kommandoer.

1. Vær sikker på at du er logget inn i det rette clusteret

```bash
gcloud auth login
kubectl config set-context dev-gcp
```

2. Finn navnet (og hashen) til podden bulk-backend kjører på

```bash
kubectl get pods -n team-bulk
```

Dette returnerer en tabell med pods til team-bulk.
Vi er interessert verdien under kolonnen `NAME` på formen `bulk-backend-XXXXXXXXXXX-XXXXX`. Der X-ene representerer en
unik hash for denne instansen av podden.

3. Finn miljøvarablene til bulk-backend i clusteret ved å sette inn verdien for bulk-backend-XXXXXXXXX-XXXXX funnet i
   steg 2.

```bash
kubectl exec bulk-backend-XXXXXXXXXX-XXXXX -n team-bulk -- env | grep AZURE_APP
```

Dette vil returnere alle miljøvariablene for podden bulk-backend kjører på som inneholder strengen AZURE_APP.
Her kan du finne AZURE_APP_JWK som kan kopieres inn i `.env` filen.

### 🏎️ Kjøring

Etter å ha satt opp `.env` filen kan man kjøre `bulk-backend` (fra bulk-backend mappen) med

```bash
./gradlew run
```

## 🧪 Testing

Etter å ha satt opp `.env` filen kan man kjøre unit testene i `bulk-backend` (fra bulk-backend mappen) med

```bash
./gradlew test
```

Som sagt, kjører dette bare Unit testene. For å kjøre integration-testene, bruker man

```bash
./gradlew integrationtest
```
