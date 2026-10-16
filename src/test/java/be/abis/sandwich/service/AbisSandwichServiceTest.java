package be.abis.sandwich.service;

import be.abis.sandwich.exception.DuplicateSandwichException;
import be.abis.sandwich.exception.SandwichInUseException;
import be.abis.sandwich.exception.SandwichNotFoundException;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.repository.SandwichRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbisSandwichServiceTest {

    private final Sandwich hesp = new Sandwich(1, "Hesp", "Ham", "Vlees", 3.2);
    private final Sandwich pinky = new Sandwich(2, "Pinky", "Special", "Specials", 4.8);
    private final Sandwich kipCurry = new Sandwich(3, "Kip curry", "Chicken", "Kip", 3.6);

    @Mock
    private SandwichRepository sandwichRepository;
    @InjectMocks
    private AbisSandwichService service;

    @Test
    void withoutFiltersAllSandwichesAreReturned() {
        when(sandwichRepository.findAllByOrderByCategoryAscNameAsc()).thenReturn(List.of(hesp, pinky));

        assertThat(service.findSandwiches(null, null)).containsExactly(hesp, pinky);
        assertThat(service.findSandwiches(" ", "")).containsExactly(hesp, pinky);
    }

    @Test
    void categoryFilterUsesTheRepository() {
        when(sandwichRepository.findByCategoryIgnoreCaseOrderByName("Kip")).thenReturn(List.of(kipCurry));

        assertThat(service.findSandwiches("Kip", null)).containsExactly(kipCurry);
        verify(sandwichRepository, never()).findAllByOrderByCategoryAscNameAsc();
    }

    @Test
    void nameFilterMatchesPartOfTheNameIgnoringCase() {
        when(sandwichRepository.findAllByOrderByCategoryAscNameAsc()).thenReturn(List.of(hesp, pinky, kipCurry));

        assertThat(service.findSandwiches(null, "IN")).containsExactly(pinky);
        assertThat(service.findSandwiches(null, "i")).containsExactly(pinky, kipCurry);
    }

    @Test
    void categoryAndNameFiltersCanBeCombined() {
        when(sandwichRepository.findByCategoryIgnoreCaseOrderByName("Vlees")).thenReturn(List.of(hesp));

        assertThat(service.findSandwiches("Vlees", "kip")).isEmpty();
        assertThat(service.findSandwiches("Vlees", "hes")).containsExactly(hesp);
    }

    @Test
    void findSandwichByIdThrowsWhenUnknown() {
        when(sandwichRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findSandwichById(99))
                .isInstanceOf(SandwichNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void addSandwichSavesANewSandwich() {
        Sandwich fresh = new Sandwich(0, "Nieuw", "New", "Vlees", 3);
        when(sandwichRepository.findByNameIgnoreCase("Nieuw")).thenReturn(Optional.empty());
        when(sandwichRepository.save(fresh)).thenReturn(fresh);

        assertThat(service.addSandwich(fresh)).isSameAs(fresh);
        verify(sandwichRepository).save(fresh);
    }

    @Test
    void addSandwichRejectsADuplicateName() {
        when(sandwichRepository.findByNameIgnoreCase("Hesp")).thenReturn(Optional.of(hesp));

        assertThatThrownBy(() -> service.addSandwich(new Sandwich(0, "Hesp", "again", "Vlees", 1)))
                .isInstanceOf(DuplicateSandwichException.class);
        verify(sandwichRepository, never()).save(any());
    }

    @Test
    void updatePriceSavesTheNewPrice() {
        when(sandwichRepository.findById(1)).thenReturn(Optional.of(hesp));
        when(sandwichRepository.save(hesp)).thenReturn(hesp);

        Sandwich updated = service.updatePrice(1, 4.0);

        assertThat(updated.getBasePrice()).isEqualTo(4.0);
        verify(sandwichRepository).save(hesp);
    }

    @Test
    void updatePriceThrowsWhenTheSandwichDoesNotExist() {
        when(sandwichRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePrice(99, 1)).isInstanceOf(SandwichNotFoundException.class);
        verify(sandwichRepository, never()).save(any());
    }

    @Test
    void deleteSandwichRemovesIt() {
        when(sandwichRepository.findById(1)).thenReturn(Optional.of(hesp));

        service.deleteSandwich(1);

        verify(sandwichRepository).delete(hesp);
    }

    @Test
    void deleteSandwichThatIsPartOfAnOrderIsRefused() {
        when(sandwichRepository.findById(1)).thenReturn(Optional.of(hesp));
        doThrow(new DataIntegrityViolationException("foreign key")).when(sandwichRepository).delete(hesp);

        assertThatThrownBy(() -> service.deleteSandwich(1))
                .isInstanceOf(SandwichInUseException.class)
                .hasMessageContaining("Hesp");
    }

    @Test
    void deleteSandwichThrowsWhenUnknown() {
        when(sandwichRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSandwich(99)).isInstanceOf(SandwichNotFoundException.class);
        verify(sandwichRepository, never()).delete(any());
    }
}
