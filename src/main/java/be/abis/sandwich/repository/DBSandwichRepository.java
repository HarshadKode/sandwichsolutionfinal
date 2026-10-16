package be.abis.sandwich.repository;

import be.abis.sandwich.model.Sandwich;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class DBSandwichRepository implements SandwichRepository {

    private static final String SELECT = "SELECT id, name, description, category, base_price FROM sandwiches";

    private final JdbcClient jdbc;

    public DBSandwichRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Sandwich> findAllSandwiches() {
        return jdbc.sql(SELECT + " ORDER BY category, name")
                .query(this::mapSandwich)
                .list();
    }

    @Override
    public Optional<Sandwich> findSandwichById(int id) {
        return jdbc.sql(SELECT + " WHERE id = :id")
                .param("id", id)
                .query(this::mapSandwich)
                .optional();
    }

    @Override
    public Optional<Sandwich> findSandwichByName(String name) {
        return jdbc.sql(SELECT + " WHERE LOWER(name) = LOWER(:name)")
                .param("name", name)
                .query(this::mapSandwich)
                .optional();
    }

    @Override
    public List<Sandwich> findSandwichesByCategory(String category) {
        return jdbc.sql(SELECT + " WHERE LOWER(category) = LOWER(:category) ORDER BY name")
                .param("category", category)
                .query(this::mapSandwich)
                .list();
    }

    @Override
    public Sandwich addSandwich(Sandwich sandwich) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO sandwiches (name, description, category, base_price) " +
                        "VALUES (:name, :description, :category, :basePrice)")
                .param("name", sandwich.getName())
                .param("description", sandwich.getDescription())
                .param("category", sandwich.getCategory())
                .param("basePrice", sandwich.getBasePrice())
                .update(keys, "ID");
        sandwich.setId(keys.getKey().intValue());
        return sandwich;
    }

    @Override
    public void updatePrice(Sandwich sandwich) {
        jdbc.sql("UPDATE sandwiches SET base_price = :basePrice WHERE id = :id")
                .param("basePrice", sandwich.getBasePrice())
                .param("id", sandwich.getId())
                .update();
    }

    @Override
    public void deleteSandwich(int id) {
        jdbc.sql("DELETE FROM sandwiches WHERE id = :id")
                .param("id", id)
                .update();
    }

    private Sandwich mapSandwich(ResultSet rs, int rowNum) throws SQLException {
        return new Sandwich(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                rs.getString("category"), rs.getDouble("base_price"));
    }
}
