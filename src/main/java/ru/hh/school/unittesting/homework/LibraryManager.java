package ru.hh.school.unittesting.homework;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class LibraryManager {

  private static final double BASE_LATE_FEE_PER_DAY = 0.5;
  private static final double BESTSELLER_MULTIPLIER = 1.5;
  private static final double PREMIUM_MEMBER_DISCOUNT = 0.8;

  private final NotificationService notificationService;
  private final UserService userService;

  private final Map<String, Integer> bookInventory = new HashMap<>();
  private final Map<String, Set<String>> borrowedBooks = new HashMap<>();

  public LibraryManager(NotificationService notificationService, UserService userService) {
    this.notificationService = notificationService;
    this.userService = userService;
  }

  public void addBook(String bookId, int quantity) {
    bookInventory.put(bookId, bookInventory.getOrDefault(bookId, 0) + quantity);
  }

  public boolean borrowBook(String bookId, String userId) {
    if (!userService.isUserActive(userId)) {
      notificationService.notifyUser(userId, "Your account is not active.");
      return false;
    }

    int availableCopies = bookInventory.getOrDefault(bookId, 0);
    if (availableCopies <= 0) {
      return false;
    }

    bookInventory.put(bookId, availableCopies - 1);

    Set<String> borrowedUsers;
    if (borrowedBooks.containsKey(bookId)) {
      borrowedUsers = borrowedBooks.get(bookId);
      borrowedUsers.add(userId);
    } else {
      borrowedUsers = new HashSet<>();
      borrowedUsers.add(userId);
      borrowedBooks.put(bookId, borrowedUsers);
    }

    notificationService.notifyUser(userId, "You have borrowed the book: " + bookId);
    return true;
  }

  public boolean returnBook(String bookId, String userId) {
    if (!borrowedBooks.containsKey(bookId)) {
      return false;
    }

    Set<String> borrowedUsers = borrowedBooks.get(bookId);
    if (!borrowedUsers.contains(userId)) {
      return false;
    }

    bookInventory.put(bookId, bookInventory.getOrDefault(bookId, 0) + 1);
    if (borrowedUsers.size() == 1) {
      borrowedBooks.remove(bookId);
    } else {
      borrowedUsers.remove(userId);
    }

    notificationService.notifyUser(userId, "You have returned the book: " + bookId);
    return true;
  }

  public int getAvailableCopies(String bookId) {
    return bookInventory.getOrDefault(bookId, 0);
  }

  public double calculateDynamicLateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember) {
    if (overdueDays < 0) {
      throw new IllegalArgumentException("Overdue days cannot be negative.");
    }

    double fee = overdueDays * BASE_LATE_FEE_PER_DAY;
    if (isBestseller) {
      fee *= BESTSELLER_MULTIPLIER;
    }
    if (isPremiumMember) {
      fee *= PREMIUM_MEMBER_DISCOUNT;
    }

    return BigDecimal.valueOf(fee)
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
