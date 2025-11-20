package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for using DTO classes with ConsumerConstructor
 * Tests the automatic schema generation and mapping from class templates
 */
public class DTOConsumerTest extends DatabaseTestBase {

    // Simple DTO using constructor only
    public static class ProductDTO {
        @Column(value = "id", autoIncrement = true)
        private final Long id;
        @Column(value = "name", unique = true)
        private final String name;
        @Column(value = "description", nullable = true)
        private final String description;
        private final Double price;
        @Column(value = "stock", nullable = true)
        private final Integer stock;
        private final Boolean available;

        public ProductDTO(Long id, String name, String description, Double price, Integer stock, Boolean available) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock = stock;
            this.available = available;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Double getPrice() { return price; }
        public Integer getStock() { return stock; }
        public Boolean getAvailable() { return available; }
    }

    // DTO with @Column annotations
    public static class UserDTO {
        @Column(value = "user_id", autoIncrement = true)
        private final Long id;

        @Column("user_name")
        private final String username;

        @Column("user_email")
        private final String email;

        private final Integer age;

        public UserDTO(Long id, String username, String email, Integer age) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.age = age;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public Integer getAge() { return age; }
    }

    // DTO with various data types
    public static class ComplexDTO {
        @Column(value = "id", autoIncrement = true)
        private final Long id;
        private final String textField;
        private final Integer intField;
        private final Double doubleField;
        private final Boolean boolField;

        public ComplexDTO(Long id, String textField, Integer intField, Double doubleField, Boolean boolField) {
            this.id = id;
            this.textField = textField;
            this.intField = intField;
            this.doubleField = doubleField;
            this.boolField = boolField;
        }

        public Long getId() { return id; }
        public String getTextField() { return textField; }
        public Integer getIntField() { return intField; }
        public Double getDoubleField() { return doubleField; }
        public Boolean getBoolField() { return boolField; }
    }

    @Test
    public void testCreateTableFromDTOClass() throws Exception {
        // Create table using DTO class template
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        // Verify table was created by inserting data
        requestHelper.insert("test_products", schema -> {
            schema.string("name", "Laptop");
            schema.string("description", "High-performance laptop");
            schema.decimal("price", 999.99);
            schema.bigInt("stock", 50);
            schema.bool("available", true);
        });

        assertEquals(1, countRows("test_products"));
    }

    @Test
    public void testInsertWithDTOInstance() throws Exception {
        // Create table
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        // Create a product instance
        ProductDTO product = new ProductDTO(null, "Mouse", "Wireless mouse", 29.99, 100, true);

        // Insert using DTO
        requestHelper.insert("test_products", ProductDTO.class, product);

        // Verify insertion
        assertEquals(1, countRows("test_products"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_products");
            assertTrue(rs.next());
            assertEquals("Mouse", rs.getString("name"));
            assertEquals("Wireless mouse", rs.getString("description"));
            assertEquals(29.99, rs.getDouble("price"), 0.01);
            assertEquals(100, rs.getInt("stock"));
            assertTrue(rs.getBoolean("available"));
        }
    }

    @Test
    public void testUpdateWithDTO() throws Exception {
        // Create and insert
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        ProductDTO product = new ProductDTO(null, "Keyboard", "Mechanical keyboard", 149.99, 30, true);
        requestHelper.insert("test_products", ProductDTO.class, product);
        
        // Update using manual schema with WHERE clause
        requestHelper.update("test_products", schema -> {
            schema.string("name", "Keyboard RGB");
            schema.string("description", "RGB Mechanical keyboard");
            schema.decimal("price", 199.99);
            schema.bigInt("stock", 25);
            schema.bool("available", true);
            schema.where("name", "Keyboard");
        });

        // Verify update
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_products");
            assertTrue(rs.next());
            assertEquals("Keyboard RGB", rs.getString("name"));
            assertEquals(199.99, rs.getDouble("price"), 0.01);
        }
    }

    @Test
    public void testUpsertWithDTO() throws Exception {
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        // First upsert (insert)
        ProductDTO product1 = new ProductDTO(null, "Monitor", "4K Monitor", 499.99, 20, true);
        requestHelper.upsert("test_products", ProductDTO.class, product1);
        assertEquals(1, countRows("test_products"));

        // Second upsert
        ProductDTO product2 = new ProductDTO(null, "Monitor", "8K Monitor", 1499.99, 15, true);
        requestHelper.upsert("test_products", ProductDTO.class, product2);

        assertTrue(countRows("test_products") >= 1);
    }

    @Test
    public void testDTOWithColumnAnnotations() throws Exception {
        // Create table using DTO with @Column annotations
        SchemaBuilder.create(null, "test_users_dto", UserDTO.class).execute(connection, testLogger);

        UserDTO user = new UserDTO(null, "john_doe", "john@example.com", 30);
        requestHelper.insert("test_users_dto", UserDTO.class, user);

        // Verify column names from @Column annotations
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT user_id, user_name, user_email, age FROM test_users_dto");
            assertTrue(rs.next());
            assertNotNull(rs.getObject("user_id"));
            assertEquals("john_doe", rs.getString("user_name"));
            assertEquals("john@example.com", rs.getString("user_email"));
            assertEquals(30, rs.getInt("age"));
        }
    }

    @Test
    public void testSelectWithDTO() throws Exception {
        // Create table and insert data
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        requestHelper.insert("test_products", schema -> {
            schema.string("name", "Headphones");
            schema.string("description", "Noise-canceling headphones");
            schema.decimal("price", 299.99);
            schema.bigInt("stock", 40);
            schema.bool("available", true);
        });

        // Select using DTO class
        List<ProductDTO> results = requestHelper.selectAll("test_products", ProductDTO.class);

        assertEquals(1, results.size());
        ProductDTO retrieved = results.get(0);
        assertNotNull(retrieved.getId());
        assertEquals("Headphones", retrieved.getName());
        assertEquals("Noise-canceling headphones", retrieved.getDescription());
        assertEquals(299.99, retrieved.getPrice(), 0.01);
        assertEquals(40, retrieved.getStock());
        assertTrue(retrieved.getAvailable());
    }

    @Test
    public void testDTOWithNullValues() throws Exception {
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        ProductDTO product = new ProductDTO(null, "Test Product", null, 10.0, null, false);
        requestHelper.insert("test_products", ProductDTO.class, product);

        List<ProductDTO> results = requestHelper.selectAll("test_products", ProductDTO.class);
        assertEquals(1, results.size());

        ProductDTO retrieved = results.get(0);
        assertEquals("Test Product", retrieved.getName());
        assertNull(retrieved.getDescription());
        assertNull(retrieved.getStock());
        assertFalse(retrieved.getAvailable());
    }

    @Test
    public void testMultipleInsertsWithDTO() throws Exception {
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        // Insert multiple products using DTO
        for (int i = 1; i <= 5; i++) {
            ProductDTO product = new ProductDTO(
                    null,
                    "Product " + i,
                    "Description " + i,
                    10.0 * i,
                    i * 10,
                    i % 2 == 0
            );
            requestHelper.insert("test_products", ProductDTO.class, product);
        }

        assertEquals(5, countRows("test_products"));

        // Select all and verify
        List<ProductDTO> results = requestHelper.selectAll("test_products", ProductDTO.class);
        assertEquals(5, results.size());
    }

    @Test
    public void testSelectWithDTOAndWhereCondition() throws Exception {
        SchemaBuilder.create(null, "test_products", ProductDTO.class).execute(connection, testLogger);

        // Insert multiple products
        requestHelper.insert("test_products", ProductDTO.class,
            new ProductDTO(null, "Cheap Product", "Budget item", 5.0, 100, true));
        requestHelper.insert("test_products", ProductDTO.class,
            new ProductDTO(null, "Expensive Product", "Premium item", 500.0, 10, true));
        requestHelper.insert("test_products", ProductDTO.class,
            new ProductDTO(null, "Mid Product", "Standard item", 50.0, 50, false));

        // Select with WHERE condition
        List<ProductDTO> results = requestHelper.select("test_products", ProductDTO.class, schema -> {
            schema.where("price", ">", 10);
            schema.where("available", true);
        });

        assertEquals(1, results.size());
        assertEquals("Expensive Product", results.get(0).getName());
    }

    @Test
    public void testDTOWithDifferentTypes() throws Exception {
        SchemaBuilder.create(null, "test_complex", ComplexDTO.class).execute(connection, testLogger);

        ComplexDTO complex = new ComplexDTO(null, "test", 42, 3.14, true);
        requestHelper.insert("test_complex", ComplexDTO.class, complex);

        List<ComplexDTO> results = requestHelper.selectAll("test_complex", ComplexDTO.class);
        assertEquals(1, results.size());

        ComplexDTO retrieved = results.get(0);
        assertEquals("test", retrieved.getTextField());
        assertEquals(42, retrieved.getIntField());
        assertEquals(3.14, retrieved.getDoubleField(), 0.001);
        assertTrue(retrieved.getBoolField());
    }
}