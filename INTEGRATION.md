# Reviews & Ratings + Admin Tools: integration notes

Copy `src/` over the repo's `src/` (paths match the existing package layout).

## New files

| Package / folder | Files |
|---|---|
| `review` | `ReviewType`, `Review`, `ReviewView`, `RatingSummary`, `CompletedRental`, `PendingReview`, `ReviewForm`, `ReviewNotAllowedException`, `RentalLookupDAO`, `ReviewDAO`, `ReviewService`, `ReviewController` |
| `admin` | `AdminActionType`, `AdminActionException`, `AdminUserView`, `AdminListingView`, `AdminActionView`, `AdminDAO`, `AdminService`, `AdminController`, `AdminAccountInitializer` |
| `config` | `ActiveAccountInterceptor`, `WebConfig`, `DemoDataInitializer` (demo profile only) |
| templates | `my-reviews`, `review-form`, `listing-reviews`, `user-reviews`, `admin/dashboard`, `admin/users`, `admin/user-detail`, `admin/listings`, `admin/actions` |
| tests | `ReviewServiceTest`, `RatingSummaryTest`, `ReviewDAOTest`, `AdminServiceTest`, `AdminDAOTest` |

## Changed existing files (small, additive edits)

- `schema.sql`: adds `rental_requests`, `reviews`, `admin_actions` tables.
- `UserDAO` / `UserService`: add `findById`. **`GearListingController` already calls `userService.findById`, which didn't exist, so the project didn't compile before this.**
- `GearListingController`: injects `ReviewService`; adds ratings to listing detail and My Gear.
- `listing-detail.html`: owner rating replaces hard-coded "No ratings yet"; gear reviews section.
- `my-gear.html`: per-listing rating + link to reviews.
- `profile.html`: links to My Gear, My reviews, ratings received, admin area (admins only).
- `login.html`: message for `?deactivated`.
- `application.properties`: team settings + admin account settings.

`SecurityConfig` is unchanged: `/admin/**` already requires `ROLE_ADMIN`.

## application.properties

Full file is in `src/main/resources/application.properties` (team's H2 settings +
`gearhub.admin.*`). Default admin login: `admin@gearhub.local` / `ChangeMe-Admin1`
(override with the `GEARHUB_ADMIN_PASSWORD` environment variable).

## Required pom.xml dependencies

Check these are present (most already are, since the team's code uses them):

```xml
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-jdbc</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
<dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
```

## Running

```bash
mvn clean test                                        # build + all tests
mvn spring-boot:run                                   # normal run
mvn spring-boot:run -Dspring-boot.run.profiles=demo   # run with demo data
```

App: http://localhost:8080

## Dependency on the Booking stories (GL-8 to GL-10)

Reviews need completed rentals. `rental_requests` in `schema.sql` follows the team's
GL-8 design. A rental counts as **completed** when its status is `ACCEPTED` or
`COMPLETED` and `end_date` is before today. Agree this with whoever owns booking;
if they rename columns/statuses only `RentalLookupDAO`, `AdminDAO` and
`DemoDataInitializer` need updating.

Until booking is built, run with the `demo` profile to get sample data
(see "Running" below).

## Story coverage

| Story | Where |
|---|---|
| #10 Renter rates/reviews gear | `/reviews` → "Rate the gear" → `review-form` (type `GEAR`) |
| #11 Renter rates owner | `/reviews` → "Rate the owner" (type `OWNER`) |
| #12 Renter sees gear + owner ratings on listing | `listing-detail.html` (gear average, gear reviews, owner rating linking to `/reviews/users/{id}`) |
| #13 Owner rates renter | `/reviews` → "Rate the renter" (type `RENTER`); visible to other owners at `/reviews/users/{id}` |
| #14 Owner sees ratings for their equipment | My Gear rating per listing → `/reviews/listings/{id}` (works for expired/removed listings too, owner only) |
| Admin: view/manage users | `/admin/users` (search name/email, filter role/status), `/admin/users/{id}` |
| Admin: deactivate users | Reason required; blocks login, signs out active sessions, removes published listings, closes pending requests |
| Admin: view/manage/remove listings | `/admin/listings`; remove / restore with reason (restore → Expired if past expiry) |
| Audit trail | `/admin/actions`, and per-user history on the user detail page |

Rules enforced server-side: only completed rentals, only the parties to the rental,
one review per type per rental (DB unique constraint + service check), rating 1–5,
comment ≤ 1000 chars; admins can't deactivate themselves or other admins.
