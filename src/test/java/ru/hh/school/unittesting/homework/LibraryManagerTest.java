package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("book1", 1);
    libraryManager.addBook("book2", 3);
    libraryManager.addBook("book1", 1);
  }

  @Test
  void testAddBook() {
    assertEquals(2, libraryManager.getAvailableCopies("book1"));
    assertEquals(3, libraryManager.getAvailableCopies("book2"));
    assertEquals(0, libraryManager.getAvailableCopies("book3"));
  }

  @Test
  void testBorrowBookWithUserInactive() {
    when(userService.isUserActive("Ivan")).thenReturn(false);
    assertFalse(libraryManager.borrowBook("book1", "Ivan"));
    verify(userService, times(1)).isUserActive("Ivan");
    verify(notificationService, times(1))
        .notifyUser("Ivan", "Your account is not active.");
  }

  @Test
  void testBorrowBookWithNonExistBook() {
    when(userService.isUserActive("Petr")).thenReturn(true);
    assertFalse(libraryManager.borrowBook("book3", "Petr"));
    verify(userService, times(1)).isUserActive("Petr");
  }

  @Test
  void testBorrowBookWithExistBook() {
    when(userService.isUserActive("Petr")).thenReturn(true);
    assertTrue(libraryManager.borrowBook("book2", "Petr"));
    assertEquals(2, libraryManager.getAvailableCopies("book2"));
    verify(userService, times(1)).isUserActive("Petr");
    verify(notificationService, times(1))
        .notifyUser("Petr", "You have borrowed the book: book2");
  }

  @Test
  void testReturnBookWithNonBorrowedBook() {
    when(userService.isUserActive("Petr")).thenReturn(true);
    libraryManager.borrowBook("book2", "Petr");
    assertFalse(libraryManager.returnBook("book1", "Petr"));
  }

  @Test
  void testReturnBookWithNonBorrowedUser() {
    when(userService.isUserActive("Petr")).thenReturn(true);
    libraryManager.borrowBook("book2", "Petr");
    assertFalse(libraryManager.returnBook("book2", "Pasha"));
  }

  @Test
  void testReturnBookWithBorrowedBookAndBorrowedUser() {
    when(userService.isUserActive("Petr")).thenReturn(true);
    libraryManager.borrowBook("book2", "Petr");
    assertTrue(libraryManager.returnBook("book2", "Petr"));
    assertEquals(3, libraryManager.getAvailableCopies("book2"));
    verify(notificationService, times(1))
        .notifyUser("Petr", "You have returned the book: book2");
  }

  @ParameterizedTest
  @CsvSource({
      "0, false, false, 0",
      "3, false, false, 1.5",
      "4, true, false, 3",
      "5, true, false, 3.75",
      "30, false, true, 12",
      "400, true, true, 240"
  })
  void testCalculateDynamicFeeLate(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedLateFee
  ) {
    assertEquals(
        expectedLateFee,
        libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember)
    );
  }

  @Test
  void calculateDynamicFeeLateShouldThrowExceptionIfNegativeOverdueDays() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-5, false, false)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @Test
  void testBorrowSameBookTwoUsersThenReturn() {
    when(userService.isUserActive("Petr")).thenReturn(true);
    assertTrue(libraryManager.borrowBook("book2", "Petr"));
    assertEquals(2, libraryManager.getAvailableCopies("book2"));

    when(userService.isUserActive("Pasha")).thenReturn(true);
    assertTrue(libraryManager.borrowBook("book2", "Pasha"));
    assertEquals(1, libraryManager.getAvailableCopies("book2"));

    assertTrue(libraryManager.returnBook("book2", "Petr"));
    assertEquals(2, libraryManager.getAvailableCopies("book2"));

    assertTrue(libraryManager.returnBook("book2", "Pasha"));
    assertEquals(3, libraryManager.getAvailableCopies("book2"));
  }
}
