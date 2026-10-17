package be.abis.sandwich.model;

import be.abis.sandwich.enumeration.BreadType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPriceTest {

    private final Person ada = new Person("Ada", "Lovelace");

    private SandwichOrderDetail detail(double basePrice, int amount) {
        return new SandwichOrderDetail(new Sandwich(1, "Test", "", "Vlees", basePrice), amount, BreadType.WHITE, true, "", ada);
    }

    @Test
    void thePriceOfADetailIsTheBasePriceTimesTheAmount() {
        assertThat(detail(3.2, 2).getPrice()).isEqualByComparingTo(new BigDecimal("6.4"));
    }

    @Test
    void theBreadAndTheVegetablesDoNotChangeThePrice() {
        SandwichOrderDetail brownWithout = new SandwichOrderDetail(new Sandwich(1, "Test", "", "Vlees", 3.2), 1, BreadType.BROWN, false, "", ada);

        assertThat(brownWithout.getPrice()).isEqualByComparingTo(detail(3.2, 1).getPrice());
    }

    @Test
    void theTotalIsTheSumOfThePrices() {
        assertThat(SandwichOrder.totalPriceOf(List.of(detail(4.8, 1), detail(3.2, 2)))).isEqualTo(11.2);
    }

    @Test
    void theTotalOfNothingIsZero() {
        assertThat(SandwichOrder.totalPriceOf(List.of())).isZero();
    }

    @Test
    void theTotalDoesNotSufferFromFloatingPointErrors() {
        // with doubles 0.1 + 0.2 is 0.30000000000000004
        assertThat(SandwichOrder.totalPriceOf(List.of(detail(0.1, 1), detail(0.2, 1)))).isEqualTo(0.3);
    }

    @Test
    void theTotalIsRoundedToTwoDecimals() {
        assertThat(SandwichOrder.totalPriceOf(List.of(detail(1.005, 1)))).isEqualTo(1.01);
    }
}
