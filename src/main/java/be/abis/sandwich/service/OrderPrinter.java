package be.abis.sandwich.service;

import be.abis.sandwich.exception.PrintOrderException;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Writes an order to a readable .txt file. */
@Component
public class OrderPrinter {

    private final Path directory;

    public OrderPrinter(@Value("${sandwich.print.directory:orders}") String directory) {
        this.directory = Path.of(directory);
    }

    /** Writes the order to {@code sandwich-order_<date>_<course>.txt} and returns the text that was written. */
    public String print(SandwichOrder order, List<SandwichOrderDetail> details) {
        String text = format(order, details);
        Path file = directory.resolve("sandwich-order_" + order.getOrderDate() + "_" + slug(order.getCourse().getTitle()) + ".txt");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PrintOrderException("Could not write the order to " + file.toAbsolutePath(), e);
        }
        return text;
    }

    private static String format(SandwichOrder order, List<SandwichOrderDetail> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("SANDWICH ORDER\n");
        sb.append("Course: ").append(order.getCourse().getTitle()).append('\n');
        sb.append("Date:   ").append(order.getOrderDate()).append("\n\n");

        sb.append("Per person\n----------\n");
        Map<String, List<SandwichOrderDetail>> perPerson = new LinkedHashMap<>();
        for (SandwichOrderDetail d : details) {
            perPerson.computeIfAbsent(d.getPerson().getFirstName() + " " + d.getPerson().getLastName(), k -> new ArrayList<>()).add(d);
        }
        perPerson.forEach((person, list) -> {
            sb.append(person).append('\n');
            for (SandwichOrderDetail d : list) {
                sb.append(String.format(Locale.ROOT, "  %dx %-22s %-6s bread, %s vegetables  %s\n",
                        d.getAmount(), d.getSandwich().getName(), d.getBreadType().name().toLowerCase(Locale.ROOT),
                        d.isVegetables() ? "with" : "no  ", euro(d.getPrice())));
                if (d.getComment() != null && !d.getComment().isBlank()) {
                    sb.append("     remark: ").append(d.getComment()).append('\n');
                }
            }
        });

        sb.append("\nTo order\n--------\n");
        Map<String, Integer> summary = new TreeMap<>();
        for (SandwichOrderDetail d : details) {
            String line = String.format(Locale.ROOT, "%-22s %-6s bread, %s vegetables", d.getSandwich().getName(),
                    d.getBreadType().name().toLowerCase(Locale.ROOT), d.isVegetables() ? "with" : "no  ");
            summary.merge(line, d.getAmount(), Integer::sum);
        }
        summary.forEach((line, amount) -> sb.append(String.format(Locale.ROOT, "  %dx %s\n", amount, line)));

        sb.append("\nTotal: ").append(euro(BigDecimal.valueOf(SandwichOrder.totalPriceOf(details)))).append('\n');
        return sb.toString();
    }

    private static String euro(BigDecimal amount) {
        return String.format(Locale.ROOT, "€ %.2f", amount);
    }

    private static String slug(String title) {
        String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "course" : slug;
    }
}
