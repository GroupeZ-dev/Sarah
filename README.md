# Sarah

A library to easily manage your database with a simple and powerful API.

## Graddle

```gradle
maven {
    name = "groupezReleases"
    url = uri("https://repo.groupez.dev/releases")
}
```

```gradle
implementation("fr.maxlego08.sarah:sarah:1.26")
```

## Maven

```xml
<repository>
    <id>groupez-releases</id>
    <name>GroupeZ Repository</name>
    <url>https://repo.groupez.dev/releases</url>
</repository>
```

```xml
<dependency>
    <groupId>fr.maxlego08.sarah</groupId>
    <artifactId>sarah</artifactId>
    <version>1.26</version>
</dependency>
```
> **Note**: Please ensure to relocate your dependencies to avoid conflicts with other projects using the same library.

## How to connect to the database ?

### With MYSQL

````java
public void connect() {
    DatabaseConfiguration configuration=DatabaseConfiguration.create(<user>,<password>,<port>,<host>,<database>);
    DatabaseConnection connection=new MySqlConnection(configuration,<logger>);
}
````

### With MariaDB

````java
public void connect() {
    DatabaseConfiguration configuration=DatabaseConfiguration.createMariaDb(<user>,<password>,<port>,<host>,<database>);
    DatabaseConnection connection=new MariaDbConnection(configuration,<logger>);
}
````

### With PostgreSQL

Add the `org.postgresql:postgresql` driver to your plugin.

````java
public void connect() {
    DatabaseConfiguration configuration=DatabaseConfiguration.createPostgreSql(<user>,<password>,<port>,<host>,<database>,<debug>);
    DatabaseConnection connection=new PostgreSqlConnection(configuration,<logger>);
}
````

Write column names in the same case everywhere: PostgreSQL compares quoted names case-sensitively.

### With a connection pool

`HikariDatabaseConnection` keeps a pool of connections and works with every engine above:

````java
DatabaseConnection connection=new HikariDatabaseConnection(configuration,<logger>);
````

### With SQLITE

````java
public void connect() {
    // The boolean allows to enable or not debug requests
    DatabaseConfiguration configuration=DatabaseConfiguration.sqlite(<boolean>);
    
    // The folder will be where the database.db file will be located
    DatabaseConnection connection=new SqliteConnection(configuration,<folder>);
}
````

## How to create a migration ?

Sarah will create a migrations table (you can change the name with the
method `` MigrationManager.setMigrationTableName``).
Each migration will be run only once.

Example
from [zEssentials](https://github.com/Maxlego08/zEssentials/blob/develop/Essentials/src/main/java/fr/maxlego08/essentials/database/migrations/CreateUserTableMigration.java):

````java
package fr.maxlego08.essentials.database.migrations;

import fr.maxlego08.sarah.SchemaBuilder;
import fr.maxlego08.sarah.database.Migration;

public class CreateUserTableMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%users", table -> {
            table.uuid("unique_id").primary();
            table.string("name", 16);
            table.text("last_location").nullable();
            table.bigInt("play_time").defaultValue("0");
            table.timestamps();
        });
    }
}
````

You can prefix your tables if you need to.

To be able to roll a migration back, override `down()` with the opposite of `up()`:

````java
@Override
public void down() {
    drop("%prefix%users");
}
````

You must then save your migration with the method ``MigrationManager.registerMigration``

After saving all your migrations, you must run them with the method ``MigrationManager.execute``
This method takes as parameter a `DatabaseConnection` and a `Logger`

````java
MigrationManager.getAppliedMigrations(connection, logger); // names already applied
MigrationManager.getPendingMigrations(connection, logger); // registered, not applied yet
MigrationManager.rollback(connection, logger);             // runs down() of the last one
MigrationManager.rollback(connection, logger, 3);          // the last three, most recent first
````

A rollback refuses to start when one of the migrations to roll back has no `down()`.

## How to create SQL queries ?

The `RequestHelper` class simplifies queries. A failed write throws a `DatabaseException`. A failed read
(`select`, `count`, aggregates, `exists`) is logged and returns an empty result, unless you call
`requestHelper.setStrictReads(true)`, which makes it throw as well.

The following examples are from [zAuctionHouse Stats](https://github.com/Maxlego08/zAuctionHouse-Stats).

### Upsert

Allows to update the database by making an `INSERT` followed by an `ON DUPLICATE KEY UPDATE.`

````java
public void upsert(GlobalKey key, GlobalValue value) {
    ZPlugin.service.execute(() -> { 
        this.requestHelper.upsert("zah_stats_global", table -> {
            table.string("key", key.name());
            table.object("value", value.getValue());
        });
    });
}
````
Attention, if you are in SQLite, you need define primary keys with the method ``.primary()``
The example above will therefore become for SQLite:
````java
public void upsert(GlobalKey key, GlobalValue value) {
    ZPlugin.service.execute(() -> { 
        this.requestHelper.upsert("zah_stats_global", table -> {
            table.string("key", key.name()).primary();
            table.object("value", value.getValue());
        });
    });
}
````

### Insert

Allows you to create an insert

````java
public void insertItemPurchased(PlayerItemPurchased item) {
    ZPlugin.service.execute(() -> {
        this.requestHelper.insert("zah_player_purchased_items", table -> {
            table.uuid("player_id", item.getPlayerId());
            table.string("player_name", item.getPlayerName());
            table.string("itemstack", item.getItemStack());
            table.bigInt("price", item.getPrice());
            table.string("economy", item.getEconomy());
            table.uuid("seller_id", item.getSellerId());
            table.string("seller_name", item.getSellerName());
            table.bigInt("purchase_time", System.currentTimeMillis());
            table.string("auction_type", item.getAuctionType().name());
        });
    });
}
````

### Select

This example retrieves all the data from the table and transforms the result into a map

````java
public Map<UUID, List<PlayerItemPurchased>> selectAll() throws SQLException {
    return this.requestHelper.selectAll("zah_player_purchased_items", PlayerItemPurchasedDTO.class).stream().map(PlayerItemPurchased::new).collect(Collectors.groupingBy(PlayerItemPurchased::getPlayerId));
}
````

Here is another example with a where and an order by
````java
public List<ChatMessageDTO> getMessages(UUID uuid) {
    return requestHelper.select("chat_message", ChatMessageDTO.class, table -> {
        table.uuid("unique_id", uuid);
        table.orderByDesc("created_at");
    });
}
````

You must create an object with a constructor that will have a constructor with the name and each column
````java
package fr.maxlego08.stats.dto;

import fr.maxlego08.zauctionhouse.api.enums.AuctionType;

import java.util.UUID;

public record PlayerItemPurchasedDTO(long id, 
                                     UUID player_id,
                                     String player_name,
                                     String itemStack, 
                                     long price,
                                     String economy,
                                     UUID seller_id, 
                                     String seller_name,
                                     long purchase_time,
                                     AuctionType auction_type
) { }
````
If you are in a java version that does not have records, you must use the `@Column` annotation to set the column name
Here is an example with the table `MigrationTable`
````java
public static class MigrationTable {

    @Column("migration")
    private final String migration;

    public MigrationTable(String migration) {
        this.migration = migration;
    }

    public String getMigration() {
        return migration;
    }
}
````

### Conditions

Conditions are joined with `AND`. `orWhere` joins one with `OR`, and `where(group -> ...)` puts several
between parentheses. As in SQL, `AND` is applied before `OR`.

````java
requestHelper.select("users", table -> {
    table.where("active", true);
    table.where(group -> group.where("role", "admin").orWhereNull("role")); // AND (role = ? OR role IS NULL)
    table.whereBetween("level", 10, 20);
    table.whereNotIn("name", "bot", "test");
    table.whereLike("email", "%@example.com");
    table.limit(50);
});
````

Also available: `whereIn`, `whereNull`, `whereNotNull`, `whereNotBetween`, `whereNotLike`, and `orWhere`,
`orWhereNull`, `orWhereNotNull`, `orWhereIn`, `orWhereBetween`.

### Counts and aggregates

````java
long players = requestHelper.count("users", table -> table.where("active", true));
Number total = requestHelper.sum("sales", "amount", table -> table.where("seller", name)); // null when no row matches
Number best = requestHelper.max("sales", "amount", table -> {});
boolean known = requestHelper.exists("users", table -> table.where("name", name));
````

`min` and `avg` work the same way. Group a select with `groupBy` and filter the groups with `having`:

````java
requestHelper.select("sales", table -> {
    table.addSelect("seller");
    table.groupBy("seller");
    table.having("SUM(`amount`)", ">=", 1000);
});
````

### Results of writes

`update`, `delete`, `upsert` and `insert` return nothing. Use their variants to get a result:

````java
int changed = requestHelper.updateAndCount("users", table -> table.bigInt("coins", 0).where("name", name));
int id = requestHelper.insertAndGetKey("logs", table -> table.string("message", message));
````

Also available: `deleteAndCount`, `upsertAndCount` and the `...MultipleAndCount` / `insertMultipleAndGetKey`
batch variants. `table.increment("coins", 10)` adds to a value instead of replacing it, so two servers
adding at the same time do not overwrite each other.

### Transactions and raw SQL

````java
requestHelper.inTransaction(tx -> {
    tx.update("accounts", table -> table.increment("balance", -10).where("id", from));
    tx.update("accounts", table -> table.increment("balance", 10).where("id", to));
    return null;
}); // committed when the callback returns, rolled back when it throws

requestHelper.execute("DELETE FROM %prefix%logs WHERE created_at < ?", limit);
List<String> names = requestHelper.query("SELECT name FROM %prefix%users", rs -> rs.getString("name"));
````

### Asynchronous queries

`AsyncRequestHelper` runs every operation off the calling thread and returns a `CompletableFuture`.
Writes complete with the row count or the generated key.

````java
AsyncRequestHelper async = new AsyncRequestHelper(requestHelper);          // own pool of 4 daemon threads
// AsyncRequestHelper async = new AsyncRequestHelper(requestHelper, executor); // or your own executor

async.select("users", UserDTO.class, table -> table.where("name", name))
     .thenAccept(users -> ...); // runs on the pool's thread, not the server's main thread

async.shutdown(); // when your plugin disables
````

## Running tests with different databases

Use JDK 21 with the Gradle wrapper. SQLite needs no server:

```powershell
.\gradlew.bat testSqlite
.\gradlew.bat testMariaDb
.\gradlew.bat testMySql
.\gradlew.bat testPostgreSql
```

Server tests require an existing **dedicated, disposable database**: the suite creates and drops tables. Defaults: host `localhost`, database/user/password `sarah`, port `3306` for MySQL/MariaDB and `5432` for PostgreSQL. The tasks do not start or provision servers.

Configure the connection with environment variables (PowerShell example):

```powershell
$env:SARAH_TEST_HOST = "localhost"
$env:SARAH_TEST_PORT = "5433"
$env:SARAH_TEST_DATABASE = "sarah_test"
$env:SARAH_TEST_USER = "sarah"
$env:SARAH_TEST_PASSWORD = "sarah"
.\gradlew.bat testPostgreSql
```

Alternatively use JVM properties, which take priority over environment variables:

```powershell
.\gradlew.bat test "-Dsarah.test.type=POSTGRESQL" "-Dsarah.test.port=5433"
.\gradlew.bat testMariaDb "-Dsarah.test.port=3307"
```

`test` defaults to SQLite; `SARAH_TEST_TYPE` also selects its engine (`SQLITE`, `MARIADB`, `MYSQL`, `POSTGRESQL`, or `POSTGRES`). Named tasks always select their named engine. All tasks run the complete suite, including tests that explicitly use SQLite. Database runs always execute again, without reusing cached test results. Reports are in `build/reports/tests/<task>/index.html`.

### Local connection file

Edit `test-databases.properties` at the repository root to configure each engine separately. This local file is ignored by Git. For a fresh checkout, create it from the committed example:

```powershell
Copy-Item test-databases.properties.example test-databases.properties
```

Use `mariadb.*`, `mysql.*` and `postgresql.*` settings for each server (host, port, database, user, password). SQLite needs no connection settings. Set `type=SQLITE` (or another engine) for the default `test` task. Named tasks such as `testMariaDb` automatically load their engine's settings.

Connection setting priority: `-Dsarah.test.*` > `SARAH_TEST_*` environment variables > local file > built-in defaults. Write values without surrounding quotes; in this Java properties file, literal backslashes must be doubled (`\\`). The file is read as UTF-8.
