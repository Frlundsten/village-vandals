## 1. Tests First (TDD)

- [x] 1.1 Write a failing integration test asserting `POST /auth/login` returns 404
- [x] 1.2 Write a failing integration test asserting `POST /auth/generateToken` returns 404
- [x] 1.3 Write a failing integration test asserting `POST /user/register` returns 404
- [x] 1.4 Write a failing test asserting `UserService.provisionKeycloakUser()` creates a `User` without a password field
- [x] 1.5 Write a failing test asserting `POST /auth/callback` still provisions a user and returns a JWT (regression)

## 2. Backend — Remove Local Auth Endpoints

- [x] 2.1 Delete `POST /auth/login` method from `AuthController`
- [x] 2.2 Delete `POST /auth/generateToken` method from `AuthController`
- [x] 2.3 Remove `AuthenticationManager` injection from `AuthController`
- [x] 2.4 Remove `SecurityConfig.permitAll()` entry for `/auth/login` and `/user/register`
- [x] 2.5 Remove `POST /user/register` endpoint from `UserController`

## 3. Backend — Remove Local Auth Infrastructure

- [x] 3.1 Delete `UserInfoService` class (implements `UserDetailsService`)
- [x] 3.2 Delete `UserInfoDetails` class
- [x] 3.3 Delete `UserInfoRepository` interface
- [x] 3.4 Delete `UserInfo` record
- [x] 3.5 Remove `PasswordEncoder` bean from `SecurityConfig`
- [x] 3.6 Remove `AuthenticationManager` bean from `SecurityConfig`
- [x] 3.7 Remove `UserInfoService` injection from `SecurityConfig`

## 4. Backend — Clean Up User Entity and Service

- [x] 4.1 Remove `password` field and constructor parameter from `User` entity
- [x] 4.2 Remove `PasswordEncoder` injection from `UserService`
- [x] 4.3 Remove `passwordEncoder.encode()` call from `UserService.newUser()`
- [x] 4.4 Update `UserService.newUser()` to construct `User` without password
- [x] 4.5 Update `UserService.provisionKeycloakUser()` — remove `UserInfo` wrapper, construct `User` directly (since `UserInfo` record is deleted)
- [x] 4.6 Remove `AuthRequest` record (was only used by local login endpoints)

## 5. Database Migration

- [x] 5.1 Add a Liquibase changeset in `changelog-master.yaml` to drop the `password` column from the `users` table

## 6. Frontend — Remove Dead Views and Routes

- [x] 6.1 Delete `RegisterView.vue`
- [x] 6.2 Delete `LoginView.vue`
- [x] 6.3 Remove `/register` route from `frontend/src/router/index.js`
- [x] 6.4 Remove `RegisterView` import from router

## 7. Verify Tests Pass

- [x] 7.1 Run `mvn test` — all backend tests green
- [x] 7.2 Run `npm run test:unit` — all frontend tests green
- [x] 7.3 Confirm the three TDD tests from step 1 now pass
