# 🪨🔙🔚 bulk-backend

Dette prosjektet representerer backenden for bulk-uttrekk av kontaktinformasjon fra KRR.

## 🔬 Utvikling

### ♻️ Miljøvariabler

Dette prosjektet krever Java 17 eller høyere installert og bruker ellers Kotlin og Gradle.

For å kjøre prosjektet må man lage en `.env` fil så samme form som .env.example i roten av prosjektet.
Her må man legge inn AZURE_APP_CLIENT_ID, AZURE_APP_CLIENT_SECRET, AZURE_APP_JWK
De andre variablene: ENVIRONMENT, AZURE_OPENID_CONFIG_TOKEN_ENDPOINT, AZURE_APP_WELL_KNOWN_URL og JSON_CONFIG kan
kopieres direkte fra .env.example.
Disse er nødvendige for å koble opp mot [digdir-krr](https://github.com/navikt/digdir-krr), også i dev miljø og testing,
og må oppdateres når disse verdiene oppdateres i K8s clusteret.

Verdien til miljøvariablene AZURE_APP_CLIENT_ID, AZURE_APP_CLIENT_SECRET, AZURE_APP_JWK finnes i appinformasjonen fra
K8s clusteret.

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

## Bruk

Bulk-backend er deployet på "https://bulk-backend.dev.intern.nav.no". APIet presenterer endepunktet `/personer`.
Endepunktet er i utgangspunktet ikke åpent for andre applikasjoner enn `bulk-frontend`.

Endepunktet `/personer` tar opptil 2 parametere:

- type (default "json"). Sier hvilket type format responsen skal være på. Tar "csv" eller "json".
- pdl (default "false"). Forteller om man ønsker å ha med navn, adresse og (evt.) dødsdato fra PDL i responsen. Tar "
  true" eller "false".

Bemerkninger:

- Dersom man sender request: `/personer?type=json&pdl=true` vil forespørselen ta betraktelig lengre tid som
  dersom `type=csv`. Dette er blant annet fordi json responsen inneholder mye mer data en csv responen.
- Endepunktet er testet for spørringer med opptil 200 000 personidenter. Ved denne størrelsen tar spørringen rundt 60
  sekunder. Endepunktet kan muligens være ustabilt for forespørseler av denne størrelsen ettersom API-ene vi bruker (KRR
  og PDL) kan knekke ved veldig stor pågang. Det er forsøkt å legge inn et delay for å legge mindre press på KRR og PDL,
  men man burde nok ikke gjøre så store spørringer med korte intervaller.  



