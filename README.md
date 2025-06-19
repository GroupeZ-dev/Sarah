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
implementation("fr.maxlego08.sarah:sarah:1.18")
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
    <version>1.18</version>
</dependency>
```
> **Note**: Please ensure to relocate your dependencies to avoid conflicts with other projects using the same library.

## How to connect to the database ?

### With MYSQL

````java
public void connect() {
    DatabaseConfiguration configuration=DatabaseConfiguration.create(<user>,<password>,<port>,<host>,<database>);
    DatabaseConnection connection=new MySqlConnection(configuration);
}
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

You must then save your migration with the method ``MigrationManager.registerMigration``

After saving all your migrations, you must run them with the method ``MigrationManager.execute``
This method takes as parameter a SQL Java `Connection`, `DatabaseConfiguration` and a `Logger`

## How to create SQL queries ?

The `RequestHelper` class simplifies queries, but you won’t be able to handle errors. To start using Sarah sa will be
enough. But if you need more control, just take the code from the `RequestHelper` class.

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
