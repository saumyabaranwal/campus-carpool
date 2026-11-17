# Campus Carpooling

A Java-based student ride sharing system built for the **OOPs using Java Lab (PBL)**, B.Tech CSE Sem III, Jaypee Institute of Information Technology, Noida.

Students with a vehicle post rides. Other students search by destination, book a seat and pay only their share of the fare. The backend is plain Java (no libraries or frameworks) and the frontend is vanilla HTML, CSS and JavaScript.

## Features

- Register as a **driver** or **passenger** with student ID, name and mobile number
- Drivers post rides (car or bike) with route, time, seats and total cost, and mark them completed
- Passengers search by destination, book a seat and cancel a booking
- Seats update automatically; a cancelled seat goes back to the ride
- Fare share per seat is calculated by vehicle type
- Booking history, input validation and clash checks
- Sample drivers, passengers, rides and bookings loaded when the server starts (data is kept in memory)

## Run

Requires **JDK 17 or newer**. Run from the project folder.

```bash
# Linux / macOS
javac -d out $(find src -name "*.java")
java -cp out carpool.Main
```

```powershell
# Windows PowerShell
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out carpool.Main
```

Open http://localhost:8080. In VS Code you can also open `src/carpool/Main.java` and press **Run** (Extension Pack for Java).

**Demo logins** (sample data): passenger `2501030004`, name `Isha Verma`, mobile `9810011004`; driver `2501030001`, name `Aarav Mehta`, mobile `9810011001`.
Data is kept in memory, so it resets every time the server restarts.

## Project structure

```
src/carpool/
  Main.java            starts the server and routes requests
  model/               User, Driver, Passenger, Vehicle, Car, Bike, Ride, Booking, Jsonable, Json
  fare/                FareCalculator, CarFare, BikeFare
  exception/           NoSeatsAvailableException
  service/             RideService (rules and ArrayLists), SampleData
web/                   index.html, style.css, app.js
```

## OOP concepts used

| Concept | Where |
|---|---|
| Encapsulation | Private fields in `Ride` and `Booking`; seats change only through `bookSeat()` and `releaseSeat()` |
| Inheritance and abstraction | `Driver` and `Passenger` extend abstract `User`; `Car` and `Bike` extend abstract `Vehicle` |
| Polymorphism and interfaces | `FareCalculator` is implemented by `CarFare` and `BikeFare`; `Jsonable` is implemented by `User`, `Ride` and `Booking` |
| Exception handling | Custom `NoSeatsAvailableException`; `IllegalArgumentException` for invalid input |
| Collections | `ArrayList` of users, rides and bookings in `RideService` |
| Strings | `String` methods for validation; `StringBuilder` in `Json` |
| Packages | `model`, `fare`, `exception`, `service` |

## Fare rule

| Vehicle | Fare per seat |
|---|---|
| Car | total cost divided by (seats + 1), so the driver also pays a share |
| Bike | 60% of the total cost |

## Validation rules

- Student ID: 10 digits; mobile: 10 digits starting with 6 to 9
- Pickup and drop must differ; total cost between ₹10 and ₹5000
- Number plate starts with two letters and ends with four digits, e.g. `UP16AB1234`; cars take 1 to 6 seats, bikes 1
- Departure at least 30 minutes ahead and within 30 days
- No two rides within 60 minutes for the same person; no duplicate booking
- No cancellation within 30 minutes of departure

## Team

Saumya Baranwal, Ammar Rizvi, Moulik Satija, Naman Vaish
Guided by Asmita Yadav and Parul Agarwal
