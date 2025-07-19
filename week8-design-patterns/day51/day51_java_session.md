# Day 51: Creational Patterns - Builder & Prototype

## Learning Goals

By the end of this session, you will:
- Master the Builder pattern for constructing complex objects step-by-step
- Understand the Prototype pattern for object cloning and performance optimization
- Implement fluent APIs and method chaining for better user experience
- Create object pools and manage expensive object creation
- Build practical applications using Builder and Prototype patterns
- Understand deep vs shallow cloning and their implications

## Table of Contents
1. [Builder Pattern](#builder-pattern)
2. [Prototype Pattern](#prototype-pattern)
3. [Object Pools and Performance](#object-pools-performance)
4. [Hands-On Practice](#hands-on-practice)
5. [Challenges](#challenges)
6. [Summary](#summary)

---

## Builder Pattern

### Problem and Solution

**Problem**: Constructing complex objects with many optional parameters leads to telescoping constructors and unclear code.

**Solution**: Separate object construction from representation, allowing step-by-step creation with a fluent API.

### Basic Builder Implementation

```java
// ComputerBuilder.java - Basic Builder Pattern implementation
package com.javajourney.patterns.builder;

/**
 * Computer Builder Pattern Implementation
 * Demonstrates step-by-step construction of complex objects
 */

// Product class
class Computer {
    // Required parameters
    private final String processor;
    private final String memory;
    
    // Optional parameters
    private final String storage;
    private final String graphicsCard;
    private final String motherboard;
    private final String powerSupply;
    private final String coolingSystem;
    private final boolean isGamingPC;
    private final boolean hasWiFi;
    private final boolean hasBluetooth;
    
    // Private constructor - only Builder can create instances
    private Computer(Builder builder) {
        this.processor = builder.processor;
        this.memory = builder.memory;
        this.storage = builder.storage;
        this.graphicsCard = builder.graphicsCard;
        this.motherboard = builder.motherboard;
        this.powerSupply = builder.powerSupply;
        this.coolingSystem = builder.coolingSystem;
        this.isGamingPC = builder.isGamingPC;
        this.hasWiFi = builder.hasWiFi;
        this.hasBluetooth = builder.hasBluetooth;
    }
    
    // Getters
    public String getProcessor() { return processor; }
    public String getMemory() { return memory; }
    public String getStorage() { return storage; }
    public String getGraphicsCard() { return graphicsCard; }
    public String getMotherboard() { return motherboard; }
    public String getPowerSupply() { return powerSupply; }
    public String getCoolingSystem() { return coolingSystem; }
    public boolean isGamingPC() { return isGamingPC; }
    public boolean hasWiFi() { return hasWiFi; }
    public boolean hasBluetooth() { return hasBluetooth; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Computer Configuration:\n");
        sb.append("  Processor: ").append(processor).append("\n");
        sb.append("  Memory: ").append(memory).append("\n");
        if (storage != null) sb.append("  Storage: ").append(storage).append("\n");
        if (graphicsCard != null) sb.append("  Graphics: ").append(graphicsCard).append("\n");
        if (motherboard != null) sb.append("  Motherboard: ").append(motherboard).append("\n");
        if (powerSupply != null) sb.append("  Power Supply: ").append(powerSupply).append("\n");
        if (coolingSystem != null) sb.append("  Cooling: ").append(coolingSystem).append("\n");
        sb.append("  Gaming PC: ").append(isGamingPC).append("\n");
        sb.append("  WiFi: ").append(hasWiFi).append("\n");
        sb.append("  Bluetooth: ").append(hasBluetooth).append("\n");
        return sb.toString();
    }
    
    // Builder class
    public static class Builder {
        // Required parameters
        private final String processor;
        private final String memory;
        
        // Optional parameters with default values
        private String storage = "500GB SSD";
        private String graphicsCard = "Integrated";
        private String motherboard = "Standard ATX";
        private String powerSupply = "500W";
        private String coolingSystem = "Stock Cooler";
        private boolean isGamingPC = false;
        private boolean hasWiFi = true;
        private boolean hasBluetooth = true;
        
        // Builder constructor with required parameters
        public Builder(String processor, String memory) {
            this.processor = processor;
            this.memory = memory;
        }
        
        // Fluent API methods for optional parameters
        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }
        
        public Builder graphicsCard(String graphicsCard) {
            this.graphicsCard = graphicsCard;
            return this;
        }
        
        public Builder motherboard(String motherboard) {
            this.motherboard = motherboard;
            return this;
        }
        
        public Builder powerSupply(String powerSupply) {
            this.powerSupply = powerSupply;
            return this;
        }
        
        public Builder coolingSystem(String coolingSystem) {
            this.coolingSystem = coolingSystem;
            return this;
        }
        
        public Builder isGamingPC(boolean isGamingPC) {
            this.isGamingPC = isGamingPC;
            return this;
        }
        
        public Builder hasWiFi(boolean hasWiFi) {
            this.hasWiFi = hasWiFi;
            return this;
        }
        
        public Builder hasBluetooth(boolean hasBluetooth) {
            this.hasBluetooth = hasBluetooth;
            return this;
        }
        
        // Convenience method for gaming configuration
        public Builder gamingConfiguration() {
            this.isGamingPC = true;
            this.graphicsCard = "RTX 4080";
            this.powerSupply = "850W Gold";
            this.coolingSystem = "Liquid Cooling";
            this.storage = "1TB NVMe SSD";
            return this;
        }
        
        // Convenience method for office configuration
        public Builder officeConfiguration() {
            this.isGamingPC = false;
            this.graphicsCard = "Integrated";
            this.powerSupply = "400W";
            this.coolingSystem = "Stock Cooler";
            this.storage = "256GB SSD";
            return this;
        }
        
        // Build method creates the final object
        public Computer build() {
            // Validation logic
            if (processor == null || processor.trim().isEmpty()) {
                throw new IllegalArgumentException("Processor is required");
            }
            if (memory == null || memory.trim().isEmpty()) {
                throw new IllegalArgumentException("Memory is required");
            }
            
            // Gaming PC validation
            if (isGamingPC && "Integrated".equals(graphicsCard)) {
                System.out.println("Warning: Gaming PC with integrated graphics may not perform well");
            }
            
            return new Computer(this);
        }
    }
}
```

### Advanced Builder: SQL Query Builder

```java
// SQLQueryBuilder.java - Advanced Builder Pattern for SQL queries
package com.javajourney.patterns.builder;

import java.util.*;

/**
 * SQL Query Builder Implementation
 * Demonstrates advanced Builder pattern with conditional logic
 */
public class SQLQueryBuilder {
    
    // Query components
    private StringBuilder selectClause;
    private StringBuilder fromClause;
    private StringBuilder joinClause;
    private StringBuilder whereClause;
    private StringBuilder groupByClause;
    private StringBuilder havingClause;
    private StringBuilder orderByClause;
    private Integer limitValue;
    private Integer offsetValue;
    
    // Query parameters for prepared statements
    private List<Object> parameters;
    
    public SQLQueryBuilder() {
        this.selectClause = new StringBuilder();
        this.fromClause = new StringBuilder();
        this.joinClause = new StringBuilder();
        this.whereClause = new StringBuilder();
        this.groupByClause = new StringBuilder();
        this.havingClause = new StringBuilder();
        this.orderByClause = new StringBuilder();
        this.parameters = new ArrayList<>();
    }
    
    // SELECT clause methods
    public SQLQueryBuilder select(String... columns) {
        if (selectClause.length() > 0) {
            selectClause.append(", ");
        }
        selectClause.append(String.join(", ", columns));
        return this;
    }
    
    public SQLQueryBuilder selectAll() {
        if (selectClause.length() > 0) {
            selectClause.append(", ");
        }
        selectClause.append("*");
        return this;
    }
    
    public SQLQueryBuilder selectCount(String column) {
        return select("COUNT(" + column + ")");
    }
    
    public SQLQueryBuilder selectSum(String column) {
        return select("SUM(" + column + ")");
    }
    
    public SQLQueryBuilder selectAvg(String column) {
        return select("AVG(" + column + ")");
    }
    
    // FROM clause methods
    public SQLQueryBuilder from(String table) {
        if (fromClause.length() > 0) {
            fromClause.append(", ");
        }
        fromClause.append(table);
        return this;
    }
    
    public SQLQueryBuilder from(String table, String alias) {
        return from(table + " AS " + alias);
    }
    
    // JOIN methods
    public SQLQueryBuilder join(String table, String condition) {
        joinClause.append(" JOIN ").append(table).append(" ON ").append(condition);
        return this;
    }
    
    public SQLQueryBuilder leftJoin(String table, String condition) {
        joinClause.append(" LEFT JOIN ").append(table).append(" ON ").append(condition);
        return this;
    }
    
    public SQLQueryBuilder rightJoin(String table, String condition) {
        joinClause.append(" RIGHT JOIN ").append(table).append(" ON ").append(condition);
        return this;
    }
    
    public SQLQueryBuilder innerJoin(String table, String condition) {
        joinClause.append(" INNER JOIN ").append(table).append(" ON ").append(condition);
        return this;
    }
    
    // WHERE clause methods
    public SQLQueryBuilder where(String condition) {
        addWhereCondition("AND", condition);
        return this;
    }
    
    public SQLQueryBuilder whereEquals(String column, Object value) {
        addWhereCondition("AND", column + " = ?");
        parameters.add(value);
        return this;
    }
    
    public SQLQueryBuilder whereIn(String column, Object... values) {
        String placeholders = String.join(", ", Collections.nCopies(values.length, "?"));
        addWhereCondition("AND", column + " IN (" + placeholders + ")");
        parameters.addAll(Arrays.asList(values));
        return this;
    }
    
    public SQLQueryBuilder whereLike(String column, String pattern) {
        addWhereCondition("AND", column + " LIKE ?");
        parameters.add(pattern);
        return this;
    }
    
    public SQLQueryBuilder whereBetween(String column, Object start, Object end) {
        addWhereCondition("AND", column + " BETWEEN ? AND ?");
        parameters.add(start);
        parameters.add(end);
        return this;
    }
    
    public SQLQueryBuilder whereNull(String column) {
        addWhereCondition("AND", column + " IS NULL");
        return this;
    }
    
    public SQLQueryBuilder whereNotNull(String column) {
        addWhereCondition("AND", column + " IS NOT NULL");
        return this;
    }
    
    public SQLQueryBuilder or() {
        if (whereClause.length() > 0) {
            whereClause.append(" OR ");
        }
        return this;
    }
    
    private void addWhereCondition(String operator, String condition) {
        if (whereClause.length() == 0) {
            whereClause.append(condition);
        } else {
            whereClause.append(" ").append(operator).append(" ").append(condition);
        }
    }
    
    // GROUP BY and HAVING methods
    public SQLQueryBuilder groupBy(String... columns) {
        if (groupByClause.length() > 0) {
            groupByClause.append(", ");
        }
        groupByClause.append(String.join(", ", columns));
        return this;
    }
    
    public SQLQueryBuilder having(String condition) {
        if (havingClause.length() > 0) {
            havingClause.append(" AND ");
        }
        havingClause.append(condition);
        return this;
    }
    
    // ORDER BY methods
    public SQLQueryBuilder orderBy(String column) {
        return orderBy(column, "ASC");
    }
    
    public SQLQueryBuilder orderBy(String column, String direction) {
        if (orderByClause.length() > 0) {
            orderByClause.append(", ");
        }
        orderByClause.append(column).append(" ").append(direction);
        return this;
    }
    
    public SQLQueryBuilder orderByDesc(String column) {
        return orderBy(column, "DESC");
    }
    
    // LIMIT and OFFSET methods
    public SQLQueryBuilder limit(int limit) {
        this.limitValue = limit;
        return this;
    }
    
    public SQLQueryBuilder offset(int offset) {
        this.offsetValue = offset;
        return this;
    }
    
    public SQLQueryBuilder page(int pageNumber, int pageSize) {
        this.limitValue = pageSize;
        this.offsetValue = (pageNumber - 1) * pageSize;
        return this;
    }
    
    // Build the final query
    public String build() {
        if (selectClause.length() == 0) {
            throw new IllegalStateException("SELECT clause is required");
        }
        if (fromClause.length() == 0) {
            throw new IllegalStateException("FROM clause is required");
        }
        
        StringBuilder query = new StringBuilder();
        
        // SELECT
        query.append("SELECT ").append(selectClause);
        
        // FROM
        query.append(" FROM ").append(fromClause);
        
        // JOIN
        if (joinClause.length() > 0) {
            query.append(joinClause);
        }
        
        // WHERE
        if (whereClause.length() > 0) {
            query.append(" WHERE ").append(whereClause);
        }
        
        // GROUP BY
        if (groupByClause.length() > 0) {
            query.append(" GROUP BY ").append(groupByClause);
        }
        
        // HAVING
        if (havingClause.length() > 0) {
            query.append(" HAVING ").append(havingClause);
        }
        
        // ORDER BY
        if (orderByClause.length() > 0) {
            query.append(" ORDER BY ").append(orderByClause);
        }
        
        // LIMIT
        if (limitValue != null) {
            query.append(" LIMIT ").append(limitValue);
        }
        
        // OFFSET
        if (offsetValue != null) {
            query.append(" OFFSET ").append(offsetValue);
        }
        
        return query.toString();
    }
    
    public List<Object> getParameters() {
        return new ArrayList<>(parameters);
    }
    
    public void reset() {
        selectClause.setLength(0);
        fromClause.setLength(0);
        joinClause.setLength(0);
        whereClause.setLength(0);
        groupByClause.setLength(0);
        havingClause.setLength(0);
        orderByClause.setLength(0);
        limitValue = null;
        offsetValue = null;
        parameters.clear();
    }
    
    @Override
    public String toString() {
        return build();
    }
}
```

---

## Prototype Pattern

### Problem and Solution

**Problem**: Creating new objects is expensive (database queries, network calls, complex calculations).

**Solution**: Create new objects by cloning existing instances instead of creating from scratch.

### Basic Prototype Implementation

```java
// GameObjectPrototype.java - Basic Prototype Pattern implementation
package com.javajourney.patterns.prototype;

import java.util.*;

/**
 * Game Object Prototype Pattern Implementation
 * Demonstrates object cloning for performance optimization
 */

// Prototype interface
interface Prototype<T> {
    T clone();
}

// Base game object
abstract class GameObject implements Prototype<GameObject> {
    protected String id;
    protected String name;
    protected int x, y;
    protected int health;
    protected int maxHealth;
    protected Map<String, Object> properties;
    
    public GameObject(String id, String name, int x, int y, int health) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.health = health;
        this.maxHealth = health;
        this.properties = new HashMap<>();
    }
    
    // Copy constructor for cloning
    protected GameObject(GameObject original) {
        this.id = original.id;
        this.name = original.name;
        this.x = original.x;
        this.y = original.y;
        this.health = original.health;
        this.maxHealth = original.maxHealth;
        this.properties = new HashMap<>(original.properties);
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = Math.max(0, Math.min(health, maxHealth)); }
    public int getMaxHealth() { return maxHealth; }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public void move(int deltaX, int deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }
    
    public void takeDamage(int damage) {
        setHealth(health - damage);
    }
    
    public void heal(int amount) {
        setHealth(health + amount);
    }
    
    public boolean isAlive() {
        return health > 0;
    }
    
    public abstract String getType();
    
    @Override
    public String toString() {
        return String.format("%s[id=%s, name=%s, pos=(%d,%d), health=%d/%d]",
            getType(), id, name, x, y, health, maxHealth);
    }
}

// Concrete game objects
class Warrior extends GameObject {
    private int strength;
    private int armor;
    private String weaponType;
    
    public Warrior(String id, String name, int x, int y, int health, 
                   int strength, int armor, String weaponType) {
        super(id, name, x, y, health);
        this.strength = strength;
        this.armor = armor;
        this.weaponType = weaponType;
    }
    
    // Copy constructor
    private Warrior(Warrior original) {
        super(original);
        this.strength = original.strength;
        this.armor = original.armor;
        this.weaponType = original.weaponType;
    }
    
    @Override
    public GameObject clone() {
        return new Warrior(this);
    }
    
    @Override
    public String getType() {
        return "Warrior";
    }
    
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public int getArmor() { return armor; }
    public void setArmor(int armor) { this.armor = armor; }
    public String getWeaponType() { return weaponType; }
    public void setWeaponType(String weaponType) { this.weaponType = weaponType; }
    
    public int attack() {
        return strength + (int)(Math.random() * 10);
    }
    
    public int defend(int incomingDamage) {
        int actualDamage = Math.max(1, incomingDamage - armor);
        takeDamage(actualDamage);
        return actualDamage;
    }
}

class Mage extends GameObject {
    private int mana;
    private int maxMana;
    private int spellPower;
    private List<String> spells;
    
    public Mage(String id, String name, int x, int y, int health, 
                int mana, int spellPower) {
        super(id, name, x, y, health);
        this.mana = mana;
        this.maxMana = mana;
        this.spellPower = spellPower;
        this.spells = new ArrayList<>();
    }
    
    // Copy constructor
    private Mage(Mage original) {
        super(original);
        this.mana = original.mana;
        this.maxMana = original.maxMana;
        this.spellPower = original.spellPower;
        this.spells = new ArrayList<>(original.spells);
    }
    
    @Override
    public GameObject clone() {
        return new Mage(this);
    }
    
    @Override
    public String getType() {
        return "Mage";
    }
    
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(mana, maxMana)); }
    public int getMaxMana() { return maxMana; }
    public int getSpellPower() { return spellPower; }
    public void setSpellPower(int spellPower) { this.spellPower = spellPower; }
    
    public void addSpell(String spell) {
        if (!spells.contains(spell)) {
            spells.add(spell);
        }
    }
    
    public List<String> getSpells() {
        return new ArrayList<>(spells);
    }
    
    public boolean castSpell(String spell, int manaCost) {
        if (spells.contains(spell) && mana >= manaCost) {
            setMana(mana - manaCost);
            return true;
        }
        return false;
    }
    
    public int magicAttack(String spell) {
        int manaCost = spell.length(); // Simple mana cost calculation
        if (castSpell(spell, manaCost)) {
            return spellPower + (int)(Math.random() * 15);
        }
        return 0;
    }
}

class Archer extends GameObject {
    private int dexterity;
    private int arrows;
    private String bowType;
    
    public Archer(String id, String name, int x, int y, int health, 
                  int dexterity, int arrows, String bowType) {
        super(id, name, x, y, health);
        this.dexterity = dexterity;
        this.arrows = arrows;
        this.bowType = bowType;
    }
    
    // Copy constructor
    private Archer(Archer original) {
        super(original);
        this.dexterity = original.dexterity;
        this.arrows = original.arrows;
        this.bowType = original.bowType;
    }
    
    @Override
    public GameObject clone() {
        return new Archer(this);
    }
    
    @Override
    public String getType() {
        return "Archer";
    }
    
    public int getDexterity() { return dexterity; }
    public void setDexterity(int dexterity) { this.dexterity = dexterity; }
    public int getArrows() { return arrows; }
    public void setArrows(int arrows) { this.arrows = Math.max(0, arrows); }
    public String getBowType() { return bowType; }
    public void setBowType(String bowType) { this.bowType = bowType; }
    
    public int rangedAttack() {
        if (arrows > 0) {
            arrows--;
            return dexterity + (int)(Math.random() * 12);
        }
        return 0; // No arrows left
    }
    
    public boolean hasArrows() {
        return arrows > 0;
    }
}
```

### Advanced Prototype: Configuration Templates

```java
// ConfigurationTemplate.java - Advanced Prototype with deep cloning
package com.javajourney.patterns.prototype;

import java.util.*;
import java.io.*;

/**
 * Configuration Template Prototype
 * Demonstrates advanced cloning with nested objects and performance optimization
 */
public class ConfigurationTemplate implements Cloneable, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String templateName;
    private String version;
    private Date createdDate;
    private Map<String, Object> settings;
    private List<DatabaseConfig> databases;
    private ServerConfig serverConfig;
    private SecurityConfig securityConfig;
    
    public ConfigurationTemplate(String templateName, String version) {
        this.templateName = templateName;
        this.version = version;
        this.createdDate = new Date();
        this.settings = new HashMap<>();
        this.databases = new ArrayList<>();
        this.serverConfig = new ServerConfig();
        this.securityConfig = new SecurityConfig();
    }
    
    // Shallow clone using Object.clone()
    @Override
    public ConfigurationTemplate clone() {
        try {
            return (ConfigurationTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }
    
    // Deep clone using copy constructor
    public ConfigurationTemplate deepClone() {
        ConfigurationTemplate cloned = new ConfigurationTemplate(this.templateName, this.version);
        cloned.createdDate = new Date(this.createdDate.getTime());
        cloned.settings = new HashMap<>(this.settings);
        cloned.databases = new ArrayList<>();
        for (DatabaseConfig db : this.databases) {
            cloned.databases.add(db.clone());
        }
        cloned.serverConfig = this.serverConfig.clone();
        cloned.securityConfig = this.securityConfig.clone();
        return cloned;
    }
    
    // Deep clone using serialization
    public ConfigurationTemplate serializationClone() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.close();
            
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            ConfigurationTemplate cloned = (ConfigurationTemplate) ois.readObject();
            ois.close();
            
            return cloned;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Serialization clone failed", e);
        }
    }
    
    // Configuration methods
    public void addDatabaseConfig(DatabaseConfig dbConfig) {
        databases.add(dbConfig);
    }
    
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }
    
    public Object getSetting(String key) {
        return settings.get(key);
    }
    
    // Getters and setters
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Date getCreatedDate() { return new Date(createdDate.getTime()); }
    public Map<String, Object> getSettings() { return new HashMap<>(settings); }
    public List<DatabaseConfig> getDatabases() { return new ArrayList<>(databases); }
    public ServerConfig getServerConfig() { return serverConfig; }
    public SecurityConfig getSecurityConfig() { return securityConfig; }
    
    @Override
    public String toString() {
        return String.format("ConfigurationTemplate[name=%s, version=%s, databases=%d, settings=%d]",
            templateName, version, databases.size(), settings.size());
    }
}

// Supporting configuration classes
class DatabaseConfig implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String type;
    private String host;
    private int port;
    private String username;
    private String password;
    private Map<String, String> properties;
    
    public DatabaseConfig(String name, String type, String host, int port) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.properties = new HashMap<>();
    }
    
    @Override
    public DatabaseConfig clone() {
        try {
            DatabaseConfig cloned = (DatabaseConfig) super.clone();
            cloned.properties = new HashMap<>(this.properties);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        return properties.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("DatabaseConfig[name=%s, type=%s, host=%s:%d]", name, type, host, port);
    }
}

class ServerConfig implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private String serverName;
    private int port;
    private int maxConnections;
    private long timeoutMs;
    private List<String> allowedHosts;
    
    public ServerConfig() {
        this.serverName = "localhost";
        this.port = 8080;
        this.maxConnections = 100;
        this.timeoutMs = 30000;
        this.allowedHosts = new ArrayList<>();
    }
    
    @Override
    public ServerConfig clone() {
        try {
            ServerConfig cloned = (ServerConfig) super.clone();
            cloned.allowedHosts = new ArrayList<>(this.allowedHosts);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }
    
    // Getters and setters
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public void addAllowedHost(String host) {
        if (!allowedHosts.contains(host)) {
            allowedHosts.add(host);
        }
    }
    
    public List<String> getAllowedHosts() {
        return new ArrayList<>(allowedHosts);
    }
}

class SecurityConfig implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean sslEnabled;
    private String encryptionAlgorithm;
    private int sessionTimeoutMinutes;
    private Set<String> enabledFeatures;
    
    public SecurityConfig() {
        this.sslEnabled = true;
        this.encryptionAlgorithm = "AES-256";
        this.sessionTimeoutMinutes = 30;
        this.enabledFeatures = new HashSet<>();
    }
    
    @Override
    public SecurityConfig clone() {
        try {
            SecurityConfig cloned = (SecurityConfig) super.clone();
            cloned.enabledFeatures = new HashSet<>(this.enabledFeatures);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }
    
    // Getters and setters
    public boolean isSslEnabled() { return sslEnabled; }
    public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
    
    public void enableFeature(String feature) {
        enabledFeatures.add(feature);
    }
    
    public void disableFeature(String feature) {
        enabledFeatures.remove(feature);
    }
    
    public boolean isFeatureEnabled(String feature) {
        return enabledFeatures.contains(feature);
    }
    
    public Set<String> getEnabledFeatures() {
        return new HashSet<>(enabledFeatures);
    }
}
```

---

## Object Pools and Performance

### Object Pool Implementation

```java
// ObjectPool.java - Generic object pool for performance optimization
package com.javajourney.patterns.prototype;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Generic Object Pool Implementation
 * Manages expensive object creation and reuse for performance optimization
 */
public class ObjectPool<T> {
    
    private final ConcurrentLinkedQueue<T> pool;
    private final Supplier<T> objectFactory;
    private final int maxPoolSize;
    private final AtomicInteger createdObjects;
    private final AtomicInteger pooledObjects;
    
    public ObjectPool(Supplier<T> objectFactory, int maxPoolSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.objectFactory = objectFactory;
        this.maxPoolSize = maxPoolSize;
        this.createdObjects = new AtomicInteger(0);
        this.pooledObjects = new AtomicInteger(0);
        
        // Pre-populate pool with initial objects
        int initialSize = Math.min(5, maxPoolSize);
        for (int i = 0; i < initialSize; i++) {
            pool.offer(createNewObject());
            pooledObjects.incrementAndGet();
        }
    }
    
    // Get object from pool or create new one
    public T borrowObject() {
        T object = pool.poll();
        if (object != null) {
            pooledObjects.decrementAndGet();
            return object;
        } else {
            return createNewObject();
        }
    }
    
    // Return object to pool
    public void returnObject(T object) {
        if (object != null && pooledObjects.get() < maxPoolSize) {
            // Reset object state if needed (implement Resettable interface)
            if (object instanceof Resettable) {
                ((Resettable) object).reset();
            }
            
            pool.offer(object);
            pooledObjects.incrementAndGet();
        }
        // If pool is full, object will be garbage collected
    }
    
    private T createNewObject() {
        createdObjects.incrementAndGet();
        return objectFactory.get();
    }
    
    // Pool statistics
    public int getPoolSize() {
        return pooledObjects.get();
    }
    
    public int getTotalCreated() {
        return createdObjects.get();
    }
    
    public int getActiveObjects() {
        return createdObjects.get() - pooledObjects.get();
    }
    
    public void clear() {
        pool.clear();
        pooledObjects.set(0);
    }
    
    public PoolStatistics getStatistics() {
        return new PoolStatistics(
            pooledObjects.get(),
            createdObjects.get(),
            getActiveObjects(),
            maxPoolSize
        );
    }
}

// Interface for objects that can be reset
interface Resettable {
    void reset();
}

// Pool statistics class
class PoolStatistics {
    private final int pooledObjects;
    private final int totalCreated;
    private final int activeObjects;
    private final int maxPoolSize;
    
    public PoolStatistics(int pooledObjects, int totalCreated, int activeObjects, int maxPoolSize) {
        this.pooledObjects = pooledObjects;
        this.totalCreated = totalCreated;
        this.activeObjects = activeObjects;
        this.maxPoolSize = maxPoolSize;
    }
    
    public int getPooledObjects() { return pooledObjects; }
    public int getTotalCreated() { return totalCreated; }
    public int getActiveObjects() { return activeObjects; }
    public int getMaxPoolSize() { return maxPoolSize; }
    
    public double getPoolUtilization() {
        return maxPoolSize > 0 ? (double) pooledObjects / maxPoolSize : 0.0;
    }
    
    public double getCreationRate() {
        return activeObjects > 0 ? (double) totalCreated / activeObjects : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("PoolStatistics[pooled=%d, created=%d, active=%d, max=%d, utilization=%.2f%%]",
            pooledObjects, totalCreated, activeObjects, maxPoolSize, getPoolUtilization() * 100);
    }
}

// Example: Database Connection Pool using Object Pool
class DatabaseConnectionPool {
    private final ObjectPool<DatabaseConnection> connectionPool;
    private final String databaseUrl;
    private final String username;
    private final String password;
    
    public DatabaseConnectionPool(String databaseUrl, String username, 
                                 String password, int maxConnections) {
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
        
        this.connectionPool = new ObjectPool<>(this::createConnection, maxConnections);
    }
    
    private DatabaseConnection createConnection() {
        // Simulate expensive connection creation
        try {
            Thread.sleep(100); // Simulate connection time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return new PooledDatabaseConnection(databaseUrl, username, password);
    }
    
    public DatabaseConnection getConnection() {
        return connectionPool.borrowObject();
    }
    
    public void releaseConnection(DatabaseConnection connection) {
        connectionPool.returnObject(connection);
    }
    
    public PoolStatistics getStatistics() {
        return connectionPool.getStatistics();
    }
}

// Pooled database connection that implements Resettable
class PooledDatabaseConnection implements DatabaseConnection, Resettable {
    private final String url;
    private final String username;
    private final String password;
    private boolean inUse;
    private long lastUsed;
    
    public PooledDatabaseConnection(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.inUse = false;
        this.lastUsed = System.currentTimeMillis();
    }
    
    @Override
    public void reset() {
        this.inUse = false;
        this.lastUsed = System.currentTimeMillis();
        // Reset any other state as needed
    }
    
    @Override
    public java.sql.Connection getConnection() {
        inUse = true;
        lastUsed = System.currentTimeMillis();
        // Return actual database connection
        return null; // Placeholder
    }
    
    @Override
    public String getConnectionInfo() {
        return "Pooled connection to: " + url;
    }
    
    @Override
    public void closeConnection() {
        // Don't actually close - will be returned to pool
        inUse = false;
    }
    
    @Override
    public boolean testConnection() {
        return true; // Simplified implementation
    }
    
    public boolean isInUse() { return inUse; }
    public long getLastUsed() { return lastUsed; }
}
```

---

## Hands-On Practice

### Practice 1: Document Builder

```java
// DocumentBuilder.java - Practice building complex documents
package com.javajourney.patterns.practice;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Document Builder Practice
 * Build complex documents with various sections and formatting
 */
public class DocumentBuilder {
    
    private StringBuilder content;
    private String title;
    private String author;
    private LocalDateTime createdDate;
    private List<String> sections;
    private Map<String, String> metadata;
    private DocumentFormat format;
    private int pageNumber;
    
    public enum DocumentFormat {
        PLAIN_TEXT, HTML, MARKDOWN, PDF
    }
    
    public DocumentBuilder() {
        this.content = new StringBuilder();
        this.sections = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.format = DocumentFormat.PLAIN_TEXT;
        this.createdDate = LocalDateTime.now();
        this.pageNumber = 1;
    }
    
    // Document metadata
    public DocumentBuilder title(String title) {
        this.title = title;
        return this;
    }
    
    public DocumentBuilder author(String author) {
        this.author = author;
        return this;
    }
    
    public DocumentBuilder format(DocumentFormat format) {
        this.format = format;
        return this;
    }
    
    public DocumentBuilder metadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }
    
    // Content building methods
    public DocumentBuilder heading(String text, int level) {
        sections.add("Heading" + level);
        
        switch (format) {
            case HTML -> content.append(String.format("<h%d>%s</h%d>\n", level, text, level));
            case MARKDOWN -> content.append("#".repeat(level)).append(" ").append(text).append("\n\n");
            default -> {
                content.append("\n");
                content.append("=".repeat(text.length())).append("\n");
                content.append(text.toUpperCase()).append("\n");
                content.append("=".repeat(text.length())).append("\n\n");
            }
        }
        return this;
    }
    
    public DocumentBuilder paragraph(String text) {
        switch (format) {
            case HTML -> content.append("<p>").append(text).append("</p>\n");
            case MARKDOWN -> content.append(text).append("\n\n");
            default -> content.append(text).append("\n\n");
        }
        return this;
    }
    
    public DocumentBuilder bulletPoint(String text) {
        switch (format) {
            case HTML -> content.append("<li>").append(text).append("</li>\n");
            case MARKDOWN -> content.append("- ").append(text).append("\n");
            default -> content.append("• ").append(text).append("\n");
        }
        return this;
    }
    
    public DocumentBuilder bulletList(String... items) {
        if (format == DocumentFormat.HTML) {
            content.append("<ul>\n");
        }
        
        for (String item : items) {
            bulletPoint(item);
        }
        
        if (format == DocumentFormat.HTML) {
            content.append("</ul>\n");
        } else {
            content.append("\n");
        }
        return this;
    }
    
    public DocumentBuilder numberedList(String... items) {
        if (format == DocumentFormat.HTML) {
            content.append("<ol>\n");
        }
        
        for (int i = 0; i < items.length; i++) {
            switch (format) {
                case HTML -> content.append("<li>").append(items[i]).append("</li>\n");
                case MARKDOWN -> content.append(i + 1).append(". ").append(items[i]).append("\n");
                default -> content.append(i + 1).append(". ").append(items[i]).append("\n");
            }
        }
        
        if (format == DocumentFormat.HTML) {
            content.append("</ol>\n");
        } else {
            content.append("\n");
        }
        return this;
    }
    
    public DocumentBuilder codeBlock(String code, String language) {
        switch (format) {
            case HTML -> content.append("<pre><code class=\"").append(language).append("\">")
                               .append(code).append("</code></pre>\n");
            case MARKDOWN -> content.append("```").append(language).append("\n")
                                   .append(code).append("\n```\n\n");
            default -> {
                content.append("CODE BLOCK (").append(language).append("):\n");
                content.append("-".repeat(40)).append("\n");
                content.append(code).append("\n");
                content.append("-".repeat(40)).append("\n\n");
            }
        }
        return this;
    }
    
    public DocumentBuilder table(String[] headers, String[][] data) {
        switch (format) {
            case HTML -> {
                content.append("<table>\n<thead>\n<tr>");
                for (String header : headers) {
                    content.append("<th>").append(header).append("</th>");
                }
                content.append("</tr>\n</thead>\n<tbody>\n");
                
                for (String[] row : data) {
                    content.append("<tr>");
                    for (String cell : row) {
                        content.append("<td>").append(cell).append("</td>");
                    }
                    content.append("</tr>\n");
                }
                content.append("</tbody>\n</table>\n");
            }
            case MARKDOWN -> {
                // Headers
                content.append("| ");
                for (String header : headers) {
                    content.append(header).append(" | ");
                }
                content.append("\n");
                
                // Separator
                content.append("|");
                for (int i = 0; i < headers.length; i++) {
                    content.append("---|");
                }
                content.append("\n");
                
                // Data rows
                for (String[] row : data) {
                    content.append("| ");
                    for (String cell : row) {
                        content.append(cell).append(" | ");
                    }
                    content.append("\n");
                }
                content.append("\n");
            }
            default -> {
                // Calculate column widths
                int[] widths = new int[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    widths[i] = headers[i].length();
                    for (String[] row : data) {
                        if (i < row.length) {
                            widths[i] = Math.max(widths[i], row[i].length());
                        }
                    }
                }
                
                // Print table
                printTableRow(headers, widths);
                content.append("+");
                for (int width : widths) {
                    content.append("-".repeat(width + 2)).append("+");
                }
                content.append("\n");
                
                for (String[] row : data) {
                    printTableRow(row, widths);
                }
                content.append("\n");
            }
        }
        return this;
    }
    
    private void printTableRow(String[] row, int[] widths) {
        content.append("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < row.length ? row[i] : "";
            content.append(" ").append(String.format("%-" + widths[i] + "s", cell)).append(" |");
        }
        content.append("\n");
    }
    
    public DocumentBuilder pageBreak() {
        switch (format) {
            case HTML -> content.append("<div style=\"page-break-after: always;\"></div>\n");
            case PDF -> content.append("<!-- PAGE BREAK -->\n");
            default -> {
                content.append("\n");
                content.append("=".repeat(50)).append("\n");
                content.append("PAGE ").append(++pageNumber).append("\n");
                content.append("=".repeat(50)).append("\n\n");
            }
        }
        return this;
    }
    
    public DocumentBuilder horizontalRule() {
        switch (format) {
            case HTML -> content.append("<hr>\n");
            case MARKDOWN -> content.append("---\n\n");
            default -> content.append("-".repeat(50)).append("\n\n");
        }
        return this;
    }
    
    // Build final document
    public String build() {
        StringBuilder document = new StringBuilder();
        
        // Add document header based on format
        switch (format) {
            case HTML -> {
                document.append("<!DOCTYPE html>\n<html>\n<head>\n");
                document.append("<title>").append(title != null ? title : "Document").append("</title>\n");
                document.append("<meta charset=\"UTF-8\">\n");
                document.append("</head>\n<body>\n");
                
                if (title != null) {
                    document.append("<h1>").append(title).append("</h1>\n");
                }
                if (author != null) {
                    document.append("<p><em>By: ").append(author).append("</em></p>\n");
                }
                
                document.append(content);
                document.append("</body>\n</html>");
            }
            default -> {
                if (title != null) {
                    document.append(title.toUpperCase()).append("\n");
                    document.append("=".repeat(title.length())).append("\n\n");
                }
                if (author != null) {
                    document.append("By: ").append(author).append("\n");
                }
                document.append("Created: ").append(createdDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
                
                document.append(content);
            }
        }
        
        return document.toString();
    }
    
    // Get document info
    public DocumentInfo getDocumentInfo() {
        return new DocumentInfo(title, author, format, sections.size(), 
                               content.length(), createdDate, metadata);
    }
}

// Document information class
class DocumentInfo {
    private final String title;
    private final String author;
    private final DocumentBuilder.DocumentFormat format;
    private final int sectionCount;
    private final int contentLength;
    private final LocalDateTime createdDate;
    private final Map<String, String> metadata;
    
    public DocumentInfo(String title, String author, DocumentBuilder.DocumentFormat format,
                       int sectionCount, int contentLength, LocalDateTime createdDate,
                       Map<String, String> metadata) {
        this.title = title;
        this.author = author;
        this.format = format;
        this.sectionCount = sectionCount;
        this.contentLength = contentLength;
        this.createdDate = createdDate;
        this.metadata = new HashMap<>(metadata);
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public DocumentBuilder.DocumentFormat getFormat() { return format; }
    public int getSectionCount() { return sectionCount; }
    public int getContentLength() { return contentLength; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    
    @Override
    public String toString() {
        return String.format("DocumentInfo[title=%s, author=%s, format=%s, sections=%d, length=%d]",
            title, author, format, sectionCount, contentLength);
    }
}
```

### Practice 2: Comprehensive Demo Application

```java
// BuilderPrototypeDemo.java - Comprehensive demonstration
package com.javajourney.patterns;

/**
 * Comprehensive Demonstration of Builder and Prototype Patterns
 */
public class BuilderPrototypeDemo {
    
    public static void main(String[] args) {
        demonstrateComputerBuilder();
        System.out.println();
        
        demonstrateSQLQueryBuilder();
        System.out.println();
        
        demonstrateGameObjectPrototype();
        System.out.println();
        
        demonstrateConfigurationTemplate();
        System.out.println();
        
        demonstrateDocumentBuilder();
        System.out.println();
        
        demonstrateObjectPool();
    }
    
    private static void demonstrateComputerBuilder() {
        System.out.println("=== Computer Builder Demo ===");
        
        // Gaming computer
        Computer gamingPC = new Computer.Builder("Intel i9-13900K", "32GB DDR5")
            .gamingConfiguration()
            .storage("2TB NVMe SSD")
            .hasWiFi(true)
            .hasBluetooth(true)
            .build();
        
        System.out.println("Gaming PC:");
        System.out.println(gamingPC);
        
        // Office computer
        Computer officePC = new Computer.Builder("Intel i5-13400", "16GB DDR4")
            .officeConfiguration()
            .build();
        
        System.out.println("Office PC:");
        System.out.println(officePC);
    }
    
    private static void demonstrateSQLQueryBuilder() {
        System.out.println("=== SQL Query Builder Demo ===");
        
        // Complex query with joins and conditions
        SQLQueryBuilder queryBuilder = new SQLQueryBuilder();
        
        String query = queryBuilder
            .select("u.username", "u.email", "p.title", "c.name as category")
            .from("users", "u")
            .leftJoin("posts p", "u.id = p.user_id")
            .leftJoin("categories c", "p.category_id = c.id")
            .whereEquals("u.active", true)
            .whereIn("c.name", "Technology", "Programming", "Java")
            .whereBetween("p.created_date", "2024-01-01", "2024-12-31")
            .orderBy("u.username")
            .orderByDesc("p.created_date")
            .limit(50)
            .build();
        
        System.out.println("Generated SQL Query:");
        System.out.println(query);
        System.out.println("Parameters: " + queryBuilder.getParameters());
        
        // Simple query
        queryBuilder.reset();
        String simpleQuery = queryBuilder
            .selectAll()
            .from("products")
            .whereEquals("category", "Electronics")
            .orderBy("price")
            .page(2, 20)
            .build();
        
        System.out.println("\nSimple Query:");
        System.out.println(simpleQuery);
    }
    
    private static void demonstrateGameObjectPrototype() {
        System.out.println("=== Game Object Prototype Demo ===");
        
        // Create template objects
        Warrior warriorTemplate = new Warrior("template", "Warrior Template", 0, 0, 100, 15, 10, "Sword");
        Mage mageTemplate = new Mage("template", "Mage Template", 0, 0, 80, 120, 20);
        mageTemplate.addSpell("Fireball");
        mageTemplate.addSpell("Lightning");
        mageTemplate.addSpell("Heal");
        
        Archer archerTemplate = new Archer("template", "Archer Template", 0, 0, 90, 18, 30, "Longbow");
        
        System.out.println("Template Objects Created:");
        System.out.println(warriorTemplate);
        System.out.println(mageTemplate);
        System.out.println(archerTemplate);
        
        // Clone objects for game instances
        System.out.println("\nCloning Objects for Game:");
        
        Warrior warrior1 = (Warrior) warriorTemplate.clone();
        warrior1.setId("warrior1");
        warrior1.setName("Conan");
        warrior1.move(10, 5);
        
        Mage mage1 = (Mage) mageTemplate.clone();
        mage1.setId("mage1");
        mage1.setName("Gandalf");
        mage1.move(15, 8);
        
        Archer archer1 = (Archer) archerTemplate.clone();
        archer1.setId("archer1");
        archer1.setName("Legolas");
        archer1.move(12, 7);
        
        System.out.println("Cloned Game Objects:");
        System.out.println(warrior1);
        System.out.println(mage1);
        System.out.println(archer1);
        
        // Demonstrate that changes to clones don't affect templates
        warrior1.takeDamage(20);
        mage1.castSpell("Fireball", 10);
        archer1.rangedAttack();
        
        System.out.println("\nAfter Actions (templates unchanged):");
        System.out.println("Template: " + warriorTemplate);
        System.out.println("Clone: " + warrior1);
    }
    
    private static void demonstrateConfigurationTemplate() {
        System.out.println("=== Configuration Template Demo ===");
        
        // Create master template
        ConfigurationTemplate masterTemplate = new ConfigurationTemplate("MasterConfig", "1.0");
        masterTemplate.setSetting("app.name", "Java Learning Journey");
        masterTemplate.setSetting("app.version", "1.0.0");
        masterTemplate.setSetting("logging.level", "INFO");
        
        DatabaseConfig primaryDB = new DatabaseConfig("primary", "PostgreSQL", "localhost", 5432);
        primaryDB.setUsername("admin");
        primaryDB.setPassword("secret");
        masterTemplate.addDatabaseConfig(primaryDB);
        
        DatabaseConfig cacheDB = new DatabaseConfig("cache", "Redis", "localhost", 6379);
        masterTemplate.addDatabaseConfig(cacheDB);
        
        System.out.println("Master Template: " + masterTemplate);
        
        // Create environment-specific configurations
        ConfigurationTemplate devConfig = masterTemplate.deepClone();
        devConfig.setTemplateName("DevelopmentConfig");
        devConfig.setSetting("logging.level", "DEBUG");
        devConfig.setSetting("debug.enabled", true);
        
        ConfigurationTemplate prodConfig = masterTemplate.serializationClone();
        prodConfig.setTemplateName("ProductionConfig");
        prodConfig.setSetting("logging.level", "WARN");
        prodConfig.setSetting("security.strict", true);
        
        System.out.println("Development Config: " + devConfig);
        System.out.println("Production Config: " + prodConfig);
        
        // Verify independence
        masterTemplate.setSetting("test.change", "original");
        System.out.println("\nAfter modifying master template:");
        System.out.println("Master has test.change: " + (masterTemplate.getSetting("test.change") != null));
        System.out.println("Dev has test.change: " + (devConfig.getSetting("test.change") != null));
        System.out.println("Prod has test.change: " + (prodConfig.getSetting("test.change") != null));
    }
    
    private static void demonstrateDocumentBuilder() {
        System.out.println("=== Document Builder Demo ===");
        
        // Create a technical document
        String document = new DocumentBuilder()
            .title("Design Patterns Guide")
            .author("Java Learning Journey")
            .format(DocumentBuilder.DocumentFormat.MARKDOWN)
            .metadata("version", "1.0")
            .metadata("category", "Technical")
            .heading("Introduction", 1)
            .paragraph("Design patterns are reusable solutions to common problems in software design.")
            .heading("Creational Patterns", 2)
            .paragraph("Creational patterns deal with object creation mechanisms.")
            .bulletList(
                "Singleton - Ensures single instance",
                "Factory - Creates objects without specifying exact classes",
                "Builder - Constructs complex objects step by step",
                "Prototype - Creates objects by cloning existing instances"
            )
            .heading("Code Example", 3)
            .codeBlock(
                "Computer pc = new Computer.Builder(\"Intel i7\", \"16GB\")\n" +
                "    .gamingConfiguration()\n" +
                "    .build();",
                "java"
            )
            .horizontalRule()
            .heading("Summary", 2)
            .paragraph("Understanding design patterns is crucial for writing maintainable code.")
            .build();
        
        System.out.println("Generated Document:");
        System.out.println(document);
    }
    
    private static void demonstrateObjectPool() {
        System.out.println("=== Object Pool Demo ===");
        
        // Create connection pool
        DatabaseConnectionPool connectionPool = new DatabaseConnectionPool(
            "jdbc:postgresql://localhost:5432/mydb", "user", "password", 5);
        
        System.out.println("Initial pool state: " + connectionPool.getStatistics());
        
        // Simulate connection usage
        List<DatabaseConnection> connections = new ArrayList<>();
        
        // Borrow connections
        for (int i = 0; i < 7; i++) {
            DatabaseConnection conn = connectionPool.getConnection();
            connections.add(conn);
            System.out.println("Borrowed connection " + (i + 1) + ": " + connectionPool.getStatistics());
        }
        
        // Return some connections
        for (int i = 0; i < 3; i++) {
            connectionPool.releaseConnection(connections.get(i));
            System.out.println("Returned connection " + (i + 1) + ": " + connectionPool.getStatistics());
        }
        
        // Borrow again (should reuse pooled connections)
        DatabaseConnection reusedConn = connectionPool.getConnection();
        System.out.println("Reused connection: " + connectionPool.getStatistics());
    }
}
```

---

## Challenges

### Challenge 1: Email Builder
Create a comprehensive email builder that supports HTML and plain text formats, attachments, templates, and batch sending.

### Challenge 2: Game Character Prototype Manager
Design a prototype manager that can create, store, and clone different character templates with complex nested properties and abilities.

### Challenge 3: Query Result Cache
Implement a prototype-based cache for database query results that can clone and return cached data efficiently.

---

## Summary

### Key Takeaways

1. **Builder Pattern Benefits**:
   - Handles complex object construction elegantly
   - Provides fluent API for better readability
   - Separates construction logic from representation
   - Enables step-by-step object creation with validation

2. **Prototype Pattern Benefits**:
   - Reduces expensive object creation overhead
   - Provides alternative to complex inheritance hierarchies
   - Enables runtime configuration of object behavior
   - Supports deep and shallow cloning strategies

3. **Object Pools Benefits**:
   - Manages expensive resource creation
   - Improves performance through object reuse
   - Reduces garbage collection pressure
   - Provides controlled resource allocation

4. **Implementation Considerations**:
   - Choose appropriate cloning strategy (shallow vs deep)
   - Implement proper validation in builders
   - Handle thread safety in object pools
   - Consider memory implications of prototypes

### Performance Checklist
- [ ] Object pools properly limit resource usage
- [ ] Deep cloning is implemented correctly for complex objects
- [ ] Builder validation prevents invalid object states
- [ ] Prototype pattern reduces object creation overhead
- [ ] Memory leaks are prevented in pooled objects

### Quality Gates Checklist
- [ ] All builders provide fluent API with method chaining
- [ ] Prototype cloning preserves object integrity
- [ ] Object pools implement proper lifecycle management
- [ ] Error handling covers all edge cases
- [ ] Thread safety is maintained where required

### Tomorrow's Preview
Tomorrow we'll explore **Structural Patterns - Adapter & Decorator** where we'll learn to make incompatible interfaces work together and add behavior to objects dynamically.

---

*Day 51 Complete! You've mastered Builder and Prototype patterns for constructing complex objects and optimizing object creation. You can now build sophisticated object creation systems that are both performant and maintainable. Tomorrow we'll dive into structural patterns that help organize and compose objects effectively.*