# Task 2 — Extend ResourceController to include production rates

Change `GET /resources/refresh` to return `ResourceStorageResponse` instead of `Map<String, Integer>`.
Fetch `village.getProduction()` via the service and populate the perHour fields.
Write a MockMvc integration test verifying all 8 fields appear in the JSON response.
