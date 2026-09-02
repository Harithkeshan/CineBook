# About CineBook

## What is CineBook?

CineBook is a specialized domain-driven backend solution for managing cinema operations, ticket bookings, seat availability, payment processing, ticket issuance, theatre check-in, and administrative scheduling.

It models the operational complexity of real-world cinema chains, offering a clean, robust API architecture for both cinema customers and staff.

## The Problem

Online cinema ticket booking involves complex domain challenges:

1. **Concurrency Control**: Multiple customers attempting to select the exact same seat for a popular showtime at the exact same second.
2. **State Separation**: Distinguishing between physical seats built into a theatre hall versus dynamic seat availability for a specific showtime.
3. **Multi-Seat & Multi-Ticket Granularity**: A single customer booking may contain multiple seats with different pricing tiers (`ADULT`, `CHILD`, `STUDENT`, `SENIOR`), requiring individual tickets for entry check-in.
4. **Financial Policy Enforcement**: Handling cancellations fairly while protecting the cinema from customer misuse and ensuring complete refunds when a show is cancelled by the management.
5. **Guest Checkout Friction**: Allowing customers to quickly book tickets without forcing upfront account creation.

CineBook addresses these requirements through clear entity modeling, database locking, transactional service logic, and explicit business policies.

## How CineBook Works

CineBook models cinema operations across distinct domain hierarchies:

### 1. Physical Infrastructure Model
```text
Cinema Location
    └── Hall
         └── Seating Section
              └── Physical Seat (Row, Seat Number, Layout Coordinates, Seat Type)
```

### 2. Show & Seat Availability Model
```text
Movie
    └── Showtime (Hall, Start Time, End Time)
         └── ShowtimeSeat (Status: AVAILABLE / HELD / BOOKED, Expiration Time)
```

### 3. Booking & Ticket Model
```text
Booking (Customer Details, Reference, Status)
    ├── BookingSeat (Seat, Ticket Type, Price)
    │    └── Ticket (Ticket Number, QR Token, Status, Issued/Used Timestamps)
    ├── Payment (Provider, Amount, Status)
    └── Refund (Amount, Reason, Status, Provider Refund ID)
```

## Important Design Decisions

- **Guest Checkout Support**: Customers can complete bookings by providing name, email, and phone without creating an account. Registered user accounts are also supported for tracking booking history.
- **Physical vs. Showtime Seat Separation**: Physical seats (`Seat`) define the permanent structure of a theatre hall. When a `Showtime` is created, showtime-specific seat records (`ShowtimeSeat`) are automatically generated, decoupling physical room layout from per-show availability.
- **Concurreny-Safe Seat Reservation**: Seats are temporarily held (5-minute window) using DB row-level pessimistic write locking (`PESSIMISTIC_WRITE`) to ensure two users cannot hold or book the same seat concurrently.
- **Individual Ticket Lifecycle**: While a `Booking` groups all seats in a transaction, each seat receives an independent `Ticket` with its own status (`ACTIVE`, `USED`, `CANCELLED`, `EXPIRED`) and unique QR token for theatre entry check-in.
- **Decoupled Payment & Refund Tracking**: Payments track individual payment attempts (`PENDING`, `PAID`, `FAILED`, `REFUNDED`). Refunds are stored in a dedicated `Refund` entity to preserve full financial audit histories.
- **Asymmetric Cancellation Policy**:
  - *Customer Cancellation*: Booking is marked `CANCELLED`, seats are released to `AVAILABLE`, tickets are cancelled, and **no refund** is issued.
  - *Cinema Showtime Cancellation*: Showtime cancellation triggers automatic cancellation of all bookings, releases seats, cancels active tickets, and issues a **full refund** (`SHOW_CANCELLED`) for successful payments while preventing duplicate refunds.
- **Historical Data Preservation**: Booking, payment, ticket, and refund records are never physically deleted from the database. Administrative operations prevent deletion of movies, halls, or locations referenced by historical showtimes or sections (`409 CONFLICT`).
- **Database Schema Migrations**: All database DDL and initial development seed data are version-controlled and executed reproducibly using Flyway migrations.

## Real-World Considerations

CineBook mirrors real-world cinema workflows:
- A cinema chain can set up a new location (e.g., *CineBook Colombo*), add halls, divide halls into seating sections (*Ground*, *VIP Balcony*), and map physical seats with exact X/Y grid positions.
- When an administrator schedules a movie showtime, the system automatically initializes `ShowtimeSeat` availability for all active physical seats in that hall.
- At the theatre, staff scan QR tokens using the check-in API (`POST /api/tickets/check-in/{qrToken}`). The system atomically verifies ticket validity and marks it `USED` with a timestamp, preventing ticket reuse.

## Current Implementation

The current backend implementation includes:
- 14 JPA domain entities and PostgreSQL database schema managed via Flyway.
- 14 Spring Data JPA repositories with custom JPQL queries and pessimistic locking.
- Complete REST APIs across 19 controller classes for public catalog browsing, real-time seat availability, seat holding, guest booking, mock payment, ticket verification, check-in, cancellations, refunds, booking history pagination, and admin configuration.
- 120 automated unit and integration tests passing with 0 failures.

## Future Direction

Planned future enhancements include:
- **Authentication & Authorization**: Integration of Spring Security with JWT tokens for user authentication and role-based endpoints (`ROLE_ADMIN`, `ROLE_CUSTOMER`).
- **Real Payment Integration**: Replacement of the mock payment provider with real gateway integrations (e.g., PayHere).
- **Frontend Web / Mobile App**: Development of an interactive visual seat-selection UI.
- **Containerization & Deployment**: Docker containerization (`Dockerfile`, `docker-compose.yml`) and Cloud deployment.
- **CI/CD & Automation**: Automated build, test, and release pipelines.
- **Customer Notifications**: Automatic email and SMS notifications for booking confirmations and show cancellations.
