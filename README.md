# 🕵️ Welcome to **Manhunt!**

## 🎯 Introduction

**Manhunt** is a mobile, multiplayer web application that reimagines classic hide-and-seek for the smartphone era. By
blending real-world movement with battle‐royale–style dynamics, it brings back a nostalgic childhood game in a modern,
enhanced form—making playing outside fun again.

Players join or create a game as either **<span style="color:#722ed1">Hunter</span>** or **<span style="color:#fadb14">
Hider</span>**, then physically move within a geo-fenced play area that dynamically shrinks. Strategic power-ups—*
*Reveal** (briefly expose all players) and **Recenter** (shift the game center)—add tactical depth. All clients remain
tightly synchronized—sharing GPS positions, player statuses, and a server-anchored countdown—to ensure fairness and
eliminate boundary or timing disputes.

**Motivation**

- Rekindle the joy of a nostalgic outdoor game with modern technology
- Solve childhood hide-and-seek frustrations (lost players, boundary disputes, unfair starts)
- Encourage physical activity, social interaction and strategic thinking
- Offer quick, repeatable rounds with clear rules and engaging mechanics

----------

## ⚙️ Technologies Used

- **Foundation:** Java & Spring Boot
- **Data Persistence** : JPA (Hibernate)
- **Build Tool:** Gradle
- **Database:** H2 (in-memory database)
- **API Architecture:** RESTful Web Services
- **Testing:** JUnit, Mockito
- **Deployment:** Google Cloud

---------- 

## 🧩 High-Level Components

1. **Controllers**
    - **Role:** Handle incoming HTTP requests and route them to the appropriate service methods.
    - **Correlations:**
        - Call **Service** methods to perform business logic.
        - Uses **DTO Mappers** to convert between DTOs and entities.
        - Convert entities returns from **Services** in API-responses.
        - Forward errors from **Services** to HTTP-Status codes.
    - **Main File:**
      [GameController](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameController.java)

2. **Services**
    - **Role:** Implement core business logic and game mechanics.
    - **Correlations:**
        - Use **Repositories** to interact with the database.
        - Called by **Controllers** to process requests.
        - Manipulate and transform **Entities** objects based on the business logic.
        - Throw errors that are caught by **Controllers** and converted to HTTP-Status codes.
    - **Main File:**
      [GameService](src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameService.java)

3. **Repositories**
    - **Role:** Provide data access interfaces for the database.
    - **Correlations:**
        - Called by **Services** to persist and retrieve data.
        - Return **Entities** objects to **Services**.
        - Provide query operations called by **Services**.
    - **Main File:**
      [GameRepository](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/GameRepository.java)

4. **Entities**
    - **Role:**  Define the application's data model and its relationships.
    - **Correlations:**
        - Used by **Repositories** for Database queries.
        - Manipulated by **Services** to implement business logic.
        - Transformed by **DTO Mappers** to create Data Transfer Objects for **Controllers**.
        - Include relationships to other **Entities**.
    - **Main File:**
      [Game](src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Game.java)

5. **DTO Mappers**
    - **Role:** Convert between Data Transfer Objects and entities.
    - **Correlations:**
        - Used by **Controllers** to transform incoming requests into entities and vice versa.
        - Allow **Services** to work with entities.
        - Protect **Entities** from direct exposure to the API.
    - **Main File:**
      [DTOMapper](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/mapper/DTOMapper.java)

---------- 

## 🚀 Launch & Deployment

Prerequisites

- Java 17
- Gradle 7.0+
  -**Browser Location:** Ensure that your browser has location services enabled otherwise the game functions will not
  work.

Clone the Repository

```bash
git clone git@github.com:kiransain/sopra-fs25-group-26-server.git
cd sopra-fs25-group-26-client
```

Local Development

Production Build

```bash
./gradlew build
```

Run the Application

```bash
./gradlew bootRun
```

Run Tests

```bash
./gradlew test
```

or directly in your IDE by running the test folder.

External Dependencies

- H2 in-memory database is used for testing, development and production. No external database setup is required.

Deployment

- The backend is automatically deployed to Google Cloud on pushes to the `main` branch.

Releases

- Make sure changes are committed and pushed to the 'develop' branch.
- Create a pull request to merge 'develop' into 'main'.
- After code review and approval, merge the pull request.
- Create a new tag for the release version and push it.
- Google Cloud will automatically deploy the latest version to production.

----------

## 🛣️ Roadmap

Future contributors might consider:

1. Customizable role assignment logic (e.g. several hunters, teams).
2. Add a persistent Database for production.

----------

## 👥 Authors & Acknowledgments

- Kiran Nanduri - frontend - [![Kiran Nanduri](https://img.shields.io/badge/-@kiransain-181717?style=flat-square&logo=github)](https://github.com/kiransain)
- Ermin Mumic - backend - [![Ermin Mumić](https://img.shields.io/badge/-@ermin--mumic-181717?style=flat-square&logo=github)](https://github.com/ermin-mumic)
- Gent Jashari - frontend - [![Gent Jashari](https://img.shields.io/badge/-@GentJash-181717?style=flat-square&logo=github)](https://github.com/GentJash)

- And many thanks to our TA Ambros Eberhard, the SoPra teaching team, the course in parallel Software Engineering by Prof. Thomas Fritz, and the open-source tools we relied on.

- This code is based on the [SoPra-FS25-Server](https://github.com/HASEL-UZH/sopra-fs25-template-server)
- To see contributions, you can see [Contributors](https://github.com/kiransain/sopra-fs25-group-26-server/graphs/contributors)

----------

## 📄 License

This project is licensed under the [Apache License Version 2.0](LICENSE).

----------
