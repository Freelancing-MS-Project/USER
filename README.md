# Microservice USER - Projet Microservices Prospera

Ce microservice gere les utilisateurs de la plateforme Prospera. Il expose les API d'inscription, d'authentification, de gestion du profil utilisateur, de gestion administrative des utilisateurs et de verification faciale via un sous-service Python FastAPI.

Le service principal est une application Spring Boot. Le module `face-id-service` est un microservice Python separe utilise pour comparer une image envoyee avec l'image de reference stockee dans la base de donnees du service USER.

## Sommaire

- [Fonctionnalites](#fonctionnalites)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Structure du projet](#structure-du-projet)
- [Configuration](#configuration)
- [Lancement en local](#lancement-en-local)
- [Lancement avec Docker Compose](#lancement-avec-docker-compose)
- [API REST](#api-rest)
- [Authentification JWT](#authentification-jwt)
- [Face ID Service](#face-id-service)
- [Base de donnees](#base-de-donnees)
- [Tests](#tests)
- [Commandes utiles](#commandes-utiles)
- [Problemes frequents](#problemes-frequents)

## Fonctionnalites

- Inscription d'un utilisateur avec ou sans image de profil.
- Connexion classique par email et mot de passe.
- Connexion par reconnaissance faciale.
- Generation d'un token JWT apres authentification.
- Recuperation de l'utilisateur connecte.
- Mise a jour du profil de l'utilisateur connecte.
- Gestion administrative des utilisateurs.
- Stockage de l'image utilisateur en base de donnees sous forme de `LONGBLOB`.
- Verification d'une image envoyee contre l'image de reference d'un utilisateur.
- Integration avec Eureka Discovery Server.
- Configuration optionnelle via Spring Cloud Config Server.
- Integration Docker Compose avec MySQL, Eureka, Config Server, API Gateway, Keycloak et le service Face ID.

## Architecture

```text
FrontEnd / API Gateway
        |
        v
Microservice USER - Spring Boot
        |
        |-- MySQL : stockage utilisateurs + image de reference
        |
        |-- Eureka : enregistrement du microservice
        |
        |-- Config Server : configuration optionnelle
        |
        v
Face ID Service - FastAPI
        |
        |-- Appelle USER pour recuperer l'image de reference
        |-- Compare l'image de reference avec l'image envoyee
```

Flux de verification faciale :

1. Le client envoie une image au microservice USER.
2. USER transmet l'image au service Python `face-id-service`.
3. `face-id-service` recupere l'image de reference depuis `GET /api/users/{id}/image`.
4. `face-id-service` detecte et encode les visages.
5. Le resultat est renvoye sous forme de `match`, `confidence` ou `error`.

## Technologies

### Microservice USER

- Java 17
- Spring Boot 4.0.2
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Cloud Config Client
- Spring Cloud Netflix Eureka Client
- MySQL
- H2, present comme dependance runtime
- JWT avec `jjwt`
- Maven Wrapper
- Docker

### Face ID Service

- Python 3.11
- FastAPI
- Uvicorn
- `face_recognition`
- `requests`
- Docker

## Structure du projet

```text
USER/
+-- Dockerfile
+-- README.md
+-- pom.xml
+-- mvnw
+-- mvnw.cmd
+-- requirements.txt
+-- src/
|   +-- main/
|   |   +-- java/tn/esprit/twin/projet_micro_user_yahya/
|   |   |   +-- Controllers/
|   |   |   |   +-- AuthController.java
|   |   |   |   +-- UserController.java
|   |   |   +-- DTO/
|   |   |   +-- Entities/
|   |   |   |   +-- Role.java
|   |   |   |   +-- User.java
|   |   |   +-- Exceptions/
|   |   |   +-- Repositories/
|   |   |   +-- Security/
|   |   |   +-- Services/
|   |   |   +-- ProjetMicroUserYahyaApplication.java
|   |   +-- resources/
|   |       +-- application.properties
|   +-- test/
+-- face-id-service/
    +-- Dockerfile
    +-- README.md
    +-- app.py
    +-- requirements.txt
```

## Configuration

La configuration principale est dans :

```text
src/main/resources/application.properties
```

Configuration locale actuelle :

```properties
spring.application.name=ProjetMicroUseryahya
server.port=8085
server.servlet.context-path=/ProjetMicroUseryahya

spring.datasource.url=jdbc:mysql://localhost:3306/freelancers?createDatabaseIfNotExist=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

app.jwt.secret=change-this-secret-key-with-at-least-32-characters
app.jwt.expiration-ms=86400000

spring.cloud.config.enabled=true
spring.cloud.config.fail-fast=false
spring.config.import=optional:configserver:http://localhost:8888

spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

face.id.service.url=http://localhost:5000/verify
```

Variables importantes :

| Propriete | Description | Exemple local | Exemple Docker |
| --- | --- | --- | --- |
| `server.port` | Port interne du service Spring Boot | `8085` | `8090` |
| `server.servlet.context-path` | Prefixe de toutes les routes | `/ProjetMicroUseryahya` | `/ProjetMicroUseryahya` |
| `spring.datasource.url` | URL JDBC MySQL | `jdbc:mysql://localhost:3306/freelancers` | `jdbc:mysql://db-mysql:3306/user_db` |
| `spring.datasource.username` | Utilisateur MySQL | `root` | `root` |
| `spring.datasource.password` | Mot de passe MySQL | vide | `root` |
| `app.jwt.secret` | Cle de signature JWT | 32 caracteres minimum | a definir en production |
| `app.jwt.expiration-ms` | Duree de validite du JWT | `86400000` | `86400000` |
| `face.id.service.url` / `FACE_ID_SERVICE_URL` | URL de verification Face ID | `http://localhost:5000/verify` | `http://faceidcontainer:5000/verify` |

## Lancement en local

### Prerequis

- Java 17
- Maven ou Maven Wrapper fourni dans le projet
- MySQL
- Python 3.11 pour le service Face ID

### 1. Preparer MySQL

Creer ou laisser Spring creer automatiquement la base via `createDatabaseIfNotExist=true`.

Configuration attendue par defaut :

```text
Host     : localhost
Port     : 3306
Database : freelancers
User     : root
Password : vide
```

Si votre MySQL utilise un mot de passe, modifiez :

```properties
spring.datasource.password=votre_mot_de_passe
```

### 2. Lancer le service Face ID

```bash
cd face-id-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 5000 --reload
```

Swagger Face ID :

```text
http://localhost:5000/docs
```

Health check :

```text
http://localhost:5000/health
```

### 3. Lancer le microservice USER

Depuis le dossier `USER` :

```bash
./mvnw spring-boot:run
```

Sur Windows :

```powershell
.\mvnw.cmd spring-boot:run
```

URL locale du service :

```text
http://localhost:8085/ProjetMicroUseryahya
```

## Lancement avec Docker Compose

Le fichier `docker-compose.yml` se trouve dans le dossier parent :

```text
MicroService_Projet/docker-compose.yml
```

Depuis le dossier parent `MicroService_Projet` :

```bash
docker compose up --build user-service face-id-service db-mysql eureka-service server-config
```

Pour lancer toute la plateforme :

```bash
docker compose up --build
```

Dans Docker Compose, le service USER est configure ainsi :

```text
Nom du service      : user-service
Container           : usercontainer
Port interne        : 8090
Port expose machine : 8091
URL machine         : http://localhost:8091/ProjetMicroUseryahya
URL interne Docker  : http://usercontainer:8090/ProjetMicroUseryahya
```

Le service Face ID est configure ainsi :

```text
Nom du service      : face-id-service
Container           : faceidcontainer
Port                : 5000
URL machine         : http://localhost:5000
URL interne Docker  : http://faceidcontainer:5000
```

Attention : le `Dockerfile` du microservice USER copie `target/*.jar`. Il faut donc generer le JAR avant de construire l'image si le build Docker n'execute pas Maven :

```bash
cd USER
./mvnw clean package
cd ..
docker compose up --build user-service
```

## API REST

En local, la base URL est :

```text
http://localhost:8085/ProjetMicroUseryahya
```

Avec Docker Compose, la base URL depuis la machine hote est :

```text
http://localhost:8091/ProjetMicroUseryahya
```

### Inscription sans image

```http
POST /api/users/register
Content-Type: application/json
```

Body :

```json
{
  "email": "client@example.com",
  "password": "password123",
  "firstName": "Yahya",
  "lastName": "Ben Ali",
  "cin": "12345678",
  "role": "Client"
}
```

Roles acceptes :

```text
Client
Freelancer
Admin
```

Exemple `curl` :

```bash
curl -X POST http://localhost:8085/ProjetMicroUseryahya/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "client@example.com",
    "password": "password123",
    "firstName": "Yahya",
    "lastName": "Ben Ali",
    "cin": "12345678",
    "role": "Client"
  }'
```

### Inscription avec image

```http
POST /api/users/register
Content-Type: multipart/form-data
```

Champs `form-data` :

| Champ | Type | Obligatoire | Description |
| --- | --- | --- | --- |
| `file` | File | oui | Image de reference de l'utilisateur |
| `email` | Text | oui | Email unique |
| `password` | Text | oui | Mot de passe |
| `firstName` | Text | non | Prenom |
| `lastName` | Text | non | Nom |
| `cin` | Text | non | CIN unique |
| `role` | Text | non | `Client`, `Freelancer` ou `Admin` |

Exemple :

```bash
curl -X POST http://localhost:8085/ProjetMicroUseryahya/api/users/register \
  -F "file=@./yahya.jpg" \
  -F "email=client-image@example.com" \
  -F "password=password123" \
  -F "firstName=Yahya" \
  -F "lastName=Ben Ali" \
  -F "cin=87654321" \
  -F "role=Client"
```

### Connexion par email et mot de passe

```http
POST /api/auth/login
Content-Type: application/json
```

Body :

```json
{
  "email": "client@example.com",
  "password": "password123"
}
```

Reponse :

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "email": "client@example.com",
  "role": "Client"
}
```

Exemple :

```bash
curl -X POST http://localhost:8085/ProjetMicroUseryahya/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"client@example.com","password":"password123"}'
```

### Connexion par reconnaissance faciale

```http
POST /api/auth/face-login
Content-Type: multipart/form-data
```

Champs :

| Champ | Type | Obligatoire | Description |
| --- | --- | --- | --- |
| `file` | File | oui | Image a comparer |
| `tolerance` | Text | non | Seuil entre `0` et `1`, par defaut `0.6` |

Exemple :

```bash
curl -X POST http://localhost:8085/ProjetMicroUseryahya/api/auth/face-login \
  -F "file=@./yahya.jpg" \
  -F "tolerance=0.6"
```

Reponse :

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "email": "client@example.com",
  "role": "Client",
  "userId": 1,
  "confidence": 0.87
}
```

### Recuperer la session courante

```http
GET /api/auth/session
Authorization: Bearer <token>
```

Exemple :

```bash
curl http://localhost:8085/ProjetMicroUseryahya/api/auth/session \
  -H "Authorization: Bearer <token>"
```

### Extraire l'ID utilisateur depuis un token

Deux formats sont acceptes.

Avec header :

```bash
curl http://localhost:8085/ProjetMicroUseryahya/api/auth/getUserConnecteById \
  -H "Authorization: Bearer <token>"
```

Avec query parameter :

```bash
curl "http://localhost:8085/ProjetMicroUseryahya/api/auth/getUserConnecteById?token=<token>"
```

Reponse :

```json
{
  "userId": 1
}
```

### Profil de l'utilisateur connecte

```http
GET /api/users/me
Authorization: Bearer <token>
```

Exemple :

```bash
curl http://localhost:8085/ProjetMicroUseryahya/api/users/me \
  -H "Authorization: Bearer <token>"
```

### Modifier le profil de l'utilisateur connecte

```http
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

Body :

```json
{
  "firstName": "Yahya",
  "lastName": "Updated",
  "cin": "11223344"
}
```

Exemple :

```bash
curl -X PUT http://localhost:8085/ProjetMicroUseryahya/api/users/me \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Yahya","lastName":"Updated","cin":"11223344"}'
```

### Recuperer l'image de reference d'un utilisateur

Cette route est publique pour permettre au service Face ID de recuperer l'image.

```http
GET /api/users/{id}/image
```

Exemple :

```bash
curl http://localhost:8085/ProjetMicroUseryahya/api/users/1/image --output user-image.jpg
```

### Verifier le visage d'un utilisateur precis

Cette route appelle le service `face-id-service`.

```http
POST /api/users/verify
Content-Type: multipart/form-data
```

Champs :

| Champ | Type | Obligatoire | Description |
| --- | --- | --- | --- |
| `userId` | Text | oui | ID de l'utilisateur |
| `file` | File | oui | Image a comparer avec l'image de reference |

Exemple :

```bash
curl -X POST http://localhost:8085/ProjetMicroUseryahya/api/users/verify \
  -F "userId=1" \
  -F "file=@./yahya.jpg"
```

Reponse si correspondance :

```json
{
  "match": true,
  "confidence": 0.87,
  "error": null
}
```

Reponse si pas de correspondance :

```json
{
  "match": false,
  "confidence": 0.42,
  "error": null
}
```

### Endpoints administrateur

Ces endpoints necessitent un utilisateur avec le role `Admin`.

```http
POST /api/users
GET /api/users
GET /api/users/{id}
PUT /api/users/{id}
DELETE /api/users/{id}
```

Exemple avec JWT admin :

```bash
curl http://localhost:8085/ProjetMicroUseryahya/api/users \
  -H "Authorization: Bearer <admin-token>"
```

## Authentification JWT

Apres connexion, le service retourne un token JWT. Les routes protegees doivent etre appelees avec :

```http
Authorization: Bearer <token>
```

Le token contient notamment :

- `userId`
- `email`
- `role`
- `sub`
- `iat`
- `exp`

La duree de validite par defaut est configuree par :

```properties
app.jwt.expiration-ms=86400000
```

Soit 24 heures.

## Face ID Service

Le sous-service `face-id-service` expose :

```http
GET /health
POST /verify
```

URL locale :

```text
http://localhost:5000
```

Swagger :

```text
http://localhost:5000/docs
```

Variables d'environnement du service Face ID :

| Variable | Description | Valeur par defaut |
| --- | --- | --- |
| `USER_SERVICE_BASE_URL` | Base URL du microservice USER | `http://localhost:8090/ProjetMicroUseryahya` |
| `FACE_ID_TOLERANCE` | Seuil de comparaison faciale | `0.6` |
| `USER_SERVICE_TIMEOUT_SECONDS` | Timeout HTTP vers USER | `10` |
| `LOG_LEVEL` | Niveau de logs | `INFO` |

Exemple d'appel direct au service Face ID :

```bash
curl -X POST http://localhost:5000/verify \
  -F "userId=1" \
  -F "file=@./yahya.jpg" \
  -F "tolerance=0.6"
```

## Base de donnees

Entite principale : `User`.

Champs principaux :

| Champ | Description |
| --- | --- |
| `id` | Identifiant auto-genere |
| `email` | Email unique et obligatoire |
| `password` | Mot de passe encode avec BCrypt |
| `firstName` | Prenom |
| `lastName` | Nom |
| `cin` | CIN unique |
| `role` | `Client`, `Freelancer` ou `Admin` |
| `userImage` | Image stockee en `LONGBLOB` |
| `userImageContentType` | Type MIME de l'image |
| `createdAt` | Date de creation |
| `updatedAt` | Date de mise a jour |

La strategie JPA actuelle est :

```properties
spring.jpa.hibernate.ddl-auto=update
```

Cela permet a Hibernate de mettre a jour le schema automatiquement pendant le developpement.

## Tests

Lancer les tests :

```bash
./mvnw test
```

Compiler et generer le JAR :

```bash
./mvnw clean package
```

Le JAR genere se trouve dans :

```text
target/
```

## Commandes utiles

Lancer USER en local :

```bash
./mvnw spring-boot:run
```

Lancer Face ID en local :

```bash
cd face-id-service
uvicorn app:app --host 0.0.0.0 --port 5000 --reload
```

Construire le JAR :

```bash
./mvnw clean package
```

Construire l'image Docker USER :

```bash
docker build -t user:1.0 .
```

Construire l'image Docker Face ID :

```bash
cd face-id-service
docker build -t faceid:1.0 .
```

Lancer avec Docker Compose depuis le dossier parent :

```bash
docker compose up --build user-service face-id-service db-mysql eureka-service server-config
```

Arreter les conteneurs :

```bash
docker compose down
```

Arreter les conteneurs et supprimer les volumes :

```bash
docker compose down -v
```

## Problemes frequents

### Le service USER ne se connecte pas a MySQL

Verifier :

- MySQL est demarre.
- Le port `3306` est disponible en local.
- Le mot de passe dans `application.properties` correspond a votre installation.
- En Docker Compose, l'URL doit utiliser `db-mysql:3306` et non `localhost:3306`.

### `Face ID service is unreachable`

Verifier :

- Le service Face ID est demarre sur le port `5000`.
- `face.id.service.url` vaut `http://localhost:5000/verify` en local.
- En Docker Compose, `FACE_ID_SERVICE_URL` vaut `http://faceidcontainer:5000/verify`.

### `Reference image not found`

Cela signifie que l'utilisateur existe mais n'a pas d'image de reference stockee, ou que l'ID envoye n'existe pas.

Corriger en inscrivant l'utilisateur avec une image :

```bash
curl -X POST http://localhost:8085/ProjetMicroUseryahya/api/users/register \
  -F "file=@./yahya.jpg" \
  -F "email=client-image@example.com" \
  -F "password=password123" \
  -F "role=Client"
```

### Erreur sur la taille du fichier

En local, la limite actuelle est :

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

En Docker Compose, elle est surchargee a :

```text
25MB
```

### Le service Docker USER expose un port different du local

Local :

```text
http://localhost:8085/ProjetMicroUseryahya
```

Docker Compose :

```text
http://localhost:8091/ProjetMicroUseryahya
```

Dans le conteneur, Spring ecoute sur `8090`, puis Docker publie ce port sur `8091`.

## Notes de securite

- Remplacer `app.jwt.secret` avant un deploiement reel.
- Eviter d'utiliser le compte MySQL `root` en production.
- Ne pas laisser `spring.jpa.hibernate.ddl-auto=update` en production sans strategie de migration controlee.
- Limiter l'acces public a `GET /api/users/{id}/image` si le service Face ID est remplace par une communication interne securisee.
- Stocker les secrets dans des variables d'environnement ou un gestionnaire de secrets.
