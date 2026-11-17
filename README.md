# Campus Carpooling

Java (OOP) backend using only the JDK, with a vanilla JS frontend.

## Run
Needs JDK 17 or newer.

    javac -d out $(find src -name "*.java")      # Linux / macOS
    java -cp out carpool.Main

On Windows PowerShell:

    javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
    java -cp out carpool.Main

Open http://localhost:8080. Run it from the project folder so `web/` and `data/` resolve.

## Packages
- model: User (Driver, Passenger), Vehicle (Car, Bike), Ride, Booking
- fare: FareCalculator interface with CarFare and BikeFare
- exception: NoSeatsAvailableException
- service: RideService holds the ArrayLists and saves them to data/carpool.dat
